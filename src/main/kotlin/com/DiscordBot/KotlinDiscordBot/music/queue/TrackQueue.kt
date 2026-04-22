package com.DiscordBot.KotlinDiscordBot.music.queue

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.ConcurrentLinkedDeque

class TrackQueue {
    private val deque = ConcurrentLinkedDeque<AudioTrack>()
    @Volatile var loopMode: LoopMode = LoopMode.OFF

    fun offer(track: AudioTrack) { deque.offer(track) }
    fun poll(): AudioTrack? = deque.poll()
    fun isEmpty(): Boolean = deque.isEmpty()
    fun size(): Int = deque.size
    fun clear() { deque.clear() }
    fun snapshot(): List<AudioTrack> = deque.toList()

    fun remove(index: Int): AudioTrack? {
        synchronized(this) {
            val list = deque.toMutableList()
            if (index !in list.indices) return null
            val removed = list.removeAt(index)
            deque.clear()
            list.forEach { deque.offer(it) }
            return removed
        }
    }

    fun shuffle() {
        synchronized(this) {
            val list = deque.toMutableList()
            list.shuffle()
            deque.clear()
            list.forEach { deque.offer(it) }
        }
    }
}
