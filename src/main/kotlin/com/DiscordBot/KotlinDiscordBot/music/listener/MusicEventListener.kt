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
        // 봇 자신의 입장/퇴장 이벤트는 무시 (humanCount 오탐 방지)
        if (event.entity.user == event.jda.selfUser) return

        val guild = event.guild
        val botChannel = guild.audioManager.connectedChannel ?: return
        if (event.channelLeft != botChannel && event.channelJoined != botChannel) return

        val humanCount = botChannel.members.count { !it.user.isBot }
//        if (humanCount == 0) {
//            voiceChannelManager.scheduleAloneDisconnect(guild, 60)
//        } else {
//            voiceChannelManager.cancelTimer(guild)
//        }
    }
}
