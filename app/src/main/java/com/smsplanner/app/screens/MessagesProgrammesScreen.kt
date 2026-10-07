
package com.smsplanner.app.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.ProgrammationMessageEntity
import com.smsplanner.app.ui.theme.SMSPlannerTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesProgrammesScreen(
    onBackClick: () -> Unit = {},
    onDeleteClick: (ProgrammationMessageEntity) -> Unit = {}
) {

    val context = LocalContext.current

    // ============================================================
    // PERMISSION CONTACTS
    // ============================================================

    var contactsPermissionGranted by remember {

        mutableStateOf(

            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val contactsPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            contactsPermissionGranted =
                isGranted
        }

    LaunchedEffect(Unit) {

        if (!contactsPermissionGranted) {

            contactsPermissionLauncher.launch(
                Manifest.permission.READ_CONTACTS
            )
        }
    }

    // ============================================================
    // BASE DE DONNÉES
    // ============================================================

    val database = remember {

        AppDatabase.getDatabase(
            context
        )
    }

    // ============================================================
    // MESSAGES ENCORE PROGRAMMÉS
    //
    // Le DAO filtre uniquement :
    // status = "message programmé"
    //
    // Et trie :
    // scheduledTime ASC
    //
    // Donc le prochain message à envoyer
    // apparaît toujours en premier.
    // ============================================================

    val scheduledMessages by database
        .programmationMessageDao()
        .obtenirMessagesProgrammes()
        .collectAsState(
            initial = emptyList()
        )

    // ============================================================
    // MESSAGE À SUPPRIMER
    // ============================================================

    var messageToDelete by remember {

        mutableStateOf<ProgrammationMessageEntity?>(null)
    }

    // ============================================================
    // DIALOGUE DE CONFIRMATION
    // ============================================================

    if (messageToDelete != null) {

        AlertDialog(

            onDismissRequest = {

                messageToDelete = null
            },

            title = {

                Text(
                    text = "Annuler le message ?",
                    fontWeight = FontWeight.Bold
                )
            },

            text = {

                Text(
                    text =
                        "Ce message sera retiré de la liste des messages programmés et de la conversation. L'envoi programmé sera également annulé."
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        messageToDelete?.let {

                            onDeleteClick(it)
                        }

                        messageToDelete = null
                    }
                ) {

                    Text(
                        text = "Confirmer",
                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        messageToDelete = null
                    }
                ) {

                    Text(
                        text = "Annuler"
                    )
                }
            }
        )
    }

    // ============================================================
    // INTERFACE
    // ============================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Messages programmés",
                        fontWeight = FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Retour"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        // ========================================================
        // AUCUN MESSAGE
        // ========================================================

        if (scheduledMessages.isEmpty()) {

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),

                verticalArrangement =
                    Arrangement.Center
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Schedule,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(

                    text =
                        "Aucun message programmé",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                Text(

                    text =
                        "Les messages que vous programmerez apparaîtront ici.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

            }

        } else {

            // ====================================================
            // LISTE
            // ====================================================

            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                item {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )
                }

                items(

                    items =
                        scheduledMessages,

                    key = {
                        it.id
                    }

                ) { scheduledMessage ->

                    ScheduledMessageCard(

                        scheduledMessage =
                            scheduledMessage,

                        contactsPermissionGranted =
                            contactsPermissionGranted,

                        onDeleteClick = {

                            messageToDelete =
                                scheduledMessage
                        }
                    )
                }

                item {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )
                }
            }
        }
    }
}


// =================================================================
// CARTE MESSAGE PROGRAMMÉ
// =================================================================

@Composable
fun ScheduledMessageCard(

    scheduledMessage:
    ProgrammationMessageEntity,

    contactsPermissionGranted:
    Boolean,

    onDeleteClick:
        () -> Unit = {}

) {

    val context =
        LocalContext.current

    // ============================================================
    // NOM DU CONTACT
    // ============================================================

    val contactName = remember(

        scheduledMessage.phoneNumber,

        contactsPermissionGranted

    ) {

        if (contactsPermissionGranted) {

            getContactNameForScheduledMessage(

                context =
                    context,

                phoneNumber =
                    scheduledMessage.phoneNumber

            )

        } else {

            null
        }
    }

    // ============================================================
    // DATE
    // ============================================================

    val dateFormat =
        remember {

            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )
        }

    // ============================================================
    // HEURE
    // ============================================================

    val timeFormat =
        remember {

            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            )
        }

    val date =
        dateFormat.format(

            Date(
                scheduledMessage.scheduledTime
            )
        )

    val time =
        timeFormat.format(

            Date(
                scheduledMessage.scheduledTime
            )
        )

    // ============================================================
    // CARTE
    // ============================================================

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            // ====================================================
            // CONTACT
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

                    // =================================================
                    // NOM
                    // =================================================

                    Text(

                        text =
                            contactName
                                ?: scheduledMessage.phoneNumber,

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )

                    // =================================================
                    // NUMÉRO
                    // =================================================

                    if (contactName != null) {

                        Spacer(
                            modifier =
                                Modifier.height(2.dp)
                        )

                        Text(

                            text =
                                scheduledMessage.phoneNumber,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    // =================================================
                    // MESSAGE
                    // =================================================

                    Text(

                        text =
                            scheduledMessage.message,

                        style =
                            MaterialTheme
                                .typography
                                .bodyLarge
                    )
                }

                // =================================================
                // SUPPRIMER
                // =================================================

                IconButton(

                    onClick =
                        onDeleteClick
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Delete,

                        contentDescription =
                            "Annuler le message",

                        tint =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            // ====================================================
            // DATE + HEURE
            // ====================================================

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(

                    text =
                        "📅 $date",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(

                    text =
                        "🕐 $time",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            // ====================================================
            // STATUT
            // ====================================================

            Text(

                text = when (

                    scheduledMessage.status

                ) {

                    "message programmé" ->
                        "Message programmé"

                    "envoi" ->
                        "Envoi en cours..."

                    "envoyé" ->
                        "Message envoyé"

                    "échec" ->
                        "Échec de l'envoi"

                    else ->
                        scheduledMessage.status
                },

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                fontWeight =
                    FontWeight.Bold,

                color = when (

                    scheduledMessage.status

                ) {

                    "message programmé" ->
                        MaterialTheme
                            .colorScheme
                            .primary

                    "envoi" ->
                        MaterialTheme
                            .colorScheme
                            .tertiary

                    "envoyé" ->
                        MaterialTheme
                            .colorScheme
                            .primary

                    "échec" ->
                        MaterialTheme
                            .colorScheme
                            .error

                    else ->
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                }
            )
        }
    }
}


// =================================================================
// RECHERCHER LE NOM DU CONTACT
// =================================================================

private fun getContactNameForScheduledMessage(

    context:
    Context,

    phoneNumber:
    String

): String? {

    return try {

        val contactsUri =
            ContactsContract
                .CommonDataKinds
                .Phone
                .CONTENT_URI

        val projection =
            arrayOf(

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .DISPLAY_NAME,

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .NUMBER
            )

        context.contentResolver.query(

            contactsUri,

            projection,

            null,

            null,

            null

        )?.use { cursor ->

            val nameIndex =
                cursor.getColumnIndex(

                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .DISPLAY_NAME
                )

            val numberIndex =
                cursor.getColumnIndex(

                    ContactsContract
                        .CommonDataKinds
                        .Phone
                        .NUMBER
                )

            if (
                nameIndex < 0 ||
                numberIndex < 0
            ) {

                return null
            }

            val normalizedSmsNumber =
                normalizeScheduledPhoneNumber(
                    phoneNumber
                )

            while (
                cursor.moveToNext()
            ) {

                val contactNumber =
                    cursor.getString(
                        numberIndex
                    ) ?: continue

                val normalizedContactNumber =
                    normalizeScheduledPhoneNumber(
                        contactNumber
                    )

                if (
                    normalizedSmsNumber ==
                    normalizedContactNumber
                ) {

                    val name =
                        cursor.getString(
                            nameIndex
                        )

                    if (
                        !name.isNullOrBlank()
                    ) {

                        return name
                    }
                }
            }

            null
        }

    } catch (exception: Exception) {

        exception.printStackTrace()

        null
    }
}


// =================================================================
// NORMALISER LE NUMÉRO
// =================================================================

private fun normalizeScheduledPhoneNumber(
    phoneNumber:
    String
): String {

    var number =

        phoneNumber
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")

    if (
        number.startsWith("+225")
    ) {

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


// =================================================================
// PREVIEW
// =================================================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MessagesProgrammesScreenPreview() {

    SMSPlannerTheme {

        MessagesProgrammesScreen()
    }
}
