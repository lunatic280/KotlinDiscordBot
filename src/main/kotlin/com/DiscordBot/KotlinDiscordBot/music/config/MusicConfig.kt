package com.DiscordBot.KotlinDiscordBot.music.config

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.TvHtml5Embedded
import dev.lavalink.youtube.clients.Web
import dev.lavalink.youtube.clients.WebEmbedded
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MusicConfig {
    @Bean
    fun audioPlayerManager(): AudioPlayerManager {
        val manager = DefaultAudioPlayerManager()
        // 클라이언트 우선순위: TvHtml5Embedded(차단 덜 됨) → Web → WebEmbedded
        // 하나가 실패하면 다음 클라이언트로 자동 폴백
        val ytManager = YoutubeAudioSourceManager(true, TvHtml5Embedded(), Web(), WebEmbedded())
        manager.registerSourceManager(ytManager)
        AudioSourceManagers.registerRemoteSources(
            manager,
            com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java
        )
        return manager
    }
}
