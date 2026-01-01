package com.DiscordBot.KotlinDiscordBot.coin.service

import com.DiscordBot.KotlinDiscordBot.coin.data.TickerDto
import com.DiscordBot.KotlinDiscordBot.coin.util.Market
import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.money.domain.Position
import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import com.DiscordBot.KotlinDiscordBot.money.dto.PositionDto
import com.DiscordBot.KotlinDiscordBot.money.repository.PositionRepository
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CoinService(
    private val webClient: WebClient,
    private val walletRepository: WalletRepository,
    private val positionRepository: PositionRepository
) {

    private val log = LoggerFactory.getLogger(CoinService::class.java)

    fun getCoin(market: Market): Mono<TickerDto> {
        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/v1/ticker")
                    .queryParam("markets", market.code)
                    .build()
            }
            .retrieve()
            .bodyToFlux(TickerDto::class.java)
            .next()
            .doOnError { error -> log.warn("코인 시세 조회 실패: ${market.code}", error) }
    }

    fun getCoinList(markets: List<Market>): Mono<Map<Market, Long>> {
        if (markets.isEmpty()) {
            return Mono.just(emptyMap())
        }

        val marketCodes = markets.joinToString(",") { it.code }

        return webClient.get()
            .uri { uriBuilder ->
                uriBuilder.path("/v1/ticker")
                    .queryParam("markets", marketCodes)
                    .build()
            }
            .retrieve()
            .bodyToFlux(TickerDto::class.java)
            .collectList()
            .map { tickers ->
                tickers.mapNotNull { ticker ->
                    val market = Market.fromCode(ticker.market)
                    if (market != null) {
                        market to ticker.trade_price  // 현재가 사용 (opening_price는 시가)
                    } else null
                }.toMap()
            }
            .doOnError { error -> log.warn("코인 시세 일괄 조회 실패", error) }
    }

    @Transactional
    fun buyCoin(dto: MemberDto, market: Market, count: Long, cost: Long): PositionDto {

        val findWallet = walletRepository.findByMemberId(dto.id!!)
        val totalCost = cost * count

        if (findWallet.getCash() < totalCost) {
            throw IllegalArgumentException("보유 현금이 부족합니다.")
        }

        val position = Position(
            wallet = findWallet,
            market = market,
            marketCount = count,
            totalCost = totalCost
        )

        //데이터베이스에 position이 존재하지 않는다면 if문 안에서 처리
        val findPosition = positionRepository.findByWalletIdAndMarket(findWallet.id!!, market)
        val resultPosition: Position
        if (findPosition == null) {
            //포지션에서 산 내용 등록
            resultPosition = positionRepository.save(position)
            //보유 현급에서 코인값 차감
            findWallet.subtractCash(totalCost)
        } else {
            //포지션에서 산 가격 설정
            findPosition.addTotalCost(totalCost)
            //코인 개수
            findPosition.addMarketCount(count)
            //지갑에서 전체 포지션 가격 차감
            findWallet.subtractCash(totalCost)
            resultPosition = findPosition
        }

        // totalWealth는 스케줄러가 1분마다 시장가로 업데이트함
        // 여기서 업데이트하면 시장가 이익/손실이 원가로 리셋됨

        return resultPosition.toDto()
    }

    @Transactional
    fun sellCoin(dto: MemberDto, market: Market, count: Long, cost: Long): PositionDto {

        val findWallet = walletRepository.findByMemberId(dto.id!!)
        val findPosition = positionRepository.findByWalletIdAndMarket(findWallet.id!!, market)

        //판매 금액
        val totalCost = count * cost
        if (findPosition == null || findPosition.getMarketCount() < count) {
            throw IllegalArgumentException("코인을 구매하지 않았거나, 보유 개수보다 팔아야할 개수가 더 많습니다.")
        }

        //차감할 원가
        val costToSubtract = (findPosition.getTotalCost() * count) / findPosition.getMarketCount()


        findWallet.addCash(totalCost)
        findPosition.minMarketCount(count)
        findPosition.subtractCost(costToSubtract)

        val resultDto = findPosition.toDto()
        //position에 count가 없으면 엔티티 삭제
        if (findPosition.getMarketCount() == 0L) {
            positionRepository.delete(findPosition)
        }

        // totalWealth는 스케줄러가 1분마다 시장가로 업데이트함

        return resultDto
    }
}
