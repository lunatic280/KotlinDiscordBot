package com.DiscordBot.KotlinDiscordBot.pubg.domain

import com.DiscordBot.KotlinDiscordBot.member.domain.Member
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "pubgplays",
    uniqueConstraints = [UniqueConstraint(
        name = "uk_pubgusers_player_id",
        columnNames = ["player_id"]
    )
    ]
)
class PubgPlayers(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    //배그 아이디
    @Column(name = "player_id", nullable = false)
    private var playerId: String,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "member_id",
        nullable = false,
        unique = true,
        foreignKey = ForeignKey(name = "fk_pubgplayers_member")
    )
    val member: Member
) {

    // 등록된 PUBG 플레이어 ID를 반환하는 함수입니다.
    fun getPlayerId(): String = playerId

    // PUBG 플레이어 ID를 공백 제거 후 새 값으로 변경하는 함수입니다.
    fun updatePlayerId(newPlayerId: String) {
        require(newPlayerId.isNotBlank()) { "playerId is a blank" }
        playerId = newPlayerId.trim()
    }
    companion object {
        // 멤버와 플레이어 ID를 연결하는 PUBG 플레이어 엔티티를 생성하는 팩토리 함수입니다.
        fun create(playerId: String, member: Member) : PubgPlayers {
            require(playerId.isNotBlank()) { "playerId is a blank" }
            return PubgPlayers(
                playerId = playerId.trim(),
                member = member
            )
        }
    }
}
