package com.DiscordBot.KotlinDiscordBot.member.service

import com.DiscordBot.KotlinDiscordBot.member.data.MemberCreateDto
import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {


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
        return savedMember.toDto()
    }
}