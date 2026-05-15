package com.DiscordBot.KotlinDiscordBot.music.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class LavaPlayerSendHandler(private val player: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    // Lavaplayer에서 다음 오디오 프레임을 제공할 수 있는지 확인하는 함수입니다.
    override fun canProvide(): Boolean {
        lastFrame = player.provide()
        return lastFrame != null
    }

    // Discord에 보낼 20ms 오디오 프레임을 ByteBuffer로 제공하는 함수입니다.
    override fun provide20MsAudio(): ByteBuffer? =
        lastFrame?.let { ByteBuffer.wrap(it.data) }

    // 제공하는 오디오 프레임이 Opus 포맷임을 알리는 함수입니다.
    override fun isOpus(): Boolean = true
}
