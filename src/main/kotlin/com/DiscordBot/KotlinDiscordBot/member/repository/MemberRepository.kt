package com.DiscordBot.KotlinDiscordBot.member.repository

import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository: JpaRepository<Member, Long> {

    fun existsByUserId(userId: String): Boolean

    fun findByUserId(userId: String): MemberDto
}