package com.DiscordBot.KotlinDiscordBot.music.util

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

// 오류 메시지를 빨간색 Discord 임베드로 만드는 함수입니다.
fun errorEmbed(msg: String): MessageEmbed =
    EmbedBuilder().setColor(Color.RED).setDescription(msg).build()

// 성공 메시지를 초록색 Discord 임베드로 만드는 함수입니다.
fun successEmbed(title: String, msg: String): MessageEmbed =
    EmbedBuilder().setColor(Color.GREEN).setTitle(title).setDescription(msg).build()

// 밀리초 단위 재생 시간을 mm:ss 또는 h:mm:ss 형식으로 변환하는 함수입니다.
fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
