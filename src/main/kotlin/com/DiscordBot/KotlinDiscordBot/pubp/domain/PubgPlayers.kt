package com.DiscordBot.KotlinDiscordBot.pubp.domain

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

    fun getPlayerId(): String = playerId

    fun updatePlayerId(newPlayerId: String) {
        require(newPlayerId.isNotBlank()) { "playerId is a blank" }
        playerId = newPlayerId.trim()
    }
    companion object {
        fun create(playerId: String, member: Member) : PubgPlayers {
            require(playerId.isNotBlank()) { "playerId is a blank" }
            return PubgPlayers(
                playerId = playerId.trim(),
                member = member
            )
        }
    }
}