package com.smsplanner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val phoneNumber: String,

    val contactName: String? = null,

    val message: String,

    val timestamp: Long,

    val type: String,

    val isRead: Boolean = false,

    val status: String = "envoyé"
)