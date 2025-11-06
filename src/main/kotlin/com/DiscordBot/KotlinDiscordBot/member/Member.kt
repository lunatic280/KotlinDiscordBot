package com.DiscordBot.KotlinDiscordBot.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.math.BigInteger

@Entity
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val username: String,

    @Column(nullable = false, unique = true)
    val userId: String,

    @Column(nullable = false)
    private var money: Long = 0,

) {
    protected constructor() : this(
        id = null,
        username = "",
        userId = "",
        money = 0,
    )

    companion object {
        fun create(
            username: String,
            userId: String,
            money: Long
        ): Member {
            return Member(
                username = username,
                userId = userId,
                money = money
            )
        }
    }

}