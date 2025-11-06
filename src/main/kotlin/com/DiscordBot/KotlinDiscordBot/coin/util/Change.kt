package com.DiscordBot.KotlinDiscordBot.coin.util

enum class Change(
    val code: String,
    val color: Int,
    val emoji: String,
    val ko: String,
    val arrow: String
) {
    RISE(
        code = "RISE",
        color = 0xE74C3C,      // 빨강
        emoji = "🔴",
        ko = "상승",
        arrow = "▲"
    ),
    FALL(
        code = "FALL",
        color = 0x3498DB,      // 파랑
        emoji = "🔵",
        ko = "하락",
        arrow = "▼"
    ),
    EVEN(
        code = "EVEN",
        color = 0x000000,          // 무색
        emoji = "⚪",
        ko = "보합",
        arrow = "—"
    );

    companion object {
        fun fromApi(value: String?): Change =
            entries.firstOrNull { it.code.equals(value, ignoreCase = true) } ?: EVEN
    }

    /** 예) "🔴 ▲ 상승 (+1.23%)" */
    fun labelWithPct(pct: Double?): String {
        val pctLabel = pct?.let { formatSignedPct(it) } ?: "0.00%"
        return "$emoji $arrow $ko ($pctLabel)"
    }
}

private fun formatSignedPct(pct: Double): String {
    val sign = if (pct >= 0.0) "+" else ""
    return "%s%.2f%%".format(sign, pct)
}