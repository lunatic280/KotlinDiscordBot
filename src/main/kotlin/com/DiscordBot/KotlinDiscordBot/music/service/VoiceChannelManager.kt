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

@Service
class VoiceChannelManager(private val musicPlayerService: MusicPlayerService) {

    private val guildLocks = ConcurrentHashMap<Long, ReentrantLock>()
    private val disconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private val aloneTimers = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    fun connect(guild: Guild, channel: AudioChannel) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
        val am = guild.audioManager
        val gmm = musicPlayerService.getOrCreate(guild)

        // 순서 중요: openAudioConnection 전에 handler/autoReconnect 세팅.
        // 그렇지 않으면 JDA가 stale handler 또는 null 상태로 프레임을 보내려다 끊김→재접속 루프 유발.
        am.sendingHandler = gmm.sendHandler
        am.isAutoReconnect = true

        val already = am.isConnected && am.connectedChannel?.idLong == channel.idLong
        if (!already) {
            am.openAudioConnection(channel)
        }
    }

    fun disconnect(guild: Guild) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
        disconnectLocked(guild)
    }

    private fun disconnectLocked(guild: Guild) {
        musicPlayerService.stop(guild)
        val am = guild.audioManager
        // 순서 중요: closeAudioConnection 전에 autoReconnect=false + sendingHandler=null.
        // 역순이면 JDA가 파괴된 player를 참조하는 handler로 자동재접속 → 즉시 끊김 → 루프.
        am.isAutoReconnect = false
        am.sendingHandler = null
        am.closeAudioConnection()
        musicPlayerService.cleanup(guild)
    }

    fun scheduleAloneDisconnect(guild: Guild, delaySec: Long = 60) {
        val guildId = guild.idLong
        withGuildLock(guildId) {
            cancelAloneTimerLocked(guildId)
            val future = disconnectScheduler.schedule({
                withGuildLock(guildId) {
                    aloneTimers.remove(guildId)
                    val botChannel = guild.audioManager.connectedChannel ?: return@withGuildLock
                    val humanCount = botChannel.members.count { !it.user.isBot }
                    if (humanCount == 0) {
                        disconnectLocked(guild)
                    }
                }
            }, delaySec, TimeUnit.SECONDS)
            aloneTimers[guildId] = future
        }
    }

    fun cancelAloneTimer(guild: Guild) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
    }

    private fun cancelAloneTimerLocked(guildId: Long) {
        aloneTimers.remove(guildId)?.cancel(false)
    }

    private fun <T> withGuildLock(guildId: Long, action: () -> T): T =
        guildLocks.computeIfAbsent(guildId) { ReentrantLock() }.withLock(action)

    @PreDestroy
    fun shutdown() {
        aloneTimers.values.forEach { it.cancel(false) }
        aloneTimers.clear()
        disconnectScheduler.shutdownNow()
    }
}
