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
        // 지갑과 마켓 정보를 기반으로 새 코인 포지션을 생성하는 팩토리 함수입니다.
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

    // 현재 포지션의 보유 코인 개수를 반환하는 함수입니다.
    fun getMarketCount(): Long = marketCount
    // 현재 포지션의 총 매수 원가를 반환하는 함수입니다.
    fun getTotalCost(): Long = totalCost

    // 포지션의 보유 코인 개수를 증가시키는 함수입니다.
    fun addMarketCount(addCount: Long) {
        marketCount += addCount
    }

    // 포지션의 보유 코인 개수를 차감하는 함수입니다.
    fun minMarketCount(minCount: Long) {
        require(minCount > 0) { "minCount값이 0보다 작거나 같습니다" }
        require(minCount <= marketCount) { "minCount값이 더 높습니다." }
        marketCount -= minCount
    }

    // 포지션의 총 매수 원가를 증가시키는 함수입니다.
    fun addTotalCost(addTotalCost: Long) {
        totalCost += addTotalCost
    }

    // 포지션의 총 매수 원가를 차감하는 함수입니다.
    fun subtractCost(subtractCost: Long) {
        require(subtractCost > 0) {"subtractCost값이 0보다 작거나 같습니다"}
        require(subtractCost <= totalCost) { "subtractCost값이 더 많습니다"}
        totalCost -= subtractCost
    }

    //TODO set함수 만들어야함

    // 포지션 엔티티를 외부 전달용 DTO로 변환하는 함수입니다.
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
