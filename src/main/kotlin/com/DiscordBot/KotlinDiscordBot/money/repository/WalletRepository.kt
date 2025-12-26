package com.DiscordBot.KotlinDiscordBot.money.repository

import com.DiscordBot.KotlinDiscordBot.money.domain.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository: JpaRepository<Wallet, Long> {
}