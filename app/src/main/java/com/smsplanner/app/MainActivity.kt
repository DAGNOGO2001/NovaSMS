package com.smsplanner.app

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

import androidx.core.content.ContextCompat

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageEntity
import com.smsplanner.app.data.ProgrammationMessageEntity

import com.smsplanner.app.notification.NotificationHelper

import com.smsplanner.app.receiver.SmsScheduleReceiver

import com.smsplanner.app.screens.AboutScreen
import com.smsplanner.app.screens.ConversationScreen
import com.smsplanner.app.screens.HomeScreen
import com.smsplanner.app.screens.MessagesProgrammesScreen
import com.smsplanner.app.screens.NewMessageScreen
import com.smsplanner.app.screens.ProgrammerMessageScreen
import com.smsplanner.app.screens.SettingsScreen

import com.smsplanner.app.sync.SmsSentSync

import com.smsplanner.app.ui.theme.SMSPlannerTheme
import com.smsplanner.app.ui.theme.ThemePreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.util.Calendar


class MainActivity : ComponentActivity() {

    // ============================================================
    // SYNCHRONISATION DES SMS ENVOYÉS
    // ============================================================

    private lateinit var smsSentSync: SmsSentSync

    private var synchronisationDemarree = false


    // ============================================================
    // DEMANDE DES PERMISSIONS SMS
    // ============================================================

    private val smsPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val sendSmsGranted =
                permissions[
                    Manifest.permission.SEND_SMS
                ] == true ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.SEND_SMS
                        ) == PackageManager.PERMISSION_GRANTED


            val receiveSmsGranted =
                permissions[
                    Manifest.permission.RECEIVE_SMS
                ] == true ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.RECEIVE_SMS
                        ) == PackageManager.PERMISSION_GRANTED


            val readSmsGranted =
                permissions[
                    Manifest.permission.READ_SMS
                ] == true ||
                        ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.READ_SMS
                        ) == PackageManager.PERMISSION_GRANTED


            if (
                sendSmsGranted &&
                receiveSmsGranted &&
                readSmsGranted
            ) {

                getPreferences(
                    MODE_PRIVATE
                )
                    .edit()
                    .putBoolean(
                        "sms_permission_granted",
                        true
                    )
                    .apply()


                demarrerSynchronisationEnArrierePlan()

            } else {

                getPreferences(
                    MODE_PRIVATE
                )
                    .edit()
                    .putBoolean(
                        "sms_permission_granted",
                        false
                    )
                    .apply()
            }
        }


    // ============================================================
    // DEMANDE DE PERMISSION NOTIFICATION
    // ============================================================

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                NotificationHelper
                    .createNotificationChannel(
                        applicationContext
                    )

                NotificationHelper
                    .updateUnreadBadge(
                        applicationContext
                    )
            }
        }


    // ============================================================
    // ON CREATE
    // ============================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        enableEdgeToEdge()


        // ========================================================
        // CRÉER LE SYNCHRONISEUR
        // ========================================================

        smsSentSync =
            SmsSentSync(
                applicationContext
            )


        // ========================================================
        // CRÉER LES CANAUX DE NOTIFICATION
        // ========================================================

        NotificationHelper
            .createNotificationChannel(
                applicationContext
            )


        // ========================================================
        // AFFICHER L'INTERFACE
        // ========================================================

        setContent {

            SMSPlannerApp()
        }


        // ========================================================
        // VÉRIFIER LES PERMISSIONS
        // ========================================================

        window.decorView.post {

            requestSmsPermissionsIfNeeded()

            requestNotificationPermissionIfNeeded()
        }
    }


    // ============================================================
    // ON RESUME
    // ============================================================

    override fun onResume() {

        super.onResume()


        if (
            ::smsSentSync.isInitialized
        ) {

            val readSmsGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED


            if (
                readSmsGranted &&
                !synchronisationDemarree
            ) {

                demarrerSynchronisationEnArrierePlan()
            }
        }


        // ========================================================
        // ACTUALISER LE BADGE SMS
        // ========================================================

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.TIRAMISU ||

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            NotificationHelper
                .updateUnreadBadge(
                    applicationContext
                )
        }
    }


    // ============================================================
    // DEMANDER POST_NOTIFICATIONS
    // ============================================================

    private fun requestNotificationPermissionIfNeeded() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED


            if (
                !permissionGranted
            ) {

                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }


    // ============================================================
    // DÉMARRER LA SYNCHRONISATION
    // ============================================================

    private fun demarrerSynchronisationEnArrierePlan() {

        if (
            synchronisationDemarree
        ) {

            return
        }


        val readSmsGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED


        if (
            !readSmsGranted
        ) {

            return
        }


        synchronisationDemarree =
            true


        smsSentSync.start()
    }


    // ============================================================
    // ON DESTROY
    // ============================================================

    override fun onDestroy() {

        if (
            ::smsSentSync.isInitialized
        ) {

            smsSentSync.stop()
        }


        super.onDestroy()
    }


    // ============================================================
    // VÉRIFIER ET DEMANDER LES PERMISSIONS SMS
    // ============================================================

    private fun requestSmsPermissionsIfNeeded() {

        val permissionsManquantes =
            mutableListOf<String>()


        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissionsManquantes.add(
                Manifest.permission.SEND_SMS
            )
        }


        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECEIVE_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissionsManquantes.add(
                Manifest.permission.RECEIVE_SMS
            )
        }


        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            permissionsManquantes.add(
                Manifest.permission.READ_SMS
            )
        }


        if (
            permissionsManquantes.isEmpty()
        ) {

            getPreferences(
                MODE_PRIVATE
            )
                .edit()
                .putBoolean(
                    "sms_permission_granted",
                    true
                )
                .apply()


            window.decorView.post {

                demarrerSynchronisationEnArrierePlan()
            }


            return
        }


        smsPermissionLauncher.launch(
            permissionsManquantes.toTypedArray()
        )
    }
}


// =================================================================
// APPLICATION SMS PLANNER
// =================================================================

@Composable
fun SMSPlannerApp() {

    val context =
        LocalContext.current


    var themeMode by remember {

        mutableStateOf(
            ThemePreference.getTheme(
                context
            )
        )
    }


    val darkTheme =
        when (themeMode) {

            ThemePreference.DARK ->
                true

            ThemePreference.LIGHT ->
                false

            else ->
                isSystemInDarkTheme()
        }


    SMSPlannerTheme(

        darkTheme =
            darkTheme

    ) {

        SMSPlanner(

            onThemeChanged = {

                    newTheme ->

                themeMode =
                    newTheme
            }
        )
    }
}


// =================================================================
// NAVIGATION SMS PLANNER
// =================================================================

@Composable
fun SMSPlanner(
    onThemeChanged: (String) -> Unit = {}
) {

    val context =
        LocalContext.current


    val coroutineScope =
        rememberCoroutineScope()


    var currentScreen by remember {

        mutableStateOf(
            "home"
        )
    }


    var selectedPhoneNumber by remember {

        mutableStateOf(
            ""
        )
    }


    var phoneNumber by remember {

        mutableStateOf(
            ""
        )
    }


    var message by remember {

        mutableStateOf(
            ""
        )
    }


    when (
        currentScreen
    ) {

        // ========================================================
        // ACCUEIL
        // ========================================================

        "home" -> {

            HomeScreen(

                onNewMessageClick = {

                    currentScreen =
                        "new_message"
                },


                onConversationClick = {

                        conversation ->

                    selectedPhoneNumber =
                        conversation.phoneNumber

                    currentScreen =
                        "conversation"
                },


                onScheduledMessagesClick = {

                    currentScreen =
                        "messages_programmes"
                },


                onHistoryClick = {},


                onSettingsClick = {

                    currentScreen =
                        "settings"
                }
            )
        }


        // ========================================================
        // CONVERSATION
        // ========================================================

        "conversation" -> {

            ConversationScreen(

                phoneNumber =
                    selectedPhoneNumber,


                onBackClick = {

                    currentScreen =
                        "home"
                },


                onProgramMessageClick = {

                        number,
                        text ->

                    phoneNumber =
                        number

                    message =
                        text

                    currentScreen =
                        "programmer_message"
                }
            )
        }


        // ========================================================
        // NOUVEAU MESSAGE
        // ========================================================

        "new_message" -> {

            NewMessageScreen(

                onBackClick = {

                    currentScreen =
                        "home"
                },


                onScheduleClick = {

                        number,
                        text ->

                    phoneNumber =
                        number

                    message =
                        text

                    currentScreen =
                        "programmer_message"
                },


                onSendClick = {

                    coroutineScope.launch {

                        val lastSentMessage =

                            withContext(
                                Dispatchers.IO
                            ) {

                                val database =
                                    AppDatabase
                                        .getDatabase(
                                            context
                                        )


                                val messages =
                                    database
                                        .messageDao()
                                        .getAllMessages()
                                        .first()


                                messages
                                    .lastOrNull {

                                        it.type ==
                                                "SENT"
                                    }
                            }


                        if (
                            lastSentMessage != null
                        ) {

                            selectedPhoneNumber =
                                lastSentMessage
                                    .phoneNumber


                            currentScreen =
                                "conversation"
                        }
                    }
                }
            )
        }


        // ========================================================
        // PROGRAMMER MESSAGE
        // ========================================================

        "programmer_message" -> {

            ProgrammerMessageScreen(

                phoneNumber =
                    phoneNumber,


                message =
                    message,


                onBackClick = {

                    if (
                        selectedPhoneNumber
                            .isNotBlank()
                    ) {

                        currentScreen =
                            "conversation"

                    } else {

                        currentScreen =
                            "new_message"
                    }
                },


                onConfirmSchedule = {

                        number,
                        text,
                        date,
                        hour,
                        minute ->

                    coroutineScope.launch {

                        val scheduledTime =

                            withContext(
                                Dispatchers.Default
                            ) {

                                val calendar =
                                    Calendar.getInstance()


                                calendar.timeInMillis =
                                    date


                                calendar.set(
                                    Calendar.HOUR_OF_DAY,
                                    hour
                                )


                                calendar.set(
                                    Calendar.MINUTE,
                                    minute
                                )


                                calendar.set(
                                    Calendar.SECOND,
                                    0
                                )


                                calendar.set(
                                    Calendar.MILLISECOND,
                                    0
                                )


                                calendar.timeInMillis
                            }


                        val result =

                            withContext(
                                Dispatchers.IO
                            ) {

                                val database =
                                    AppDatabase
                                        .getDatabase(
                                            context
                                        )


                                val messageDao =
                                    database
                                        .messageDao()


                                val duplicate =
                                    messageDao
                                        .findScheduledDuplicate(

                                            phoneNumber =
                                                number,

                                            message =
                                                text,

                                            scheduledTime =
                                                scheduledTime
                                        )


                                if (
                                    duplicate != null
                                ) {

                                    return@withContext Pair(
                                        duplicate.id,
                                        false
                                    )
                                }


                                val messageEntity =
                                    MessageEntity(

                                        phoneNumber =
                                            number,

                                        message =
                                            text,

                                        timestamp =
                                            scheduledTime,

                                        type =
                                            "SENT",

                                        isRead =
                                            true,

                                        status =
                                            "message programmé"
                                    )


                                val messageId =
                                    messageDao
                                        .insertMessage(
                                            messageEntity
                                        )


                                val programmation =
                                    ProgrammationMessageEntity(

                                        messageId =
                                            messageId,

                                        phoneNumber =
                                            number,

                                        message =
                                            text,

                                        scheduledTime =
                                            scheduledTime,

                                        status =
                                            "message programmé"
                                    )


                                val programmationId =
                                    database
                                        .programmationMessageDao()
                                        .insererMessageProgramme(
                                            programmation
                                        )


                                Pair(
                                    programmationId,
                                    true
                                )
                            }


                        if (
                            result.second
                        ) {

                            programmerAlarme(

                                context =
                                    context,

                                programmationId =
                                    result.first,

                                scheduledTime =
                                    scheduledTime
                            )
                        }


                        selectedPhoneNumber =
                            number

                        phoneNumber =
                            number

                        message =
                            text

                        currentScreen =
                            "conversation"
                    }
                }
            )
        }


        // ========================================================
        // MESSAGES PROGRAMMÉS
        // ========================================================

        "messages_programmes" -> {

            MessagesProgrammesScreen(

                onBackClick = {

                    currentScreen =
                        "home"
                },


                onDeleteClick = {

                        messageProgramme ->

                    coroutineScope.launch {

                        annulerAlarme(

                            context =
                                context,

                            messageId =
                                messageProgramme.id
                        )


                        withContext(
                            Dispatchers.IO
                        ) {

                            val database =
                                AppDatabase
                                    .getDatabase(
                                        context
                                    )


                            database
                                .programmationMessageDao()
                                .supprimerParId(
                                    messageProgramme.id
                                )


                            database
                                .messageDao()
                                .deleteMessage(
                                    messageProgramme.messageId
                                )
                        }
                    }
                }
            )
        }


        // ========================================================
        // PARAMÈTRES
        // ========================================================

        "settings" -> {

            SettingsScreen(

                onBackClick = {

                    currentScreen =
                        "home"
                },


                onThemeChanged = {

                        newTheme ->

                    onThemeChanged(
                        newTheme
                    )
                },


                onAboutClick = {

                    currentScreen =
                        "about"
                }
            )
        }


        // ========================================================
        // À PROPOS
        // ========================================================

        "about" -> {

            AboutScreen(

                onBackClick = {

                    currentScreen =
                        "settings"
                }
            )
        }
    }
}


// =================================================================
// PROGRAMMER L'ALARME
// =================================================================

private fun programmerAlarme(
    context: Context,
    programmationId: Long,
    scheduledTime: Long
) {

    val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager


    val intent =
        Intent(
            context,
            SmsScheduleReceiver::class.java
        ).apply {

            putExtra(
                SmsScheduleReceiver
                    .EXTRA_MESSAGE_ID,
                programmationId
            )
        }


    val pendingIntent =
        PendingIntent.getBroadcast(

            context,

            programmationId.toInt(),

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )


    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.S
    ) {

        if (
            alarmManager
                .canScheduleExactAlarms()
        ) {

            alarmManager
                .setExactAndAllowWhileIdle(

                    AlarmManager.RTC_WAKEUP,

                    scheduledTime,

                    pendingIntent
                )

        } else {

            alarmManager
                .setAndAllowWhileIdle(

                    AlarmManager.RTC_WAKEUP,

                    scheduledTime,

                    pendingIntent
                )
        }

    } else {

        alarmManager
            .setExactAndAllowWhileIdle(

                AlarmManager.RTC_WAKEUP,

                scheduledTime,

                pendingIntent
            )
    }
}


// =================================================================
// ANNULER UNE ALARME
// =================================================================

private fun annulerAlarme(
    context: Context,
    messageId: Long
) {

    val alarmManager =
        context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager


    val intent =
        Intent(
            context,
            SmsScheduleReceiver::class.java
        )


    val pendingIntent =
        PendingIntent.getBroadcast(

            context,

            messageId.toInt(),

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )


    alarmManager.cancel(
        pendingIntent
    )


    pendingIntent.cancel()
}


// =================================================================
// PREVIEW
// =================================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun SMSPlannerAppPreview() {

    SMSPlannerTheme {

        SMSPlanner()
    }
}