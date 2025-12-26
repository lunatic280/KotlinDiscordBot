package com.DiscordBot.KotlinDiscordBot.money.domain

import com.DiscordBot.KotlinDiscordBot.member.domain.Member
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

    @Column(name = "cash", nullable = false)
    private var cash: Long


) {

    companion object {
        fun createWallet(member: Member): Wallet {
            return Wallet(member = member, cash = 1_000_000_000L)
        }
    }

    //cash 관련 함수
    fun getCash(): Long = cash

    fun addCash(amount: Long) {
        require(amount >= 0) { "amount must be >= 0" }
        cash = Math.addExact(cash, amount)
    }

    fun subtractCash(amount: Long) {
        require(amount >= 0) { "amount must be >= 0" }
        require(cash >= amount) { "insufficient cash" }
        cash = Math.subtractExact(cash, amount)
    }
}