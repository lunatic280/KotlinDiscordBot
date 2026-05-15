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
    // 현재 멤버 레벨 값을 반환하는 함수입니다.
    fun getLevel() = level
    // 멤버 레벨을 1 증가시키는 내부 함수입니다.
    private fun levelUp() {
        level += 1
    }

    //닉네임 관련 메서드
    // 현재 멤버 닉네임을 반환하는 함수입니다.
    fun getNickname() = nickname
    // 멤버 닉네임을 새 값으로 변경하는 함수입니다.
    fun updateNickname(newNickname: String) {
        nickname = newNickname
    }

    companion object {
        // 신규 멤버 엔티티를 생성하는 팩토리 함수입니다.
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

    // 멤버 엔티티를 외부 전달용 DTO로 변환하는 함수입니다.
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

    // 현재 멤버 값을 기반으로 새 엔티티 인스턴스를 만드는 함수입니다.
    fun toEntity(): Member{
        return Member(
            id = this.id,
            username = this.username,
            userId = this.userId,
            nickname = this.nickname,
            level = this.level,
            lastDailyReward = this.lastDailyReward
        )
    }

}
