package com.DiscordBot.KotlinDiscordBot.music.player

import com.DiscordBot.KotlinDiscordBot.music.queue.LoopMode
import com.DiscordBot.KotlinDiscordBot.music.queue.TrackQueue
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason

class TrackScheduler(
    private val player: AudioPlayer,
    val queue: TrackQueue,
    private val onNothingLeft: () -> Unit,
) : AudioEventAdapter() {

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (!endReason.mayStartNext) return
        val next = when (queue.loopMode) {
            LoopMode.TRACK -> track.makeClone()
            LoopMode.QUEUE -> { queue.offer(track.makeClone()); queue.poll() }
            LoopMode.OFF   -> queue.poll()
        }
        if (next != null) player.startTrack(next, false) else onNothingLeft()
    }
}
