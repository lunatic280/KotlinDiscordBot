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

    fun getOrCreate(guild: Guild): GuildMusicManager =
        managers.computeIfAbsent(guild.idLong) { id ->
            GuildMusicManager(id, audioPlayerManager, onNothingLeft = {})
        }

    fun load(guild: Guild, query: String): Mono<LoadResult> {
        getOrCreate(guild)
        val resolved = if (query.startsWith("http://") || query.startsWith("https://")) query
                       else "ytsearch:$query"
        return Mono.create { sink ->
            audioPlayerManager.loadItem(resolved, object : AudioLoadResultHandler {
                override fun trackLoaded(track: AudioTrack) =
                    sink.success(LoadResult.Single(track))

                override fun playlistLoaded(playlist: AudioPlaylist) {
                    if (playlist.isSearchResult) {
                        sink.success(LoadResult.Single(playlist.tracks.first()))
                    } else {
                        sink.success(LoadResult.Playlist(playlist.tracks, playlist.name))
                    }
                }

                override fun noMatches() = sink.success(LoadResult.NotFound)
                override fun loadFailed(e: FriendlyException) = sink.success(LoadResult.Failed(e))
            })
        }
    }

    fun enqueue(guild: Guild, track: AudioTrack) {
        val gmm = getOrCreate(guild)
        if (!gmm.player.startTrack(track, true)) {
            gmm.queue.offer(track)
        }
    }

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

    fun skip(guild: Guild): AudioTrack? {
        val gmm = getOrCreate(guild)
        val skipped = gmm.player.playingTrack ?: return null
        val next = gmm.queue.poll()
        if (next != null) gmm.player.startTrack(next, false) else gmm.player.stopTrack()
        return skipped
    }

    fun stop(guild: Guild) {
        val gmm = getOrCreate(guild)
        gmm.queue.clear()
        gmm.player.stopTrack()
    }

    fun setPaused(guild: Guild, paused: Boolean) {
        getOrCreate(guild).player.isPaused = paused
    }

    fun setVolume(guild: Guild, volume: Int) {
        getOrCreate(guild).player.volume = volume.coerceIn(0, 150)
    }

    fun setLoop(guild: Guild, mode: LoopMode) {
        getOrCreate(guild).queue.loopMode = mode
    }

    fun current(guild: Guild): AudioTrack? = getOrCreate(guild).player.playingTrack
    fun isPaused(guild: Guild): Boolean = getOrCreate(guild).player.isPaused

    fun cleanup(guild: Guild) {
        managers.remove(guild.idLong)?.shutdown()
    }
}
