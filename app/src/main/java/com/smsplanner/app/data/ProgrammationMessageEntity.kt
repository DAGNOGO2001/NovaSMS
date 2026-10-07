package com.smsplanner.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages_programmes")
data class ProgrammationMessageEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val messageId: Long,

    val phoneNumber: String,

    val message: String,

    val scheduledTime: Long,

    val status: String = "message programmé"
)