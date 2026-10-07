package com.smsplanner.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageEntity
import com.smsplanner.app.notification.NotificationHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "Action ignorée : ${intent.action}")
            return
        }

        val pendingResult = goAsync()

        try {
            val messages =
                Telephony.Sms.Intents.getMessagesFromIntent(intent)

            if (messages.isEmpty()) {
                Log.e(TAG, "Aucune partie de SMS reçue.")
                pendingResult.finish()
                return
            }

            /*
             * Récupération robuste du numéro de l'expéditeur.
             * On cherche la première partie qui contient une adresse.
             */
            val sender = messages
                .firstNotNullOfOrNull {
                    it.originatingAddress?.trim()
                }

            if (sender.isNullOrBlank()) {
                Log.e(TAG, "Numéro de l'expéditeur introuvable.")
                pendingResult.finish()
                return
            }

            /*
             * Reconstruction du contenu du SMS.
             * On ne supprime pas les espaces internes du message.
             */
            val messageBody = messages
                .joinToString(separator = "") {
                    it.messageBody.orEmpty()
                }
                .trim()

            if (messageBody.isBlank()) {
                Log.e(TAG, "Le contenu du SMS est vide.")
                pendingResult.finish()
                return
            }

            Log.d(TAG, "SMS reçu de : $sender")
            Log.d(TAG, "Nombre de parties : ${messages.size}")
            Log.d(TAG, "Contenu : $messageBody")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val applicationContext = context.applicationContext

                    val database =
                        AppDatabase.getDatabase(applicationContext)

                    val messageDao = database.messageDao()

                    val now = System.currentTimeMillis()

                    /*
                     * Anti-doublon sur les SMS reçus durant les
                     * dix dernières secondes.
                     */
                    val duplicate =
                        messageDao.findRecentDuplicate(
                            phoneNumber = sender,
                            message = messageBody,
                            type = "RECEIVED",
                            minimumTimestamp = now - 10_000
                        )

                    if (duplicate != null) {
                        Log.d(
                            TAG,
                            "Doublon détecté. SMS non enregistré."
                        )
                        return@launch
                    }

                    val insertedId =
                        messageDao.insertMessage(
                            MessageEntity(
                                phoneNumber = sender,
                                contactName = null,
                                message = messageBody,
                                timestamp = now,
                                type = "RECEIVED",
                                isRead = false,
                                status = "reçu"
                            )
                        )

                    Log.d(
                        TAG,
                        "SMS enregistré dans Room. ID : $insertedId"
                    )

                    try {
                        NotificationHelper.showSmsNotification(
                            context = applicationContext,
                            phoneNumber = sender,
                            contactName = sender,
                            message = messageBody
                        )

                        Log.d(
                            TAG,
                            "Notification du SMS affichée."
                        )
                    } catch (notificationException: Exception) {
                        /*
                         * Une erreur de notification ne doit pas
                         * empêcher le SMS d'être conservé dans Room.
                         */
                        Log.e(
                            TAG,
                            "Erreur lors de l'affichage de la notification.",
                            notificationException
                        )
                    }

                    try {
                        NotificationHelper.updateUnreadBadge(
                            applicationContext
                        )

                        Log.d(
                            TAG,
                            "Badge des SMS non lus mis à jour."
                        )
                    } catch (badgeException: Exception) {
                        /*
                         * Une erreur de badge ne doit pas empêcher
                         * l'enregistrement du SMS.
                         */
                        Log.e(
                            TAG,
                            "Erreur lors de la mise à jour du badge.",
                            badgeException
                        )
                    }

                } catch (databaseException: Exception) {
                    Log.e(
                        TAG,
                        "Erreur pendant l'enregistrement du SMS dans Room.",
                        databaseException
                    )
                } finally {
                    pendingResult.finish()
                }
            }

        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Erreur lors de la réception du SMS.",
                exception
            )

            pendingResult.finish()
        }
    }

    companion object {
        private const val TAG = "SMS_PLANNER"
    }
}