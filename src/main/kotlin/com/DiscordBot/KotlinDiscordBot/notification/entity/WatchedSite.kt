package com.DiscordBot.KotlinDiscordBot.notification.entity

import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

//@Entity
@Table(name = "watched_site",
    indexes = [
        Index(name = "idx_watched_site_alert_channel", columnList = "alert_channel_id"),
        Index(name = "idx_watched_site_user_id", columnList = "user_id"),
    Index(name = "idx_watched_site_enabled_next_check", columnList = "user_name"),
    ])
class WatchedSite(

) {

}