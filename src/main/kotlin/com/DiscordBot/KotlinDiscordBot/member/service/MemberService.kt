package com.DiscordBot.KotlinDiscordBot.member.service

import com.DiscordBot.KotlinDiscordBot.member.data.MemberCreateDto
import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import com.DiscordBot.KotlinDiscordBot.money.repository.WalletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val walletRepository: WalletRepository
) {
    fun existsMember(userId: String): Boolean {
        return memberRepository.existsByUserId(userId)
    }

    fun getMember(userId: String): MemberDto {
        val get = memberRepository.findByUserId(userId)
        return get
    }


    @Transactional
    fun memberRegistration(memberCreateDto: MemberCreateDto): MemberDto? {
        if (memberRepository.existsByUserId(memberCreateDto.userId)) {
            return null
        }
        val member = Member.create(
            username = memberCreateDto.username,
            userId = memberCreateDto.userId,
            nickname = memberCreateDto.nickname
        )
        val savedMember = memberRepository.save(member)

        //멤버의 지갑도 같이 추가
        val wallet = Wallet.createWallet(savedMember)
        val savedWallet = walletRepository.save(wallet)
        return savedMember.toDto()
    }
}