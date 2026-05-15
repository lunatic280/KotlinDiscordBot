package com.DiscordBot.KotlinDiscordBot.music.config

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.YoutubeSourceOptions
import dev.lavalink.youtube.clients.Web
import dev.lavalink.youtube.clients.AndroidVrWithThumbnail
import dev.lavalink.youtube.clients.MWebWithThumbnail
import dev.lavalink.youtube.clients.MusicWithThumbnail
import dev.lavalink.youtube.clients.WebEmbeddedWithThumbnail
import dev.lavalink.youtube.clients.WebWithThumbnail
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MusicConfig(
    @Value("\${youtube.po-token:}") private val poToken: String,
    @Value("\${youtube.visitor-data:}") private val visitorData: String,
    @Value("\${youtube.remote-cipher.url:}") private val remoteCipherUrl: String,
    @Value("\${youtube.remote-cipher.password:}") private val remoteCipherPassword: String,
    @Value("\${youtube.remote-cipher.user-agent:}") private val remoteCipherUserAgent: String,
    @Value("\${youtube.oauth.refresh-token:}") private val oauthRefreshToken: String,
) {

    // Lavaplayer와 YouTube 소스 매니저를 구성해 음악 재생 매니저 빈을 생성하는 함수입니다.
    @Bean
    fun audioPlayerManager(): AudioPlayerManager {
        val manager = DefaultAudioPlayerManager()

        if (poToken.isNotBlank() && visitorData.isNotBlank()) {
            Web.setPoTokenAndVisitorData(poToken, visitorData)
        }

        val ytManager: YoutubeAudioSourceManager = if (remoteCipherUrl.isNotBlank()) {
            val opts = YoutubeSourceOptions()
                .setAllowSearch(true)
                .setAllowDirectVideoIds(true)
                .setAllowDirectPlaylistIds(true)
                .setRemoteCipher(
                    remoteCipherUrl,
                    remoteCipherPassword.ifBlank { null },
                    remoteCipherUserAgent.ifBlank { null },
                )
            YoutubeAudioSourceManager(
                opts,
                MusicWithThumbnail(),
                AndroidVrWithThumbnail(),
                MWebWithThumbnail(),
                WebWithThumbnail(),
                WebEmbeddedWithThumbnail(),
            )
        } else {
            YoutubeAudioSourceManager(
                /* allowSearch = */ true,
                MusicWithThumbnail(),
                AndroidVrWithThumbnail(),
                MWebWithThumbnail(),
                WebWithThumbnail(),
                WebEmbeddedWithThumbnail(),
            )
        }

        // if (oauthRefreshToken.isNotBlank()) {
        //     ytManager.useOauth2(oauthRefreshToken, /* skipInitialization = */ true)
        // }

        manager.registerSourceManager(ytManager)
        AudioSourceManagers.registerRemoteSources(manager)
        return manager
    }
}
