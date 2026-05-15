package com.DiscordBot.KotlinDiscordBot.music.service

import jakarta.annotation.PreDestroy
import net.dv8tion.jda.api.audio.hooks.ConnectionListener
import net.dv8tion.jda.api.audio.hooks.ConnectionStatus
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class VoiceChannelManager(private val musicPlayerService: MusicPlayerService) {

    private val log = LoggerFactory.getLogger(VoiceChannelManager::class.java)
    private val guildLocks = ConcurrentHashMap<Long, ReentrantLock>()
    private val disconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private val aloneTimers = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    // 지정 음성 채널에 봇을 연결하고 오디오 전송 핸들러를 설정하는 함수입니다.
    fun connect(guild: Guild, channel: AudioChannel) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
        val am = guild.audioManager
        val gmm = musicPlayerService.getOrCreate(guild)

        // 순서 중요: openAudioConnection 전에 handler/autoReconnect 세팅.
        // 그렇지 않으면 JDA가 stale handler 또는 null 상태로 프레임을 보내려다 끊김→재접속 루프 유발.
        am.sendingHandler = gmm.sendHandler
        am.isAutoReconnect = true

        // 루프 진단용: 모든 voice 상태 전환 로깅. 배포 후 /입장 실행 시 나오는 status 전이 패턴으로 원인 식별.
        am.connectionListener = object : ConnectionListener {
            // Discord 음성 연결 상태 변화를 로그로 남기는 콜백 함수입니다.
            override fun onStatusChange(status: ConnectionStatus) {
                log.info("[voice] guild={} channel={} status={}", guild.idLong, channel.name, status)
            }
        }

        val already = am.isConnected && am.connectedChannel?.idLong == channel.idLong
        if (!already) {
            log.info("[voice] openAudioConnection guild={} channel={}", guild.idLong, channel.name)
            am.openAudioConnection(channel)
        }
    }

    // 길드 음성 연결을 끊고 자동 퇴장 타이머를 취소하는 함수입니다.
    fun disconnect(guild: Guild) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
        disconnectLocked(guild)
    }

    // 이미 락을 잡은 상태에서 음악 정지와 음성 연결 해제를 수행하는 내부 함수입니다.
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

    // 봇이 음성 채널에 혼자 남았을 때 지연 후 자동 연결 해제를 예약하는 함수입니다.
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

    // 길드에 예약된 혼자 남음 자동 퇴장 타이머를 취소하는 함수입니다.
    fun cancelAloneTimer(guild: Guild) = withGuildLock(guild.idLong) {
        cancelAloneTimerLocked(guild.idLong)
    }

    // 이미 락을 잡은 상태에서 길드 자동 퇴장 타이머를 취소하는 내부 함수입니다.
    private fun cancelAloneTimerLocked(guildId: Long) {
        aloneTimers.remove(guildId)?.cancel(false)
    }

    // 길드별 ReentrantLock을 사용해 음성 상태 변경 작업을 직렬화하는 함수입니다.
    private fun <T> withGuildLock(guildId: Long, action: () -> T): T =
        guildLocks.computeIfAbsent(guildId) { ReentrantLock() }.withLock(action)

    // 애플리케이션 종료 시 예약 타이머와 스케줄러 자원을 정리하는 함수입니다.
    @PreDestroy
    fun shutdown() {
        aloneTimers.values.forEach { it.cancel(false) }
        aloneTimers.clear()
        disconnectScheduler.shutdownNow()
    }
}
