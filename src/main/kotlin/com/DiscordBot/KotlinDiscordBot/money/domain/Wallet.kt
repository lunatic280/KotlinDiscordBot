package com.DiscordBot.KotlinDiscordBot.money.domain

import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.money.dto.WalletDto
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "wallets",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_wallet_member",
        columnNames = ["member_id"]
    )]
)
class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    val member: Member,

    //보유 현금
    @Column(name = "cash", nullable = false)
    private var cash: Long,

    @Column(name = "total_wealth", nullable = false)
    private var totalWealth: Long


) {

    companion object {
        // 멤버에게 기본 현금과 총재산을 가진 지갑을 생성하는 팩토리 함수입니다.
        fun createWallet(member: Member): Wallet {
            return Wallet(member = member, cash = 1_000_000_000L, totalWealth = 1_000_000_000L)
        }
    }

    //cash 관련 함수
    // 현재 보유 현금 값을 반환하는 함수입니다.
    fun getCash(): Long = cash

    // 지갑 현금을 지정 금액만큼 증가시키는 함수입니다.
    fun addCash(amount: Long) {
        require(amount >= 0) { "amount must be >= 0" }
        cash = Math.addExact(cash, amount)
    }

    // 지갑 현금을 지정 금액만큼 차감하는 함수입니다.
    fun subtractCash(amount: Long) {
        require(amount >= 0) { "amount must be >= 0" }
        require(cash >= amount) { "insufficient cash" }
        cash = Math.subtractExact(cash, amount)
    }

    //totalWealth 관련 함수
    // 총재산에서 현금을 뺀 코인 평가액을 계산하는 함수입니다.
    fun getCoinValue() = totalWealth - cash
    // 현재 총재산 값을 반환하는 함수입니다.
    fun getTotalWealth(): Long = totalWealth
    // 코인 평가액을 반영해 총재산을 갱신하는 함수입니다.
    fun updateTotalWealth(coinValue: Long) {
        totalWealth = this.cash + coinValue
    }

    // 지갑 엔티티를 외부 전달용 DTO로 변환하는 함수입니다.
    fun toDto(): WalletDto {
        return WalletDto(
            id = id,
            memberId = member.userId,
            cash = cash,
            totalWealth = totalWealth
        )
    }
}
