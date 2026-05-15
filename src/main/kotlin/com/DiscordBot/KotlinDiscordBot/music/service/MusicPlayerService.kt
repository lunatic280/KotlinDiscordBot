package com.DiscordBot.KotlinDiscordBot.music.service

import com.DiscordBot.KotlinDiscordBot.music.player.GuildMusicManager
import com.DiscordBot.KotlinDiscordBot.music.queue.LoopMode
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

sealed interface LoadResult {
    data class Single(val track: AudioTrack) : LoadResult
    data class Playlist(val tracks: List<AudioTrack>, val name: String) : LoadResult
    data object NotFound : LoadResult
    data class Failed(val cause: Throwable) : LoadResult
}

@Service
class MusicPlayerService(private val audioPlayerManager: AudioPlayerManager) {

    private val managers = ConcurrentHashMap<Long, GuildMusicManager>()

    // 길드별 음악 매니저를 조회하거나 없으면 새로 생성하는 함수입니다.
    fun getOrCreate(guild: Guild): GuildMusicManager =
        managers.computeIfAbsent(guild.idLong) { id ->
            GuildMusicManager(id, audioPlayerManager, onNothingLeft = {})
        }

    // URL 또는 검색어를 Lavaplayer로 로드해 단일 곡, 플레이리스트, 실패 결과로 변환하는 함수입니다.
    fun load(guild: Guild, query: String): Mono<LoadResult> {
        getOrCreate(guild)
        val resolved = if (query.startsWith("http://") || query.startsWith("https://")) query
                       else "ytsearch:$query"
        return Mono.create { sink ->
            audioPlayerManager.loadItem(resolved, object : AudioLoadResultHandler {
                // 단일 트랙 로드 성공 결과를 Mono sink에 전달하는 콜백 함수입니다.
                override fun trackLoaded(track: AudioTrack) =
                    sink.success(LoadResult.Single(track))

                // 플레이리스트 또는 검색 결과 로드 성공 결과를 Mono sink에 전달하는 콜백 함수입니다.
                override fun playlistLoaded(playlist: AudioPlaylist) {
                    if (playlist.isSearchResult) {
                        sink.success(LoadResult.Single(playlist.tracks.first()))
                    } else {
                        sink.success(LoadResult.Playlist(playlist.tracks, playlist.name))
                    }
                }

                // 검색 결과가 없을 때 NotFound 결과를 Mono sink에 전달하는 콜백 함수입니다.
                override fun noMatches() = sink.success(LoadResult.NotFound)
                // 로드 실패 예외를 Failed 결과로 감싸 Mono sink에 전달하는 콜백 함수입니다.
                override fun loadFailed(e: FriendlyException) = sink.success(LoadResult.Failed(e))
            })
        }
    }

    // 단일 트랙을 즉시 재생하거나 재생 중이면 대기열에 추가하는 함수입니다.
    fun enqueue(guild: Guild, track: AudioTrack) {
        val gmm = getOrCreate(guild)
        if (!gmm.player.startTrack(track, true)) {
            gmm.queue.offer(track)
        }
    }

    // 여러 트랙을 재생 목록으로 받아 첫 곡은 재생 시도하고 나머지는 대기열에 추가하는 함수입니다.
    fun enqueueAll(guild: Guild, tracks: List<AudioTrack>) {
        val gmm = getOrCreate(guild)
        tracks.forEachIndexed { i, track ->
            if (i == 0) {
                if (!gmm.player.startTrack(track, true)) gmm.queue.offer(track)
            } else {
                gmm.queue.offer(track)
            }
        }
    }

    // 현재 곡을 건너뛰고 다음 대기열 곡을 재생하는 함수입니다.
    fun skip(guild: Guild): AudioTrack? {
        val gmm = getOrCreate(guild)
        val skipped = gmm.player.playingTrack ?: return null
        val next = gmm.queue.poll()
        if (next != null) gmm.player.startTrack(next, false) else gmm.player.stopTrack()
        return skipped
    }

    // 재생을 중지하고 해당 길드의 대기열을 비우는 함수입니다.
    fun stop(guild: Guild) {
        val gmm = getOrCreate(guild)
        gmm.queue.clear()
        gmm.player.stopTrack()
    }

    // 해당 길드 음악 플레이어의 일시정지 상태를 설정하는 함수입니다.
    fun setPaused(guild: Guild, paused: Boolean) {
        getOrCreate(guild).player.isPaused = paused
    }

    // 해당 길드 음악 플레이어의 볼륨을 0에서 150 사이로 설정하는 함수입니다.
    fun setVolume(guild: Guild, volume: Int) {
        getOrCreate(guild).player.volume = volume.coerceIn(0, 150)
    }

    // 해당 길드 음악 대기열의 반복 모드를 설정하는 함수입니다.
    fun setLoop(guild: Guild, mode: LoopMode) {
        getOrCreate(guild).queue.loopMode = mode
    }

    // 현재 재생 중인 트랙을 반환하는 함수입니다.
    fun current(guild: Guild): AudioTrack? = getOrCreate(guild).player.playingTrack
    // 현재 음악 플레이어가 일시정지 상태인지 반환하는 함수입니다.
    fun isPaused(guild: Guild): Boolean = getOrCreate(guild).player.isPaused

    // 길드 음악 매니저를 제거하고 관련 플레이어 자원을 정리하는 함수입니다.
    fun cleanup(guild: Guild) {
        managers.remove(guild.idLong)?.shutdown()
    }
}
