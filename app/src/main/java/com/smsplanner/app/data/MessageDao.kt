package com.smsplanner.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    // ============================================================
    // INSERTION
    // ============================================================

    @Insert
    suspend fun insertMessage(
        message: MessageEntity
    ): Long


    // ============================================================
    // TOUS LES MESSAGES
    // ============================================================

    @Query("""
        SELECT * FROM messages
        ORDER BY timestamp DESC
    """)
    fun getAllMessages(): Flow<List<MessageEntity>>


    // ============================================================
    // NOMBRE TOTAL DE MESSAGES
    // ============================================================

    @Query("""
        SELECT COUNT(*) FROM messages
    """)
    fun getTotalMessageCount(): Flow<Int>


    // ============================================================
    // NOMBRE TOTAL DE SMS NON LUS
    // ============================================================

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE type = 'RECEIVED'
        AND isRead = 0
    """)
    fun getTotalUnreadCount(): Flow<Int>


    // ============================================================
    // MESSAGES D'UNE CONVERSATION
    // ============================================================

    @Query("""
        SELECT * FROM messages
        WHERE phoneNumber = :phoneNumber
        ORDER BY timestamp ASC
    """)
    fun getMessagesForNumber(
        phoneNumber: String
    ): Flow<List<MessageEntity>>


    // ============================================================
    // NOMBRE DE SMS NON LUS PAR CONVERSATION
    // ============================================================

    @Query("""
        SELECT COUNT(*) FROM messages
        WHERE phoneNumber = :phoneNumber
        AND type = 'RECEIVED'
        AND isRead = 0
    """)
    fun getUnreadCount(
        phoneNumber: String
    ): Flow<Int>


    // ============================================================
    // MARQUER LES SMS COMME LUS
    // ============================================================

    @Query("""
        UPDATE messages
        SET isRead = 1
        WHERE phoneNumber = :phoneNumber
        AND type = 'RECEIVED'
        AND isRead = 0
    """)
    suspend fun markMessagesAsRead(
        phoneNumber: String
    )


    // ============================================================
    // MARQUER UN SMS PRÉCIS COMME LU
    // ============================================================

    @Query("""
        UPDATE messages
        SET isRead = 1
        WHERE id = :messageId
        AND type = 'RECEIVED'
    """)
    suspend fun markMessageAsRead(
        messageId: Long
    )


    // ============================================================
    // MODIFIER LE STATUT D'UN MESSAGE
    // ============================================================

    @Query("""
        UPDATE messages
        SET status = :status
        WHERE id = :messageId
    """)
    suspend fun updateMessageStatus(
        messageId: Long,
        status: String
    )


    // ============================================================
    // RÉCUPÉRER UN MESSAGE PAR SON ID
    // ============================================================

    @Query("""
        SELECT * FROM messages
        WHERE id = :messageId
        LIMIT 1
    """)
    suspend fun getMessageById(
        messageId: Long
    ): MessageEntity?


    // ============================================================
    // RECHERCHE D'UN DOUBLON RÉCENT
    // ============================================================

    @Query("""
        SELECT * FROM messages
        WHERE phoneNumber = :phoneNumber
        AND message = :message
        AND type = :type
        AND timestamp >= :minimumTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findRecentDuplicate(
        phoneNumber: String,
        message: String,
        type: String,
        minimumTimestamp: Long
    ): MessageEntity?


    // ============================================================
    // RECHERCHE D'UN DOUBLON POUR UN SMS ENVOYÉ
    // ============================================================

    @Query("""
        SELECT * FROM messages
        WHERE phoneNumber = :phoneNumber
        AND message = :message
        AND type = 'SENT'
        AND timestamp >= :minimumTimestamp
        ORDER BY timestamp DESC
        LIMIT 1
    """)
    suspend fun findRecentSentDuplicate(
        phoneNumber: String,
        message: String,
        minimumTimestamp: Long
    ): MessageEntity?


    // ============================================================
    // RECHERCHE D'UNE PROGRAMMATION IDENTIQUE
    // ============================================================

    @Query("""
        SELECT * FROM messages
        WHERE phoneNumber = :phoneNumber
        AND message = :message
        AND type = 'SENT'
        AND timestamp = :scheduledTime
        AND status = 'message programmé'
        LIMIT 1
    """)
    suspend fun findScheduledDuplicate(
        phoneNumber: String,
        message: String,
        scheduledTime: Long
    ): MessageEntity?


    // ============================================================
    // SUPPRIMER UN MESSAGE
    // ============================================================

    @Query("""
        DELETE FROM messages
        WHERE id = :messageId
    """)
    suspend fun deleteMessage(
        messageId: Long
    )


    // ============================================================
    // SUPPRIMER TOUS LES MESSAGES
    // ============================================================

    @Query("""
        DELETE FROM messages
    """)
    suspend fun deleteAllMessages()
}
