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
        guild.audioManager.isAutoReconnect = true
        guild.audioManager.sendingHandler = gmm.sendHandler
        if (!guild.audioManager.isConnected) {
            guild.audioManager.openAudioConnection(channel)
        }
    }

    fun disconnect(guild: Guild) {
        cancelTimer(guild)
        musicPlayerService.stop(guild)
        // 자동재접속 차단 후 핸들러 제거 → closeAudioConnection 순서 중요
        // 역순으로 하면 파괴된 player를 참조한 채 JDA가 재접속 시도하며 루프 발생
        guild.audioManager.isAutoReconnect = false
        guild.audioManager.sendingHandler = null
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
