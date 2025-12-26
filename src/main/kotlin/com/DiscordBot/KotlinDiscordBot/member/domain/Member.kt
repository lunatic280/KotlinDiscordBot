package com.DiscordBot.KotlinDiscordBot.member.domain

import com.DiscordBot.KotlinDiscordBot.member.data.MemberDto
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_members_user_id",
        columnNames = ["user_id"]
    )
    ]
)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    //디스코드 기본 이름
    @Column(nullable = false)
    val username: String,

    //디스코드 아이디
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: String,

    @Column(nullable = false)
    private var nickname: String,


    //레벨
    @Column(name = "level")
    private var level: Long = 0,

    //마지막 보상일
    @Column(name = "last_daily_reward")
    private var lastDailyReward: LocalDate? = null

) {

    //레벨 관련 메서드
    fun getLevel() = level
    private fun levelUp() {
        level += 1
    }

    //닉네임 관련 메서드
    fun getNickname() = nickname
    fun updateNickname(newNickname: String) {
        nickname = newNickname
    }

    companion object {
        fun create(
            username: String,
            userId: String,
            nickname: String
        ): Member {
            return Member(
                username = username,
                userId = userId,
                nickname = nickname
            )
        }
    }

    fun toDto(): MemberDto {
        return MemberDto(
            id = this.id,
            username = this.username,
            userId = this.userId,
            nickname = this.nickname,
            level = this.level,
            lastDailyReward = this.lastDailyReward
        )
    }

}