package com.smsplanner.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

import com.smsplanner.app.MainActivity
import com.smsplanner.app.R
import com.smsplanner.app.data.AppDatabase

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object NotificationHelper {

    // ============================================================
    // CANAUX DE NOTIFICATION
    // ============================================================

    private const val CHANNEL_ID_VIBRATION =
        "smsplanner_sms_channel_vibration"

    private const val CHANNEL_ID_SILENT =
        "smsplanner_sms_channel_silent"

    private const val CHANNEL_NAME =
        "Messages SMS"

    private const val CHANNEL_DESCRIPTION =
        "Notifications des nouveaux messages SMS"


    // ============================================================
    // ID UNIQUE DE LA NOTIFICATION SMS
    // ============================================================

    private const val SMS_NOTIFICATION_ID =
        900001


    // ============================================================
    // PRÉFÉRENCES
    // ============================================================

    private const val PREFERENCES_NAME =
        "smsplanner_conversations"

    private const val MUTED_NUMBERS_KEY =
        "muted_numbers"

    private const val NOTIFICATIONS_ENABLED_KEY =
        "notifications_enabled"

    private const val VIBRATION_ENABLED_KEY =
        "vibration_enabled"


    // ============================================================
    // NORMALISER UN NUMÉRO
    // ============================================================

    private fun normalizePhoneNumber(
        phoneNumber: String
    ): String {

        var number =
            phoneNumber
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")
                .replace(".", "")

        if (number.startsWith("+225")) {

            number =
                number.removePrefix("+225")

        } else if (
            number.startsWith("225") &&
            number.length > 10
        ) {

            number =
                number.removePrefix("225")
        }

        return number.filter {
            it.isDigit()
        }
    }


    // ============================================================
    // PRÉFÉRENCES
    // ============================================================

    private fun getPreferences(
        context: Context
    ) =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )


    // ============================================================
    // NOTIFICATIONS ACTIVÉES
    // ============================================================

    fun areNotificationsEnabled(
        context: Context
    ): Boolean {

        return getPreferences(context)
            .getBoolean(
                NOTIFICATIONS_ENABLED_KEY,
                true
            )
    }


    // ============================================================
    // ACTIVER / DÉSACTIVER LES NOTIFICATIONS
    // ============================================================

    fun setNotificationsEnabled(
        context: Context,
        enabled: Boolean
    ) {

        getPreferences(context)
            .edit()
            .putBoolean(
                NOTIFICATIONS_ENABLED_KEY,
                enabled
            )
            .apply()


        if (!enabled) {

            try {

                NotificationManagerCompat
                    .from(context)
                    .cancel(
                        SMS_NOTIFICATION_ID
                    )

            } catch (
                exception: SecurityException
            ) {

                exception.printStackTrace()
            }
        }
    }


    // ============================================================
    // VIBRATION
    // ============================================================

    fun isVibrationEnabled(
        context: Context
    ): Boolean {

        return getPreferences(context)
            .getBoolean(
                VIBRATION_ENABLED_KEY,
                true
            )
    }


    fun setVibrationEnabled(
        context: Context,
        enabled: Boolean
    ) {

        getPreferences(context)
            .edit()
            .putBoolean(
                VIBRATION_ENABLED_KEY,
                enabled
            )
            .apply()
    }


    // ============================================================
    // NUMÉROS EN SOURDINE
    // ============================================================

    fun getMutedNumbers(
        context: Context
    ): Set<String> {

        return getPreferences(context)
            .getStringSet(
                MUTED_NUMBERS_KEY,
                emptySet()
            )
            ?.toSet()
            ?: emptySet()
    }


    // ============================================================
    // CONVERSATION EN SOURDINE
    // ============================================================

    fun isConversationMuted(
        context: Context,
        phoneNumber: String
    ): Boolean {

        val normalizedNumber =
            normalizePhoneNumber(
                phoneNumber
            )

        return getMutedNumbers(context)
            .contains(
                normalizedNumber
            )
    }


    // ============================================================
    // ACTIVER / DÉSACTIVER LA SOURDINE
    // ============================================================

    fun setConversationMuted(
        context: Context,
        phoneNumber: String,
        muted: Boolean
    ) {

        val mutedNumbers =
            getMutedNumbers(context)
                .toMutableSet()

        val normalizedNumber =
            normalizePhoneNumber(
                phoneNumber
            )


        if (muted) {

            mutedNumbers.add(
                normalizedNumber
            )

        } else {

            mutedNumbers.remove(
                normalizedNumber
            )
        }


        getPreferences(context)
            .edit()
            .putStringSet(
                MUTED_NUMBERS_KEY,
                mutedNumbers
            )
            .apply()
    }


    // ============================================================
    // CRÉER LES CANAUX DE NOTIFICATION
    // ============================================================

    fun createNotificationChannel(
        context: Context
    ) {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.O
        ) {

            return
        }


        val notificationManager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager


        // ========================================================
        // CANAL AVEC VIBRATION
        // ========================================================

        val vibrationChannel =
            NotificationChannel(
                CHANNEL_ID_VIBRATION,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    CHANNEL_DESCRIPTION

                enableVibration(
                    true
                )

                setShowBadge(
                    true
                )
            }


        // ========================================================
        // CANAL SANS VIBRATION
        // ========================================================

        val silentChannel =
            NotificationChannel(
                CHANNEL_ID_SILENT,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {

                description =
                    CHANNEL_DESCRIPTION

                enableVibration(
                    false
                )

                setShowBadge(
                    true
                )
            }


        notificationManager
            .createNotificationChannel(
                vibrationChannel
            )

        notificationManager
            .createNotificationChannel(
                silentChannel
            )
    }


    // ============================================================
    // PERMISSION NOTIFICATION
    // ============================================================

    private fun hasNotificationPermission(
        context: Context
    ): Boolean {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU
        ) {

            return true
        }


        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }


    // ============================================================
    // PENDING INTENT PRINCIPAL
    // ============================================================

    private fun createMainPendingIntent(
        context: Context
    ): PendingIntent {

        val intent =
            Intent(
                context,
                MainActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP

                putExtra(
                    "open_conversation",
                    false
                )
            }


        return PendingIntent.getActivity(
            context,
            SMS_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }


    // ============================================================
    // METTRE À JOUR LA NOTIFICATION + BADGE ANDROID
    // ============================================================

    fun updateUnreadBadge(
        context: Context
    ) {

        if (
            !areNotificationsEnabled(
                context
            )
        ) {

            try {

                NotificationManagerCompat
                    .from(context)
                    .cancel(
                        SMS_NOTIFICATION_ID
                    )

            } catch (
                exception: SecurityException
            ) {

                exception.printStackTrace()
            }

            return
        }


        if (
            !hasNotificationPermission(
                context
            )
        ) {

            return
        }


        createNotificationChannel(
            context
        )


        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val database =
                    AppDatabase
                        .getDatabase(
                            context.applicationContext
                        )


                val messageDao =
                    database
                        .messageDao()


                // =================================================
                // NOMBRE TOTAL DE SMS NON LUS
                // =================================================

                val unreadCount =
                    messageDao
                        .getTotalUnreadCount()
                        .first()


                // =================================================
                // TOUS LES MESSAGES
                // =================================================

                val messages =
                    messageDao
                        .getAllMessages()
                        .first()


                val dernierMessageNonLu =
                    messages.firstOrNull {

                        it.type ==
                                "RECEIVED" &&
                                !it.isRead
                    }


                withContext(
                    Dispatchers.Main
                ) {

                    try {

                        val notificationManager =
                            NotificationManagerCompat
                                .from(context)


                        // =========================================
                        // AUCUN MESSAGE NON LU
                        // =========================================

                        if (
                            unreadCount <= 0
                        ) {

                            notificationManager.cancel(
                                SMS_NOTIFICATION_ID
                            )

                            return@withContext
                        }


                        // =========================================
                        // EXPÉDITEUR
                        // =========================================

                        val contactName =
                            dernierMessageNonLu
                                ?.contactName
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: dernierMessageNonLu
                                    ?.phoneNumber
                                ?: "Nouveau message"


                        // =========================================
                        // CONTENU
                        // =========================================

                        val messageText =
                            dernierMessageNonLu
                                ?.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "$unreadCount message(s) non lu(s)"


                        // =========================================
                        // CANAL
                        // =========================================

                        val channelId =
                            if (
                                isVibrationEnabled(
                                    context
                                )
                            ) {

                                CHANNEL_ID_VIBRATION

                            } else {

                                CHANNEL_ID_SILENT
                            }


                        // =========================================
                        // PENDING INTENT
                        // =========================================

                        val pendingIntent =
                            createMainPendingIntent(
                                context
                            )


                        // =========================================
                        // NOTIFICATION
                        // =========================================

                        val notification =
                            NotificationCompat
                                .Builder(
                                    context,
                                    channelId
                                )
                                .setSmallIcon(
                                    R.drawable.ic_notification_sms
                                )
                                .setContentTitle(
                                    contactName
                                )
                                .setContentText(
                                    messageText
                                )
                                .setStyle(
                                    NotificationCompat
                                        .BigTextStyle()
                                        .bigText(
                                            messageText
                                        )
                                )
                                .setNumber(
                                    unreadCount
                                )
                                .setBadgeIconType(
                                    NotificationCompat
                                        .BADGE_ICON_LARGE
                                )
                                .setPriority(
                                    NotificationCompat
                                        .PRIORITY_HIGH
                                )
                                .setAutoCancel(
                                    false
                                )
                                .setOnlyAlertOnce(
                                    true
                                )
                                .setContentIntent(
                                    pendingIntent
                                )
                                .build()


                        // =========================================
                        // AFFICHER
                        // =========================================

                        notificationManager.notify(
                            SMS_NOTIFICATION_ID,
                            notification
                        )

                    } catch (
                        exception: SecurityException
                    ) {

                        exception.printStackTrace()
                    }
                }

            } catch (
                exception: Exception
            ) {

                exception.printStackTrace()
            }
        }
    }


    // ============================================================
    // NOTIFICATION D'UN NOUVEAU SMS
    // ============================================================

    fun showSmsNotification(
        context: Context,
        phoneNumber: String,
        contactName: String,
        message: String
    ) {

        if (
            !areNotificationsEnabled(
                context
            )
        ) {

            return
        }


        if (
            !hasNotificationPermission(
                context
            )
        ) {

            return
        }


        createNotificationChannel(
            context
        )


        if (
            isConversationMuted(
                context,
                phoneNumber
            )
        ) {

            updateUnreadBadge(
                context
            )

            return
        }


        updateUnreadBadge(
            context
        )
    }
}