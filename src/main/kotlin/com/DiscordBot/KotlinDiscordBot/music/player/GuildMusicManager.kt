package com.DiscordBot.KotlinDiscordBot.music.player

import com.DiscordBot.KotlinDiscordBot.music.queue.TrackQueue
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import net.dv8tion.jda.api.audio.AudioSendHandler

class GuildMusicManager(
    val guildId: Long,
    audioPlayerManager: AudioPlayerManager,
    onNothingLeft: () -> Unit,
) {
    val player: AudioPlayer = audioPlayerManager.createPlayer()
    val queue: TrackQueue = TrackQueue()
    val scheduler: TrackScheduler = TrackScheduler(player, queue, onNothingLeft)
    val sendHandler: AudioSendHandler = LavaPlayerSendHandler(player)

    init {
        player.addListener(scheduler)
    }

    // 길드 음악 플레이어를 종료하고 대기열을 비우는 함수입니다.
    fun shutdown() {
        player.destroy()
        queue.clear()
    }
}
