package com.DiscordBot.KotlinDiscordBot.money.repository

import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import com.DiscordBot.KotlinDiscordBot.money.dto.WalletDto
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository: JpaRepository<Wallet, Long> {

    // 멤버 PK로 지갑 엔티티를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByMemberId(memberId: Long): Wallet

    // Discord userId로 지갑 엔티티를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByMemberUserId(userId: String): Wallet?

    // 총재산 내림차순 기준 상위 10개 지갑을 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findTop10ByOrderByTotalWealthDesc(): List<Wallet>
}
