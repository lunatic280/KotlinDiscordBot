package com.DiscordBot.KotlinDiscordBot.money

import com.DiscordBot.KotlinDiscordBot.coin.service.CoinService
import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.money.repository.PositionRepository
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration


@Component
class WealthUpdateScheduler(
    private val walletRepository: WalletRepository,
    private val positionRepository: PositionRepository,
    private val coinService: CoinService
) {
    private val log = LoggerFactory.getLogger(WealthUpdateScheduler::class.java)


    @Scheduled(fixedRate = 60_000) //1분
    @Transactional
    fun updateAllWealth() {
        log.info("총 재산 일괄 업데이트 시작")

        try {
            val allPositions = positionRepository.findAll()
            if (allPositions.isEmpty()) {
                log.info("업데이트할 포지션이 없습니다.")
                return
            }

            val markets = allPositions
                .map { it.market }
                .distinct()

            if (markets.isEmpty()) {
                log.info("마켓 포지션이 없습니다")
                return
            }

            val prices: Map<Market, Long> = coinService.getCoinList(markets)
                .block(Duration.ofSeconds(30)) ?: emptyMap()

            val positionsByWallet = allPositions.groupBy { it.wallet.id }

            val wallets = walletRepository.findAll()
            wallets.forEach { wallet ->
                val walletPositions = positionsByWallet[wallet.id] ?: emptyList()

                val coinValue = walletPositions.sumOf { position ->
                    val price = prices[position.market] ?: 0L
                    position.getMarketCount() * price
                }

                wallet.updateTotalWealth(coinValue)
            }
            walletRepository.saveAll(wallets)

            log.info("총 재산 업데이트 완료: ${wallets.size}개 지갑")

        } catch (e: Exception) {
            log.error("총 재산 업데이트 실패", e)
        }
    }
}