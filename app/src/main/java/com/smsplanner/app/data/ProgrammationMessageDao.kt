package com.smsplanner.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgrammationMessageDao {

    /*
     * Enregistre un message programmé.
     *
     * Room retourne automatiquement l'ID généré
     * pour ce nouveau message.
     */
    @Insert
    suspend fun insererMessageProgramme(
        message: ProgrammationMessageEntity
    ): Long

    /*
     * Récupère tous les messages programmés.
     *
     * Le résultat se met à jour automatiquement
     * lorsqu'une modification est effectuée dans Room.
     */
    @Query("""
        SELECT * FROM messages_programmes
        ORDER BY scheduledTime ASC
    """)
    fun obtenirTousLesMessagesProgrammes():
            Flow<List<ProgrammationMessageEntity>>

    /*
     * Récupère un message programmé grâce à son ID.
     */
    @Query("""
        SELECT * FROM messages_programmes
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun obtenirMessageProgramme(
        id: Long
    ): ProgrammationMessageEntity?

    /*
     * Récupère uniquement les messages
     * qui sont encore programmés.
     *
     * Le plus récent est affiché en premier.
     */
    @Query("""
        SELECT * FROM messages_programmes
        WHERE status = 'message programmé'
        ORDER BY scheduledTime DESC
    """)
    fun obtenirMessagesProgrammes():
            Flow<List<ProgrammationMessageEntity>>

    /*
     * Modifie le statut d'un message.
     *
     * Statuts utilisés :
     *
     * message programmé
     * envoi
     * envoyé
     * échec
     */
    @Query("""
        UPDATE messages_programmes
        SET status = :status
        WHERE id = :id
    """)
    suspend fun modifierStatut(
        id: Long,
        status: String
    )

    /*
     * Supprime un message programmé.
     */
    @Delete
    suspend fun supprimerMessageProgramme(
        message: ProgrammationMessageEntity
    )

    /*
     * Supprime un message programmé grâce à son ID.
     */
    @Query("""
        DELETE FROM messages_programmes
        WHERE id = :id
    """)
    suspend fun supprimerParId(
        id: Long
    )
}