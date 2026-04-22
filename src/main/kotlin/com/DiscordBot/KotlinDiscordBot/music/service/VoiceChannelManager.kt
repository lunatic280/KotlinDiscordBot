package com.DiscordBot.KotlinDiscordBot.music.service

import jakarta.annotation.PreDestroy
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class VoiceConnectResult(
    val previousChannelName: String?,
    val targetChannelName: String,
    val alreadyConnected: Boolean,
)

@Service
class VoiceChannelManager(private val musicPlayerService: MusicPlayerService) {

    private val disconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private val timers = ConcurrentHashMap<Long, ScheduledFuture<*>>()
    private val guildLocks = ConcurrentHashMap<Long, ReentrantLock>()

    fun connect(guild: Guild, channel: AudioChannel): VoiceConnectResult =
        withGuildLock(guild.idLong) {
            cancelTimerForGuild(guild.idLong)

            val audioManager = guild.audioManager
            val currentChannel = audioManager.connectedChannel
            val alreadyConnected = currentChannel?.idLong == channel.idLong && audioManager.isConnected
            val gmm = musicPlayerService.getOrCreate(guild)

            audioManager.sendingHandler = gmm.sendHandler
            audioManager.isAutoReconnect = true

            if (!alreadyConnected) {
                audioManager.openAudioConnection(channel)
            }

            VoiceConnectResult(
                previousChannelName = currentChannel?.name,
                targetChannelName = channel.name,
                alreadyConnected = alreadyConnected,
            )
        }

    fun disconnect(guild: Guild) =
        withGuildLock(guild.idLong) {
            cancelTimerForGuild(guild.idLong)
            disconnectLocked(guild)
        }

    private fun disconnectLocked(guild: Guild) {
        musicPlayerService.stop(guild)
        // Clear reconnect and handler before closing so JDA does not revive a torn-down player.
        guild.audioManager.isAutoReconnect = false
        guild.audioManager.sendingHandler = null
        guild.audioManager.closeAudioConnection()
        musicPlayerService.cleanup(guild)
    }

    fun scheduleAloneDisconnect(guild: Guild, delaySec: Long = 60) {
        cancelTimerForGuild(guild.idLong)
        val future = disconnectScheduler.schedule({
            withGuildLock(guild.idLong) {
                timers.remove(guild.idLong)
                val botChannel = guild.audioManager.connectedChannel ?: return@withGuildLock
                val humanCount = botChannel.members.count { !it.user.isBot }
                if (humanCount == 0) {
                    disconnectLocked(guild)
                }
            }
        }, delaySec, TimeUnit.SECONDS)
        timers[guild.idLong] = future
    }

    fun cancelTimer(guild: Guild) {
        cancelTimerForGuild(guild.idLong)
    }

    private fun cancelTimerForGuild(guildId: Long) {
        timers.remove(guildId)?.cancel(false)
    }

    private fun <T> withGuildLock(guildId: Long, action: () -> T): T =
        guildLocks.computeIfAbsent(guildId) { ReentrantLock() }.withLock(action)

    @PreDestroy
    fun shutdownScheduler() {
        timers.values.forEach { it.cancel(false) }
        timers.clear()
        disconnectScheduler.shutdownNow()
    }
}
