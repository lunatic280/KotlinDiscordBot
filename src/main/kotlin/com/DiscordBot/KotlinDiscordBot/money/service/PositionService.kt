package com.DiscordBot.KotlinDiscordBot.money.service

import com.DiscordBot.KotlinDiscordBot.money.dto.PositionMarketDto
import com.DiscordBot.KotlinDiscordBot.money.repository.PositionRepository
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PositionService(
    private val positionRepository: PositionRepository
) {

    fun getPositionMarketList(walletId: Long): List<PositionMarketDto> {
        val findWallet = positionRepository.findByWalletId(walletId)
        val makeList = findWallet.map { PositionMarketDto(it.market, it.getMarketCount()) }
        //TODO 나중에 현재 평가 가격 추가
        return makeList
    }
}