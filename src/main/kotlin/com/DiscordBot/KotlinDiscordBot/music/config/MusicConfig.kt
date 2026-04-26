package com.DiscordBot.KotlinDiscordBot.music.config

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.lavalink.youtube.YoutubeAudioSourceManager
import dev.lavalink.youtube.clients.TvHtml5Simply
import dev.lavalink.youtube.clients.Web
import dev.lavalink.youtube.clients.WebEmbedded
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MusicConfig {

    @Bean
    fun audioPlayerManager(): AudioPlayerManager {
        val manager = DefaultAudioPlayerManager()

        // 1) youtube-source v2 등록.
        //    - 1번째 인자 allowSearch=true: "ytsearch:" 스킴을 처리할 수 있게 한다 (MusicPlayerService.load 가 사용).
        //    - 클라이언트 순서는 우선순위. Web(메인) → TvHtml5Simply(폴백) → WebEmbedded(임베드 한정 폴백).
        //      TvHtml5Embedded는 1.18.0에서 EOL로 제거되었으므로 사용하지 않는다.
        val ytManager = YoutubeAudioSourceManager(
            /* allowSearch = */ true,
            Web(),
            TvHtml5Simply(),
            WebEmbedded()
        )
        manager.registerSourceManager(ytManager)

        // 2) 비-YouTube remote source(SoundCloud / Bandcamp / Vimeo / Twitch / HTTP / Beam 등)를 등록.
        //    lavaplayer 2.2.x 의 기본 YoutubeAudioSourceManager(com.sedmelluq...) 클래스는
        //    이미 패키지에서 제거되었으므로 exclusion 인자를 넘기지 않는다.
        //    인자 없이 호출하면 기본 remote source 묶음이 안전하게 등록된다.
        AudioSourceManagers.registerRemoteSources(manager)

        return manager
    }
}
