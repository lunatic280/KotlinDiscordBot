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
