package com.DiscordBot.KotlinDiscordBot.music.queue

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.ConcurrentLinkedDeque

class TrackQueue {
    private val deque = ConcurrentLinkedDeque<AudioTrack>()
    @Volatile var loopMode: LoopMode = LoopMode.OFF

    // 재생 대기열 끝에 트랙을 추가하는 함수입니다.
    fun offer(track: AudioTrack) { deque.offer(track) }
    // 재생 대기열 맨 앞의 트랙을 꺼내는 함수입니다.
    fun poll(): AudioTrack? = deque.poll()
    // 재생 대기열이 비어 있는지 확인하는 함수입니다.
    fun isEmpty(): Boolean = deque.isEmpty()
    // 현재 재생 대기열의 트랙 개수를 반환하는 함수입니다.
    fun size(): Int = deque.size
    // 재생 대기열의 모든 트랙을 삭제하는 함수입니다.
    fun clear() { deque.clear() }
    // 현재 재생 대기열을 읽기용 목록으로 복사하는 함수입니다.
    fun snapshot(): List<AudioTrack> = deque.toList()

    // 지정한 인덱스의 트랙을 대기열에서 제거하고 반환하는 함수입니다.
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

    // 현재 재생 대기열의 순서를 무작위로 섞는 함수입니다.
    fun shuffle() {
        synchronized(this) {
            val list = deque.toMutableList()
            list.shuffle()
            deque.clear()
            list.forEach { deque.offer(it) }
        }
    }
}
