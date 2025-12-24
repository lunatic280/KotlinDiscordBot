package com.DiscordBot.KotlinDiscordBot.member.service

import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.repository.MemberRepository
import org.springframework.stereotype.Service

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {


}