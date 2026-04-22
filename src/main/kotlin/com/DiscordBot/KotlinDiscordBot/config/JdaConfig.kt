package com.DiscordBot.KotlinDiscordBot.config

import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import com.DiscordBot.KotlinDiscordBot.SlashCommandListener
import com.DiscordBot.KotlinDiscordBot.music.listener.MusicEventListener
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JdaConfig(
    @Value("\${discord.token}") private val token: String
) {

    @Bean
    fun jda(
        slashListener: SlashCommandListener,
        commands: List<SlashCommand>,
        musicEventListener: MusicEventListener
    ): JDA {
        val jda = JDABuilder.createDefault(token)
            .enableIntents(
                GatewayIntent.GUILD_VOICE_STATES,
                // 특권 인텐트 — Discord 개발자 포털에서 활성화 필요.
                // MemberCachePolicy.VOICE 가 음성 채널 인원을 정확히 카운트하려면 필수.
                GatewayIntent.GUILD_MEMBERS,
            )
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setActivity(Activity.playing("Type /ping"))
            .addEventListeners(slashListener, musicEventListener)
            .addEventListeners(object : ListenerAdapter() {
                override fun onReady(event: ReadyEvent) {
                    val commandData: List<SlashCommandData> = commands.map {
                        it.getCommandData()
                    }
                    event.jda.updateCommands().addCommands(commandData).queue()
                }
            })
            .build()
        return jda
    }
}