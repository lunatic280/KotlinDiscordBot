package com.DiscordBot.KotlinDiscordBot.coin.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class MarketCodeConverter : AttributeConverter<Market, String> {
    // Market enum 값을 데이터베이스에 저장할 마켓 코드 문자열로 변환하는 함수입니다.
    override fun convertToDatabaseColumn(attribute: Market?): String? =
        attribute?.code

    // 데이터베이스의 마켓 코드 문자열을 Market enum 값으로 복원하는 함수입니다.
    override fun convertToEntityAttribute(dbData: String?): Market? =
        dbData?.let { Market.fromCode(it) }

}
