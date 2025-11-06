package com.DiscordBot.KotlinDiscordBot.coin.data

import kotlinx.serialization.Serializable

@Serializable
data class TickerDto(
    val opening_price: Long,
    val low_price: Long,
    val high_price: Long,
    val trade_price: Long,
    val change_rate: Double? = null,
    val signed_change_rate: Double? = null,
    val prev_closing_price: Long? = null,
    val change: String
)

fun TickerDto.showRate(): Double? {
    signed_change_rate?.let { return it * 100 }

    if (change_rate != null) {
        val base = change_rate * 100
        return when (change.uppercase()) {
            "FALL" -> -base
            "RISE" -> +base
            else -> 0.0
        }
    }

    if (prev_closing_price != null && prev_closing_price > 0L) {
        val pct = (trade_price - prev_closing_price).toDouble() / prev_closing_price * 100
        return pct
    }

    return null
}
