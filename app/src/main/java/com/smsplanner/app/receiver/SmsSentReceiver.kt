package com.smsplanner.app.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.smsplanner.app.data.AppDatabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsSentReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        /*
         * IMPORTANT :
         * On récupère le resultCode immédiatement,
         * avant de lancer la coroutine.
         */
        val smsResultCode =
            resultCode

        val messageId =
            intent.getLongExtra(
                EXTRA_MESSAGE_ID,
                -1L
            )

        val programmationId =
            intent.getLongExtra(
                EXTRA_PROGRAMMATION_ID,
                -1L
            )

        if (messageId == -1L) {

            Log.e(
                TAG,
                "ID du message introuvable"
            )

            return
        }

        Log.d(
            TAG,
            "Confirmation SMS reçue : " +
                    "messageId=$messageId, " +
                    "programmationId=$programmationId, " +
                    "resultCode=$smsResultCode"
        )

        val pendingResult =
            goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val database =
                    AppDatabase.getDatabase(
                        context.applicationContext
                    )

                val messageDao =
                    database.messageDao()

                val programmationDao =
                    database.programmationMessageDao()

                /*
                 * ==================================================
                 * ENVOI CONFIRMÉ
                 * ==================================================
                 */

                if (
                    smsResultCode ==
                    Activity.RESULT_OK
                ) {

                    messageDao.updateMessageStatus(
                        messageId = messageId,
                        status = "envoyé"
                    )

                    if (
                        programmationId != -1L
                    ) {

                        programmationDao.modifierStatut(
                            id = programmationId,
                            status = "envoyé"
                        )
                    }

                    Log.d(
                        TAG,
                        "SMS confirmé comme envoyé : " +
                                "message=$messageId"
                    )

                } else {

                    /*
                     * ==================================================
                     * ÉCHEC
                     * ==================================================
                     */

                    Log.e(
                        TAG,
                        "Android indique un échec d'envoi : " +
                                "resultCode=$smsResultCode"
                    )

                    messageDao.updateMessageStatus(
                        messageId = messageId,
                        status = "échec"
                    )

                    if (
                        programmationId != -1L
                    ) {

                        programmationDao.modifierStatut(
                            id = programmationId,
                            status = "échec"
                        )
                    }
                }

            } catch (exception: Exception) {

                Log.e(
                    TAG,
                    "Erreur dans SmsSentReceiver : " +
                            exception.message,
                    exception
                )

            } finally {

                pendingResult.finish()
            }
        }
    }

    companion object {

        const val ACTION_SMS_SENT =
            "com.smsplanner.app.SMS_SENT"

        const val EXTRA_MESSAGE_ID =
            "message_id"

        const val EXTRA_PROGRAMMATION_ID =
            "programmation_id"

        private const val TAG =
            "SMS_PLANNER"
    }
}