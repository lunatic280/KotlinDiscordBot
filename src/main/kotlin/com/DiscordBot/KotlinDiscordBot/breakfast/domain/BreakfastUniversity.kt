package com.DiscordBot.KotlinDiscordBot.breakfast.domain

enum class BreakfastUniversity(
    val optionValue: String,
    val displayName: String
) {
    CATHOLIC("catholic", "Catholic University of Korea"),
    CNU("cnu", "Chungnam National University");

    companion object {
        // 슬래시 명령 옵션 문자열을 지원하는 대학 enum 값으로 변환하는 함수입니다.
        fun fromOption(value: String): BreakfastUniversity? {
            return entries.firstOrNull { it.optionValue.equals(value.trim(), ignoreCase = true) }
        }
    }
}
