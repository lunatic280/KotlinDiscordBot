package com.DiscordBot.KotlinDiscordBot.money.domain

import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.coin.util.MarketCodeConverter
import com.DiscordBot.KotlinDiscordBot.money.dto.PositionDto
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "positions",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_positions_wallet_market",
            columnNames = ["wallet_id", "market"]
        )
    ]
)
class Position(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    val wallet: Wallet,


    @Column(name = "market", nullable = false)
    @Convert(converter = MarketCodeConverter::class)
    val market: Market,

    @Column(name = "market_count", nullable = false)
    private var marketCount: Long = 0,

    @Column(name = "total_cost", nullable = false)
    private var totalCost: Long = 0
    ) {

    companion object {
        fun createPosition(
            wallet: Wallet,
            market: Market,
            marketCount: Long,
            cost: Long
        ): Position {
            return Position(
                wallet = wallet,
                market = market,
                marketCount = marketCount,
                totalCost = cost
            )
        }
    }

    fun getMarketCount(): Long = marketCount
    fun getTotalCost(): Long = totalCost

    fun addMarketCount(addCount: Long) {
        marketCount += addCount
    }

    fun minMarketCount(minCount: Long) {
        require(minCount > 0) { "minCount값이 0보다 작거나 같습니다" }
        require(minCount <= marketCount) { "minCount값이 더 높습니다." }
        marketCount -= minCount
    }

    fun addTotalCost(addTotalCost: Long) {
        totalCost += addTotalCost
    }

    fun subtractCost(subtractCost: Long) {
        require(subtractCost > 0) {"subtractCost값이 0보다 작거나 같습니다"}
        require(subtractCost <= totalCost) { "subtractCost값이 더 많습니다"}
        totalCost -= subtractCost
    }

    //TODO set함수 만들어야함

    fun toDto(): PositionDto {
        return PositionDto(
            id = id!!,
            walletId = wallet.id!!,
            market = market,
            marketCount = marketCount,
            cost = totalCost
        )
    }
}