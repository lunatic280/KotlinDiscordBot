package com.DiscordBot.KotlinDiscordBot.member.repository

import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository: JpaRepository<Member, Long> {

    // Discord userId로 멤버 존재 여부를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun existsByUserId(userId: String): Boolean

    // Discord userId로 멤버 DTO를 조회하는 Spring Data JPA 파생 쿼리 함수입니다.
    fun findByUserId(userId: String): MemberDto
}
