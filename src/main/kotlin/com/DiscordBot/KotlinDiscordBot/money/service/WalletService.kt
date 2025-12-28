package com.DiscordBot.KotlinDiscordBot.money.service

import com.DiscordBot.KotlinDiscordBot.money.dto.WalletDto
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class WalletService(
    private val walletRepository: WalletRepository
) {

    fun getWalletByUserId(userId: String): WalletDto? {
        return walletRepository.findByMemberUserId(userId)?.toDto()
    }

    fun getTop10Ranking(): List<WalletDto> {
        return walletRepository.findTop10ByOrderByTotalWealthDesc()
            .map { it.toDto() }
    }
}