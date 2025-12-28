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
            log.info("조회된 포지션 수: ${allPositions.size}")

            // 포지션이 없어도 지갑 업데이트는 진행 (코인 전량 판매 시에도 업데이트 필요)
            val prices: Map<Market, Long> = if (allPositions.isNotEmpty()) {
                val markets = allPositions.map { it.market }.distinct()
                log.info("조회할 마켓: $markets")
                val result = coinService.getCoinList(markets)
                    .block(Duration.ofSeconds(30)) ?: emptyMap()
                log.info("조회된 가격: $result")
                result
            } else {
                emptyMap()
            }

            val positionsByWallet = allPositions.groupBy { it.wallet.id }
            log.info("지갑별 포지션 그룹: ${positionsByWallet.keys}")

            // 모든 지갑 업데이트 (포지션 유무와 관계없이)
            val wallets = walletRepository.findAll()
            if (wallets.isEmpty()) {
                log.info("업데이트할 지갑이 없습니다.")
                return
            }

            wallets.forEach { wallet ->
                val walletPositions = positionsByWallet[wallet.id] ?: emptyList()
                log.info("지갑 ${wallet.id}: 포지션 ${walletPositions.size}개")

                val coinValue = walletPositions.sumOf { position ->
                    val price = prices[position.market] ?: 0L
                    val value = position.getMarketCount() * price
                    log.info("  - ${position.market}: ${position.getMarketCount()}개 × ${price}원 = ${value}원")
                    value
                }

                val oldWealth = wallet.getTotalWealth()
                wallet.updateTotalWealth(coinValue)
                log.info("지갑 ${wallet.id}: 코인가치=${coinValue}, 현금=${wallet.getCash()}, 총재산: ${oldWealth} → ${wallet.getTotalWealth()}")
            }
            walletRepository.saveAll(wallets)

            log.info("총 재산 업데이트 완료: ${wallets.size}개 지갑")

        } catch (e: Exception) {
            log.error("총 재산 업데이트 실패", e)
        }
    }
}
