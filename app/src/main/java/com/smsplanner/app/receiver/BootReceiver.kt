package com.smsplanner.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

import com.smsplanner.app.data.AppDatabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        // Vérifier que le téléphone vient bien de redémarrer
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d(
            TAG,
            "Redémarrage du téléphone détecté"
        )

        // Garder le BroadcastReceiver actif pendant
        // que nous récupérons les messages dans Room.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val database =
                    AppDatabase.getDatabase(context)

                val dao =
                    database.programmationMessageDao()

                // Récupérer tous les messages encore programmés
                // depuis le Flow sous forme de liste.
                val messagesProgrammes =
                    dao.obtenirMessagesProgrammes().first()

                val alarmManager =
                    context.getSystemService(
                        Context.ALARM_SERVICE
                    ) as AlarmManager

                val maintenant =
                    System.currentTimeMillis()

                for (message in messagesProgrammes) {

                    // Si la date est déjà passée,
                    // on ne recrée pas l'alarme.
                    if (message.scheduledTime <= maintenant) {

                        Log.d(
                            TAG,
                            "Message ignoré car sa date est passée : ${message.id}"
                        )

                        continue
                    }

                    val alarmIntent =
                        Intent(
                            context,
                            SmsScheduleReceiver::class.java
                        ).apply {

                            putExtra(
                                SmsScheduleReceiver.EXTRA_MESSAGE_ID,
                                message.id
                            )
                        }

                    val pendingIntent =
                        PendingIntent.getBroadcast(
                            context,
                            message.id.toInt(),
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or
                                    PendingIntent.FLAG_IMMUTABLE
                        )

                    // Android 12 et versions supérieures
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                        if (alarmManager.canScheduleExactAlarms()) {

                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                message.scheduledTime,
                                pendingIntent
                            )

                            Log.d(
                                TAG,
                                "Alarme exacte reprogrammée : ${message.id}"
                            )

                        } else {

                            // Si les alarmes exactes ne sont pas autorisées,
                            // utiliser une alarme non exacte.
                            alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                message.scheduledTime,
                                pendingIntent
                            )

                            Log.d(
                                TAG,
                                "Alarme non exacte reprogrammée : ${message.id}"
                            )
                        }

                    } else {

                        // Android 11 et versions antérieures
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            message.scheduledTime,
                            pendingIntent
                        )

                        Log.d(
                            TAG,
                            "Alarme reprogrammée : ${message.id}"
                        )
                    }
                }

                Log.d(
                    TAG,
                    "${messagesProgrammes.size} message(s) programmé(s) vérifié(s) après redémarrage"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Erreur lors de la reprogrammation des SMS",
                    e
                )

            } finally {

                // Indiquer que le BroadcastReceiver a terminé
                pendingResult.finish()
            }
        }
    }

    companion object {

        private const val TAG =
            "SMS_PLANNER"
    }
}