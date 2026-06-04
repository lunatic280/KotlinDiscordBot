package com.DiscordBot.KotlinDiscordBot.config

import com.DiscordBot.KotlinDiscordBot.SlashCommandListener
import com.DiscordBot.KotlinDiscordBot.command.SlashCommand
import moe.kyokobot.libdave.NativeDaveFactory
import moe.kyokobot.libdave.jda.LDJDADaveSessionFactory
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.audio.AudioModuleConfig
import net.dv8tion.jda.api.audio.dave.DaveSessionFactory
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
    @Value("\${discord.token}") private val token: String,
) {

    // Discord JDA 클라이언트를 구성하고 슬래시 명령과 음악 이벤트 리스너를 등록하는 함수입니다.
    @Bean
    fun jda(
        slashListener: SlashCommandListener,
        commands: List<SlashCommand>,
        daveSessionFactory: DaveSessionFactory,
    ): JDA {
        val jda = JDABuilder.createDefault(token)
            .setAudioModuleConfig(
                AudioModuleConfig()
                    .withDaveSessionFactory(daveSessionFactory)
            )
            .enableIntents(
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MEMBERS,
            )
            .enableCache(CacheFlag.VOICE_STATE)
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setActivity(Activity.playing("Type /ping"))
            .addEventListeners(object : ListenerAdapter() {
                // JDA 준비 완료 시 현재 애플리케이션의 슬래시 명령들을 Discord에 갱신하는 함수입니다.
                override fun onReady(event: ReadyEvent) {
                    val commandData: List<SlashCommandData> = commands.map { it.getCommandData() }
                    event.jda.updateCommands().addCommands(commandData).queue()
                }
            })
            .build()
        return jda
    }

    // Discord 음성 암호화에 사용할 Dave 세션 팩토리 빈을 생성하는 함수입니다.
    @Bean
    fun daveSessionFactory(): DaveSessionFactory =
        LDJDADaveSessionFactory(NativeDaveFactory())
}
