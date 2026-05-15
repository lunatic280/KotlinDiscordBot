package com.DiscordBot.KotlinDiscordBot.member.service

import com.DiscordBot.KotlinDiscordBot.member.data.MemberCreateDto
import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val walletRepository: WalletRepository
) {

    private val log = LoggerFactory.getLogger(MemberService::class.java)


    // Discord userId로 등록된 멤버가 있는지 확인하는 함수입니다.
    fun existsMember(userId: String): Boolean {
        return memberRepository.existsByUserId(userId)
    }

    // Discord userId에 해당하는 멤버 정보를 조회하는 함수입니다.
    fun getMember(userId: String): MemberDto {
        val get = memberRepository.findByUserId(userId)
        log.info("getMember() -> ${get}")
        return get
    }


    // 새 멤버를 등록하고 기본 지갑을 함께 생성하는 함수입니다.
    @Transactional
    fun memberRegistration(memberCreateDto: MemberCreateDto): MemberDto? {
        if (memberRepository.existsByUserId(memberCreateDto.userId)) {
            log.info("memberRegistration() -> null 반환")
            return null
        }
        val member = Member.create(
            username = memberCreateDto.username,
            userId = memberCreateDto.userId,
            nickname = memberCreateDto.nickname
        )
        val savedMember = memberRepository.save(member)
        log.info("memberRegistration() -> $savedMember")

        //멤버의 지갑도 같이 추가
        val wallet = Wallet.createWallet(savedMember)
        val savedWallet = walletRepository.save(wallet)
        log.info("$savedMember 의 wallet 저장")
        return savedMember.toDto()
    }
}
