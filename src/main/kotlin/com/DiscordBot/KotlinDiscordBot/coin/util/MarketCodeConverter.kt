package com.DiscordBot.KotlinDiscordBot.coin.util

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class MarketCodeConverter : AttributeConverter<Market, String> {
    override fun convertToDatabaseColumn(attribute: Market?): String? =
        attribute?.code

    override fun convertToEntityAttribute(dbData: String?): Market? =
        dbData?.let { Market.fromCode(it) }

}