package com.DiscordBot.KotlinDiscordBot.music.service

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class VoiceChannelManager {

    private val guildLocks = ConcurrentHashMap<Long, ReentrantLock>()

    fun connect(guild: Guild, channel: AudioChannel) =
        withGuildLock(guild.idLong) {
            val audioManager = guild.audioManager

            // 여기서 openAudioConnection은 딱 1번만 호출
            audioManager.openAudioConnection(channel)
        }

    fun disconnect(guild: Guild) =
        withGuildLock(guild.idLong) {
            guild.audioManager.closeAudioConnection()
        }

    private fun <T> withGuildLock(guildId: Long, action: () -> T): T =
        guildLocks.computeIfAbsent(guildId) { ReentrantLock() }.withLock(action)
}