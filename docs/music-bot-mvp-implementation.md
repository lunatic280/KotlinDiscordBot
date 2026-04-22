# 노래봇 MVP 구현 가이드

> Phase 1 범위: `/play` `/pause` `/resume` `/skip` `/stop` `/nowplaying` `/leave` + 자동 퇴장

---

## 파일 구조

```
music/
├── config/MusicConfig.kt
├── queue/LoopMode.kt
├── queue/TrackQueue.kt
├── player/LavaPlayerSendHandler.kt
├── player/TrackScheduler.kt
├── player/GuildMusicManager.kt
├── service/MusicPlayerService.kt
├── service/VoiceChannelManager.kt
├── listener/MusicEventListener.kt
├── util/MusicEmbed.kt
└── commands/
    ├── PlayCommand.kt
    ├── PauseCommand.kt
    ├── ResumeCommand.kt
    ├── SkipCommand.kt
    ├── StopCommand.kt
    ├── NowPlayingCommand.kt
    └── LeaveCommand.kt
```

---

## Step 1. `build.gradle.kts` — 의존성 추가

```kotlin
// dependencies { } 블록 안에 추가
implementation("dev.arbjerg:lavaplayer:2.2.2")
implementation("dev.lavalink.youtube:v2:1.11.4")
```

---

## Step 2. `config/JdaConfig.kt` — Intent + 리스너 추가

기존 `jda()` 빈에 두 줄 추가합니다.

```kotlin
@Bean
fun jda(
    slashListener: SlashCommandListener,
    commands: List<SlashCommand>,
    musicEventListener: MusicEventListener,   // 추가
): JDA {
    val jda = JDABuilder.createDefault(token)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES)       // 추가
        .setActivity(Activity.playing("Type /ping"))
        .addEventListeners(slashListener, musicEventListener)  // musicEventListener 추가
        .addEventListeners(object : ListenerAdapter() {
            override fun onReady(event: ReadyEvent) {
                event.jda.updateCommands()
                    .addCommands(commands.map { it.getCommandData() })
                    .queue()
            }
        })
        .build()
    return jda
}
```

---

## Step 3. `music/config/MusicConfig.kt`

`AudioPlayerManager` Spring Bean. YouTube v2 플러그인을 먼저 등록하고 구 YouTube 소스를 제외합니다.

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.config

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import dev.lavalink.youtube.YoutubeAudioSourceManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MusicConfig {
    @Bean
    fun audioPlayerManager(): AudioPlayerManager {
        val manager = DefaultAudioPlayerManager()
        manager.registerSourceManager(YoutubeAudioSourceManager())
        AudioSourceManagers.registerRemoteSources(
            manager,
            com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager::class.java
        )
        return manager
    }
}
```

---

## Step 4. `music/queue/LoopMode.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.queue

enum class LoopMode { OFF, TRACK, QUEUE }
```

---

## Step 5. `music/queue/TrackQueue.kt`

```kotlin
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
```

---

## Step 6. `music/player/LavaPlayerSendHandler.kt`

JDA `AudioSendHandler` ↔ LavaPlayer `AudioPlayer` 브리지.

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.player

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class LavaPlayerSendHandler(private val player: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = player.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteBuffer? =
        lastFrame?.let { ByteBuffer.wrap(it.data) }

    override fun isOpus(): Boolean = true
}
```

---

## Step 7. `music/player/TrackScheduler.kt`

트랙 종료 이벤트를 받아 `LoopMode`에 따라 다음 곡을 시작합니다.

> **주의**: `skip()` 에서 `player.stopTrack()` 호출 시 end reason이 `STOP_REQUESTED`
> (`mayStartNext = false`) 가 돼서 이 스케줄러가 동작하지 않습니다.
> 그래서 `MusicPlayerService.skip()` 은 직접 다음 트랙을 `startTrack` 합니다.

```kotlin
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
```

---

## Step 8. `music/player/GuildMusicManager.kt`

길드별 상태 컨테이너. `AudioPlayer`, `TrackQueue`, `TrackScheduler`, `SendHandler`를 한 곳에서 보관합니다.

```kotlin
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

    fun shutdown() {
        player.destroy()
        queue.clear()
    }
}
```

---

## Step 9. `music/service/MusicPlayerService.kt`

모든 커맨드가 이 서비스만 호출합니다. 길드별 `GuildMusicManager`를 `ConcurrentHashMap`으로 관리합니다.

```kotlin
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
            GuildMusicManager(id, audioPlayerManager, onNothingLeft = {
                // Phase 2: idle 타이머 예약 위치
            })
        }

    fun load(guild: Guild, query: String): Mono<LoadResult> {
        getOrCreate(guild)
        return Mono.create { sink ->
            audioPlayerManager.loadItem(query, object : AudioLoadResultHandler {
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
        // noInterrupt=true: 재생 중이면 큐에 추가, 비어있으면 바로 재생
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

    // stopTrack()의 end reason은 mayStartNext=false라 스케줄러가 동작하지 않으므로
    // 다음 트랙을 직접 startTrack으로 지정합니다.
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
```

---

## Step 10. `music/service/VoiceChannelManager.kt`

음성 채널 연결·해제·자동 퇴장 타이머를 담당합니다.

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.service

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service
class VoiceChannelManager(private val musicPlayerService: MusicPlayerService) {

    private val disconnectScheduler = Executors.newSingleThreadScheduledExecutor()
    private val timers = ConcurrentHashMap<Long, ScheduledFuture<*>>()

    fun connect(guild: Guild, channel: AudioChannel) {
        cancelTimer(guild)
        val gmm = musicPlayerService.getOrCreate(guild)
        guild.audioManager.sendingHandler = gmm.sendHandler
        if (!guild.audioManager.isConnected) {
            guild.audioManager.openAudioConnection(channel)
        }
    }

    fun disconnect(guild: Guild) {
        cancelTimer(guild)
        musicPlayerService.stop(guild)
        guild.audioManager.closeAudioConnection()
        musicPlayerService.cleanup(guild)
    }

    fun scheduleAloneDisconnect(guild: Guild, delaySec: Long = 60) {
        cancelTimer(guild)
        val future = disconnectScheduler.schedule({ disconnect(guild) }, delaySec, TimeUnit.SECONDS)
        timers[guild.idLong] = future
    }

    fun cancelTimer(guild: Guild) {
        timers.remove(guild.idLong)?.cancel(false)
    }
}
```

---

## Step 11. `music/listener/MusicEventListener.kt`

봇이 혼자 남으면 60초 후 자동 퇴장합니다.

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.listener

import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class MusicEventListener(
    private val voiceChannelManager: VoiceChannelManager
) : ListenerAdapter() {

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val guild = event.guild
        val botChannel = guild.audioManager.connectedChannel ?: return
        if (event.channelLeft != botChannel && event.channelJoined != botChannel) return

        val humanCount = botChannel.members.count { !it.user.isBot }
        if (humanCount == 0) {
            voiceChannelManager.scheduleAloneDisconnect(guild, 60)
        } else {
            voiceChannelManager.cancelTimer(guild)
        }
    }
}
```

---

## Step 12. `music/util/MusicEmbed.kt`

커맨드에서 공통으로 쓰는 임베드 헬퍼입니다.

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

fun errorEmbed(msg: String): MessageEmbed =
    EmbedBuilder().setColor(Color.RED).setDescription(msg).build()

fun successEmbed(title: String, msg: String): MessageEmbed =
    EmbedBuilder().setColor(Color.GREEN).setTitle(title).setDescription(msg).build()

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
```

---

## Step 13. 커맨드 7개

### `music/commands/PlayCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.LoadResult
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class PlayCommand(
    private val musicPlayerService: MusicPlayerService,
    private val voiceChannelManager: VoiceChannelManager,
) : SlashCommand {
    override val name = "play"
    override val description = "음악 재생"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel as? AudioChannel
        if (channel == null) {
            event.replyEmbeds(errorEmbed("먼저 음성 채널에 입장해주세요.")).setEphemeral(true).queue()
            return
        }
        val query = event.getOption("query")?.asString?.trim() ?: return

        event.deferReply().queue()
        voiceChannelManager.connect(guild, channel)

        musicPlayerService.load(guild, query).subscribe { result ->
            when (result) {
                is LoadResult.Single -> {
                    musicPlayerService.enqueue(guild, result.track)
                    event.hook.sendMessageEmbeds(
                        EmbedBuilder().setColor(Color.GREEN)
                            .setTitle("대기열 추가")
                            .setDescription("[${result.track.info.title}](${result.track.info.uri})")
                            .setFooter(event.user.effectiveName, event.user.avatarUrl)
                            .build()
                    ).queue()
                }
                is LoadResult.Playlist -> {
                    musicPlayerService.enqueueAll(guild, result.tracks)
                    event.hook.sendMessageEmbeds(
                        EmbedBuilder().setColor(Color.GREEN)
                            .setTitle("플레이리스트 추가")
                            .setDescription("**${result.name}** (${result.tracks.size}곡)")
                            .setFooter(event.user.effectiveName, event.user.avatarUrl)
                            .build()
                    ).queue()
                }
                is LoadResult.NotFound ->
                    event.hook.sendMessageEmbeds(errorEmbed("검색 결과가 없습니다.")).setEphemeral(true).queue()
                is LoadResult.Failed ->
                    event.hook.sendMessageEmbeds(errorEmbed("로드 실패: ${result.cause.message}")).setEphemeral(true).queue()
            }
        }
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description)
            .setNameLocalization(DiscordLocale.KOREAN, "재생")
            .addOptions(
                OptionData(OptionType.STRING, "query", "URL 또는 검색어", true)
                    .setNameLocalization(DiscordLocale.KOREAN, "검색어")
                    .setDescriptionLocalization(DiscordLocale.KOREAN, "YouTube URL 또는 검색어를 입력하세요")
            )
}
```

---

### `music/commands/PauseCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class PauseCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "pause"
    override val description = "일시정지"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (musicPlayerService.current(guild) == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        musicPlayerService.setPaused(guild, true)
        event.replyEmbeds(successEmbed("일시정지", "⏸ 재생을 일시정지했습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "일시정지")
}
```

---

### `music/commands/ResumeCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class ResumeCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "resume"
    override val description = "재생 재개"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (!musicPlayerService.isPaused(guild)) {
            event.replyEmbeds(errorEmbed("일시정지 상태가 아닙니다.")).setEphemeral(true).queue()
            return
        }
        musicPlayerService.setPaused(guild, false)
        event.replyEmbeds(successEmbed("재개", "▶ 재생을 재개합니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "재개")
}
```

---

### `music/commands/SkipCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class SkipCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "skip"
    override val description = "현재 곡 스킵"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val skipped = musicPlayerService.skip(guild)
        if (skipped == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        event.replyEmbeds(successEmbed("스킵", "⏭ **${skipped.info.title}** 을(를) 스킵했습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "스킵")
}
```

---

### `music/commands/StopCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class StopCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "stop"
    override val description = "재생 정지 및 대기열 초기화"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        musicPlayerService.stop(guild)
        event.replyEmbeds(successEmbed("정지", "⏹ 재생을 멈추고 대기열을 비웠습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "정지")
}
```

---

### `music/commands/NowPlayingCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.MusicPlayerService
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.formatDuration
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component
import java.awt.Color

@Component
class NowPlayingCommand(private val musicPlayerService: MusicPlayerService) : SlashCommand {
    override val name = "nowplaying"
    override val description = "현재 재생 중인 곡"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val track = musicPlayerService.current(guild)
        if (track == null) {
            event.replyEmbeds(errorEmbed("현재 재생 중인 곡이 없습니다.")).setEphemeral(true).queue()
            return
        }
        val position = formatDuration(track.position)
        val duration = if (track.info.isStream) "라이브" else formatDuration(track.duration)
        event.replyEmbeds(
            EmbedBuilder()
                .setColor(Color.CYAN)
                .setTitle("지금 재생 중")
                .setDescription("[${track.info.title}](${track.info.uri})")
                .addField("진행", "`$position / $duration`", true)
                .addField("채널", track.info.author, true)
                .build()
        ).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "지금곡")
}
```

---

### `music/commands/LeaveCommand.kt`

```kotlin
package com.DiscordBot.KotlinDiscordBot.music.commands

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.music.service.VoiceChannelManager
import com.DiscordBot.KotlinDiscordBot.music.util.errorEmbed
import com.DiscordBot.KotlinDiscordBot.music.util.successEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.springframework.stereotype.Component

@Component
class LeaveCommand(private val voiceChannelManager: VoiceChannelManager) : SlashCommand {
    override val name = "leave"
    override val description = "봇 퇴장"

    override fun handle(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        if (!guild.audioManager.isConnected) {
            event.replyEmbeds(errorEmbed("봇이 음성 채널에 없습니다.")).setEphemeral(true).queue()
            return
        }
        voiceChannelManager.disconnect(guild)
        event.replyEmbeds(successEmbed("퇴장", "음성 채널에서 나갔습니다.")).queue()
    }

    override fun getCommandData(): SlashCommandData =
        Commands.slash(name, description).setNameLocalization(DiscordLocale.KOREAN, "퇴장")
}
```

---

## 체크리스트

- [ ] `build.gradle.kts` 의존성 2줄 추가
- [ ] `JdaConfig.kt` — `GUILD_VOICE_STATES` Intent + `MusicEventListener` 등록
- [ ] `MusicConfig.kt` 생성 (AudioPlayerManager Bean)
- [ ] `queue/`, `player/`, `service/`, `listener/`, `util/` 패키지 생성
- [ ] `commands/` 7개 파일 생성
- [ ] `./gradlew clean bootJar -x test` 빌드 확인
- [ ] YouTube URL 한 개로 `/play` → `/nowplaying` → `/skip` → `/leave` 수동 QA

---

## 참고 — skip 동작 원리

`player.stopTrack()` 호출 시 LavaPlayer가 내부적으로 `AudioTrackEndReason.STOPPED`를 발생시키는데,
이 reason의 `mayStartNext = false` 입니다. 따라서 `TrackScheduler.onTrackEnd` 에서 아무 동작도 하지 않습니다.

그래서 `MusicPlayerService.skip()` 은 `stopTrack()` 을 호출하지 않고, 큐에서 다음 트랙을 꺼내
`player.startTrack(next, false)` 로 직접 교체합니다. (`noInterrupt = false` 는 현재 재생 중인 트랙을 강제로 교체)
