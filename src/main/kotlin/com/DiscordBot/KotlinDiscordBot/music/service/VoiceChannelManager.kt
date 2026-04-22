package com.DiscordBot.KotlinDiscordBot.music.service

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class VoiceChannelManager(private val musicPlayerService: MusicPlayerService) {

    private val disconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private val timers = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    fun connect(guild: Guild, channel: AudioChannel) {
        cancelTimer(guild)
        val gmm = musicPlayerService.getOrCreate(guild)
        guild.audioManager.sendingHandler = gmm.sendHandler
        if (!guild.audioManager.isConnected) {
            guild.audioManager.openAudioConnection(channel)
        }
    }

    fun disconnect(guild: Guild) {
        cancelTimer(guild)
        musicPlayerService.stop(guild)
        guild.audioManager.closeAudioConnection()
        musicPlayerService.cleanup(guild)
    }

    fun scheduleAloneDisconnect(guild: Guild, delaySec: Long = 60) {
        cancelTimer(guild)
        val future = disconnectScheduler.schedule({ disconnect(guild) }, delaySec, TimeUnit.SECONDS)
        timers[guild.idLong] = future
    }

    fun cancelTimer(guild: Guild) {
        timers.remove(guild.idLong)?.cancel(false)
    }
}
