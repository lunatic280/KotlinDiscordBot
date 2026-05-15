package com.DiscordBot.KotlinDiscordBot.breakfast.service

import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastInfo
import com.DiscordBot.KotlinDiscordBot.breakfast.domain.BreakfastUniversity
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BreakfastQueryService(
    private val catholicBreakfastClient: CatholicBreakfastClient,
    private val cnuBreakfastClient: CnuBreakfastClient
) {

    // 선택된 대학에 맞는 조식 클라이언트로 조회를 위임하는 함수입니다.
    fun fetch(university: BreakfastUniversity, targetDate: LocalDate): BreakfastInfo {
        return when (university) {
            BreakfastUniversity.CATHOLIC -> catholicBreakfastClient.fetch(targetDate)
            BreakfastUniversity.CNU -> cnuBreakfastClient.fetch(targetDate)
        }
    }
}
