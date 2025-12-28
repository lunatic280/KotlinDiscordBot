package com.DiscordBot.KotlinDiscordBot.money.service

import com.DiscordBot.KotlinDiscordBot.money.dto.WalletDto
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WalletService(
    private val walletRepository: WalletRepository
) {
    private val log = LoggerFactory.getLogger(WalletService::class.java)

    fun getWalletByUserId(userId: String): WalletDto? {
        val getWallet = walletRepository.findByMemberUserId(userId)?.toDto()
        log.info("getWalletByUserId() -> $getWallet, $userId")
        return walletRepository.findByMemberUserId(userId)?.toDto()
    }

    fun getTop10Ranking(): List<WalletDto> {
        val getRanking = walletRepository.findTop10ByOrderByTotalWealthDesc()
            .map { it.toDto() }
        log.info("getTop10Ranking() -> $getRanking")
        return getRanking
    }
}