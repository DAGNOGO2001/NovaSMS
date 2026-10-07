package com.smsplanner.app.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log

import com.smsplanner.app.data.AppDatabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        // ID de la programmation
        val programmationId =
            intent.getLongExtra(
                EXTRA_MESSAGE_ID,
                -1L
            )

        if (programmationId == -1L) {

            Log.e(
                TAG,
                "ID de la programmation introuvable"
            )

            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val database =
                    AppDatabase.getDatabase(
                        context.applicationContext
                    )

                val programmationDao =
                    database.programmationMessageDao()

                val messageDao =
                    database.messageDao()

                /*
                 * ==================================================
                 * RÉCUPÉRATION DE LA PROGRAMMATION
                 * ==================================================
                 */

                val messageProgramme =
                    programmationDao
                        .obtenirMessageProgramme(
                            programmationId
                        )

                if (messageProgramme == null) {

                    Log.e(
                        TAG,
                        "Programmation introuvable : $programmationId"
                    )

                    return@launch
                }

                /*
                 * ==================================================
                 * VÉRIFICATION DU STATUT
                 * ==================================================
                 */

                if (
                    messageProgramme.status !=
                    "message programmé"
                ) {

                    Log.d(
                        TAG,
                        "Programmation déjà traitée : $programmationId"
                    )

                    return@launch
                }

                /*
                 * ==================================================
                 * RÉCUPÉRATION DU MESSAGE
                 * ==================================================
                 */

                val message =
                    messageDao.getMessageById(
                        messageProgramme.messageId
                    )

                if (message == null) {

                    Log.e(
                        TAG,
                        "Message introuvable : " +
                                messageProgramme.messageId
                    )

                    programmationDao.modifierStatut(
                        id = programmationId,
                        status = "échec"
                    )

                    return@launch
                }

                /*
                 * ==================================================
                 * STATUT : ENVOI EN COURS
                 * ==================================================
                 */

                messageDao.updateMessageStatus(
                    messageId = message.id,
                    status = "envoi"
                )

                /*
                 * ==================================================
                 * INTENT DE CONFIRMATION D'ENVOI
                 * ==================================================
                 */

                val sentIntent =
                    Intent(
                        context,
                        SmsSentReceiver::class.java
                    ).apply {

                        action =
                            SmsSentReceiver.ACTION_SMS_SENT

                        putExtra(
                            SmsSentReceiver.EXTRA_MESSAGE_ID,
                            message.id
                        )

                        putExtra(
                            SmsSentReceiver.EXTRA_PROGRAMMATION_ID,
                            programmationId
                        )
                    }

                val sentPendingIntent =
                    PendingIntent.getBroadcast(
                        context,
                        message.id.toInt(),
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or
                                PendingIntent.FLAG_IMMUTABLE
                    )

                /*
                 * ==================================================
                 * ENVOI DU SMS
                 * ==================================================
                 */

                val smsManager =
                    SmsManager.getDefault()

                smsManager.sendTextMessage(
                    messageProgramme.phoneNumber,
                    null,
                    messageProgramme.message,
                    sentPendingIntent,
                    null
                )

                /*
                 * IMPORTANT :
                 *
                 * On ne met pas "envoyé" ici.
                 *
                 * SmsSentReceiver recevra la confirmation
                 * réelle d'Android.
                 */

                Log.d(
                    TAG,
                    "Tentative d'envoi du SMS programmée : " +
                            "programmation=$programmationId, " +
                            "message=${message.id}"
                )

            } catch (e: Exception) {

                /*
                 * ==================================================
                 * ERREUR DIRECTE
                 * ==================================================
                 */

                Log.e(
                    TAG,
                    "Erreur lors de l'envoi du SMS : ${e.message}",
                    e
                )

                try {

                    val database =
                        AppDatabase.getDatabase(
                            context.applicationContext
                        )

                    val programmationDao =
                        database.programmationMessageDao()

                    val messageDao =
                        database.messageDao()

                    val messageProgramme =
                        programmationDao
                            .obtenirMessageProgramme(
                                programmationId
                            )

                    if (messageProgramme != null) {

                        messageDao.updateMessageStatus(
                            messageId =
                                messageProgramme.messageId,
                            status = "échec"
                        )
                    }

                    programmationDao.modifierStatut(
                        id = programmationId,
                        status = "échec"
                    )

                } catch (databaseException: Exception) {

                    Log.e(
                        TAG,
                        "Impossible de modifier le statut : " +
                                databaseException.message,
                        databaseException
                    )
                }

            } finally {

                pendingResult.finish()
            }
        }
    }

    companion object {

        const val EXTRA_MESSAGE_ID =
            "message_programme_id"

        private const val TAG =
            "SMS_PLANNER"
    }
}