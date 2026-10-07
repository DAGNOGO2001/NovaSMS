package com.smsplanner.app.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageEntity
import com.smsplanner.app.notification.NotificationHelper
import com.smsplanner.app.ui.theme.DeleteConfirmationPreference
import com.smsplanner.app.ui.theme.SMSPlannerTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/*
 * ============================================================
 * MODÈLE D'UNE CONVERSATION
 * ============================================================
 */
data class Conversation(
    val id: Long,
    val contact: String,
    val message: String,
    val time: String,
    val phoneNumber: String,
    val unreadCount: Int = 0,
    val status: String = "envoyé",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val timestamp: Long = 0L
)


/*
 * ============================================================
 * ÉCRAN D'ACCUEIL
 * ============================================================
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewMessageClick: () -> Unit = {},
    onConversationClick: (Conversation) -> Unit = {},
    onScheduledMessagesClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {

    val context = LocalContext.current

    /*
     * ========================================================
     * BASE DE DONNÉES
     * ========================================================
     */
    val database =
        remember {
            AppDatabase.getDatabase(context)
        }

    val messageDao =
        database.messageDao()

    /*
     * ========================================================
     * MESSAGES
     * ========================================================
     */
    val messages by messageDao
        .getAllMessages()
        .collectAsStateWithLifecycle<List<MessageEntity>?>(
            initialValue = null
        )

    /*
     * ========================================================
     * COROUTINE
     * ========================================================
     */
    val coroutineScope =
        rememberCoroutineScope()

    /*
     * ========================================================
     * PRÉFÉRENCES
     * ========================================================
     */
    val preferences =
        remember {

            context.getSharedPreferences(
                "smsplanner_conversations",
                Context.MODE_PRIVATE
            )
        }

    /*
     * ========================================================
     * CONVERSATIONS ÉPINGLÉES
     * ========================================================
     */
    var pinnedNumbers by remember {

        mutableStateOf(
            preferences
                .getStringSet(
                    "pinned_numbers",
                    emptySet()
                )
                ?.toSet()
                ?: emptySet()
        )
    }

    /*
     * ========================================================
     * CONVERSATIONS EN SOURDINE
     * ========================================================
     */
    var mutedNumbers by remember {

        mutableStateOf(
            NotificationHelper.getMutedNumbers(
                context
            )
        )
    }

    /*
     * ========================================================
     * SÉLECTION DES CONVERSATIONS
     * ========================================================
     */
    var selectedConversationNumbers by remember {

        mutableStateOf<Set<String>>(
            emptySet()
        )
    }

    val isSelectionMode =
        selectedConversationNumbers.isNotEmpty()

    /*
     * ========================================================
     * CONFIRMATION DE SUPPRESSION
     * ========================================================
     */
    var deleteConfirmationVisible by remember {

        mutableStateOf(false)
    }

    /*
     * ========================================================
     * PERMISSION CONTACTS
     * ========================================================
     */
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

            contract =
                ActivityResultContracts.RequestPermission()

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

    /*
     * ========================================================
     * CONTACTS
     * ========================================================
     */
    var contactNames by remember {

        mutableStateOf<Map<String, String>>(
            emptyMap()
        )
    }

    LaunchedEffect(
        contactsPermissionGranted
    ) {

        if (!contactsPermissionGranted) {

            contactNames =
                emptyMap()

            return@LaunchedEffect
        }

        val contacts =
            withContext(
                Dispatchers.IO
            ) {

                loadContactNames(
                    context
                )
            }

        contactNames =
            contacts
    }

    /*
     * ========================================================
     * CONVERSATIONS
     * ========================================================
     */
    val conversations = remember(
        messages,
        contactNames,
        pinnedNumbers,
        mutedNumbers
    ) {

        messages
            ?.groupBy { message ->

                normalizePhoneNumber(
                    message.phoneNumber
                )
            }
            ?.mapNotNull { (_, contactMessages) ->

                val lastMessage =
                    contactMessages.maxByOrNull {
                        it.timestamp
                    }
                        ?: return@mapNotNull null

                val unreadCount =
                    contactMessages.count { message ->

                        message.type == "RECEIVED" &&
                                !message.isRead
                    }

                val normalizedNumber =
                    normalizePhoneNumber(
                        lastMessage.phoneNumber
                    )

                val contactName =
                    contactNames[
                        normalizedNumber
                    ]

                val displayName =
                    contactName
                        ?: lastMessage.phoneNumber

                Conversation(

                    id =
                        lastMessage.id,

                    contact =
                        displayName,

                    message =
                        lastMessage.message,

                    time =
                        formatTime(
                            lastMessage.timestamp
                        ),

                    phoneNumber =
                        lastMessage.phoneNumber,

                    unreadCount =
                        unreadCount,

                    status =
                        lastMessage.status,

                    isPinned =
                        pinnedNumbers.contains(
                            normalizedNumber
                        ),

                    isMuted =
                        mutedNumbers.contains(
                            normalizedNumber
                        ),

                    timestamp =
                        lastMessage.timestamp
                )
            }
            ?.sortedWith(

                compareByDescending<Conversation> {
                    it.isPinned
                }.thenByDescending {
                    it.timestamp
                }
            )
            ?: emptyList()
    }

    /*
     * ========================================================
     * RECHERCHE
     * ========================================================
     */
    var searchQuery by remember {

        mutableStateOf("")
    }

    val filteredConversations =

        conversations.filter { conversation ->

            if (searchQuery.isBlank()) {

                true

            } else {

                val query =
                    searchQuery
                        .trim()
                        .lowercase()

                conversation.contact
                    .lowercase()
                    .contains(query) ||

                        conversation.phoneNumber
                            .lowercase()
                            .contains(query) ||

                        conversation.message
                            .lowercase()
                            .contains(query)
            }
        }

    /*
     * ========================================================
     * MENU
     * ========================================================
     */
    var menuExpanded by remember {

        mutableStateOf(false)
    }

    /*
     * ========================================================
     * FONCTION DE SUPPRESSION
     * ========================================================
     */
    fun deleteSelectedConversations() {

        val numbersToDelete =
            selectedConversationNumbers

        coroutineScope.launch {

            withContext(
                Dispatchers.IO
            ) {

                val messagesToDelete =
                    messages
                        ?.filter { message ->

                            numbersToDelete.contains(
                                normalizePhoneNumber(
                                    message.phoneNumber
                                )
                            )
                        }
                        ?: emptyList()

                messagesToDelete
                    .forEach { message ->

                        messageDao
                            .deleteMessage(
                                message.id
                            )
                    }
            }

            /*
             * ==================================================
             * MISE À JOUR DES CONVERSATIONS ÉPINGLÉES
             * ==================================================
             */
            val newPinnedNumbers =
                pinnedNumbers
                    .toMutableSet()

            numbersToDelete
                .forEach { number ->

                    newPinnedNumbers
                        .remove(
                            number
                        )
                }

            preferences
                .edit()
                .putStringSet(
                    "pinned_numbers",
                    newPinnedNumbers
                )
                .apply()

            pinnedNumbers =
                newPinnedNumbers.toSet()

            selectedConversationNumbers =
                emptySet()

            /*
             * ==================================================
             * MISE À JOUR DU BADGE DE L'APPLICATION
             *
             * Si les conversations supprimées contenaient
             * des SMS non lus, le nombre du badge doit être
             * recalculé immédiatement.
             * ==================================================
             */
            NotificationHelper.updateUnreadBadge(
                context
            )
        }
    }

    /*
     * ========================================================
     * SCAFFOLD
     * ========================================================
     */
    Scaffold(

        topBar = {

            /*
             * ==================================================
             * MODE SÉLECTION
             * ==================================================
             */
            if (isSelectionMode) {

                TopAppBar(

                    title = {

                        Text(

                            text =
                                "${selectedConversationNumbers.size} sélectionnée(s)",

                            fontWeight =
                                FontWeight.Bold
                        )
                    },

                    navigationIcon = {

                        IconButton(

                            onClick = {

                                selectedConversationNumbers =
                                    emptySet()
                            }

                        ) {

                            Icon(

                                imageVector =
                                    Icons
                                        .AutoMirrored
                                        .Filled
                                        .ArrowBack,

                                contentDescription =
                                    "Quitter la sélection"
                            )
                        }
                    },

                    actions = {

                        /*
                         * ==================================================
                         * SOURDINE / RÉACTIVER
                         * ==================================================
                         */
                        val allSelectedMuted =

                            selectedConversationNumbers
                                .all { number ->

                                    mutedNumbers.contains(
                                        number
                                    )
                                }

                        IconButton(

                            onClick = {

                                val numbersToUpdate =
                                    selectedConversationNumbers

                                if (allSelectedMuted) {

                                    numbersToUpdate
                                        .forEach { number ->

                                            NotificationHelper
                                                .setConversationMuted(
                                                    context =
                                                        context,

                                                    phoneNumber =
                                                        number,

                                                    muted =
                                                        false
                                                )
                                        }

                                } else {

                                    numbersToUpdate
                                        .forEach { number ->

                                            NotificationHelper
                                                .setConversationMuted(
                                                    context =
                                                        context,

                                                    phoneNumber =
                                                        number,

                                                    muted =
                                                        true
                                                )
                                        }
                                }

                                mutedNumbers =
                                    NotificationHelper
                                        .getMutedNumbers(
                                            context
                                        )

                                selectedConversationNumbers =
                                    emptySet()
                            }

                        ) {

                            Text(

                                text =

                                    if (allSelectedMuted) {

                                        "🔔"

                                    } else {

                                        "🔕"
                                    },

                                fontSize =
                                    22.sp
                            )
                        }

                        /*
                         * ==================================================
                         * ÉPINGLER
                         * ==================================================
                         */
                        IconButton(

                            onClick = {

                                val newPinnedNumbers =
                                    pinnedNumbers
                                        .toMutableSet()

                                selectedConversationNumbers
                                    .forEach { number ->

                                        if (
                                            newPinnedNumbers
                                                .contains(
                                                    number
                                                )
                                        ) {

                                            newPinnedNumbers
                                                .remove(
                                                    number
                                                )

                                        } else {

                                            newPinnedNumbers
                                                .add(
                                                    number
                                                )
                                        }
                                    }

                                preferences
                                    .edit()
                                    .putStringSet(
                                        "pinned_numbers",
                                        newPinnedNumbers
                                    )
                                    .apply()

                                pinnedNumbers =
                                    newPinnedNumbers.toSet()

                                selectedConversationNumbers =
                                    emptySet()
                            }

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.PushPin,

                                contentDescription =
                                    "Épingler"
                            )
                        }

                        /*
                         * ==================================================
                         * SUPPRIMER
                         * ==================================================
                         */
                        IconButton(

                            onClick = {

                                if (
                                    DeleteConfirmationPreference
                                        .isEnabled(
                                            context
                                        )
                                ) {

                                    deleteConfirmationVisible =
                                        true

                                } else {

                                    deleteSelectedConversations()
                                }
                            }

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Delete,

                                contentDescription =
                                    "Supprimer"
                            )
                        }
                    }
                )

            } else {

                /*
                 * ==================================================
                 * MODE NORMAL
                 * ==================================================
                 */
                TopAppBar(

                    title = {

                        Text(

                            text =
                                "NovaSMS ",

                            fontWeight =
                                FontWeight.Bold
                        )
                    },

                    actions = {

                        /*
                         * NOUVEAU MESSAGE
                         */
                        IconButton(

                            onClick =
                                onNewMessageClick

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Add,

                                contentDescription =
                                    "Nouveau message"
                            )
                        }

                        /*
                         * MENU
                         */
                        IconButton(

                            onClick = {

                                menuExpanded =
                                    true
                            }

                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.MoreVert,

                                contentDescription =
                                    "Menu"
                            )
                        }

                        DropdownMenu(

                            expanded =
                                menuExpanded,

                            onDismissRequest = {

                                menuExpanded =
                                    false
                            }

                        ) {

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        "Messages programmés"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onScheduledMessagesClick()
                                }
                            )


                            DropdownMenuItem(

                                text = {

                                    Text(
                                        "Paramètres"
                                    )
                                },

                                onClick = {

                                    menuExpanded =
                                        false

                                    onSettingsClick()
                                }
                            )
                        }
                    }
                )
            }
        }

    ) { innerPadding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
        ) {

            /*
             * ==================================================
             * BARRE DE RECHERCHE
             * ==================================================
             */
            if (!isSelectionMode) {

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = {

                        searchQuery =
                            it
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp
                            )
                            .height(48.dp),

                    singleLine =
                        true,

                    shape =
                        RoundedCornerShape(
                            24.dp
                        ),

                    placeholder = {

                        Text(
                            text =
                                "Rechercher une conversation..."
                        )
                    },

                    leadingIcon = {

                        Icon(

                            imageVector =
                                Icons.Default.Search,

                            contentDescription =
                                "Rechercher"
                        )
                    },

                    trailingIcon = {

                        if (
                            searchQuery.isNotEmpty()
                        ) {

                            IconButton(

                                onClick = {

                                    searchQuery =
                                        ""
                                }

                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.Close,

                                    contentDescription =
                                        "Effacer"
                                )
                            }
                        }
                    },

                    colors =
                        TextFieldDefaults.colors(

                            focusedContainerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant,

                            unfocusedContainerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surfaceVariant,

                            focusedIndicatorColor =
                                MaterialTheme
                                    .colorScheme
                                    .outline,

                            unfocusedIndicatorColor =
                                MaterialTheme
                                    .colorScheme
                                    .outline,

                            focusedLeadingIconColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            unfocusedLeadingIconColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            cursorColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,

                            focusedTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface,

                            unfocusedTextColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )
                )
            }

            /*
             * ==================================================
             * CHARGEMENT INITIAL
             * ==================================================
             */
            if (messages == null) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(

                        text =
                            "Chargement des conversations...",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

            } else if (conversations.isEmpty()) {

                /*
                 * ==================================================
                 * AUCUNE CONVERSATION
                 * ==================================================
                 */
                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Text(

                        text =
                            "Aucune conversation",

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
                            "Vos conversations SMS apparaîtront ici.",

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

            } else if (
                filteredConversations.isEmpty()
            ) {

                /*
                 * ==================================================
                 * AUCUN RÉSULTAT
                 * ==================================================
                 */
                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Search,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(48.dp),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(

                        text =
                            "Aucun résultat",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )

                    Text(

                        text =
                            "Aucune conversation ne correspond à \"$searchQuery\".",

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

                /*
                 * ==================================================
                 * LISTE DES CONVERSATIONS
                 * ==================================================
                 */
                LazyColumn(

                    modifier =
                        Modifier.fillMaxSize()
                ) {

                    items(

                        items =
                            filteredConversations,

                        key = { conversation ->

                            normalizePhoneNumber(
                                conversation.phoneNumber
                            )

                        }

                    ) { conversation ->

                        ConversationItem(

                            conversation =
                                conversation,

                            isSelected =
                                selectedConversationNumbers
                                    .contains(
                                        normalizePhoneNumber(
                                            conversation.phoneNumber
                                        )
                                    ),

                            onClick = {

                                val normalizedNumber =
                                    normalizePhoneNumber(
                                        conversation.phoneNumber
                                    )

                                if (isSelectionMode) {

                                    selectedConversationNumbers =

                                        if (
                                            selectedConversationNumbers
                                                .contains(
                                                    normalizedNumber
                                                )
                                        ) {

                                            selectedConversationNumbers -
                                                    normalizedNumber

                                        } else {

                                            selectedConversationNumbers +
                                                    normalizedNumber
                                        }

                                } else {

                                    onConversationClick(
                                        conversation
                                    )
                                }
                            },

                            onLongPress = {

                                val normalizedNumber =
                                    normalizePhoneNumber(
                                        conversation.phoneNumber
                                    )

                                selectedConversationNumbers =

                                    if (
                                        selectedConversationNumbers
                                            .contains(
                                                normalizedNumber
                                            )
                                    ) {

                                        selectedConversationNumbers -
                                                normalizedNumber

                                    } else {

                                        selectedConversationNumbers +
                                                normalizedNumber
                                    }
                            }
                        )
                    }
                }
            }
        }
    }

    /*
     * ========================================================
     * DIALOGUE DE CONFIRMATION
     * ========================================================
     */
    if (deleteConfirmationVisible) {

        AlertDialog(

            onDismissRequest = {

                deleteConfirmationVisible =
                    false
            },

            title = {

                Text(
                    text =
                        "Supprimer les conversations ?"
                )
            },

            text = {

                Text(

                    text =

                        if (
                            selectedConversationNumbers.size == 1
                        ) {

                            "Cette conversation sera supprimée définitivement."

                        } else {

                            "Les ${selectedConversationNumbers.size} conversations sélectionnées seront supprimées définitivement."
                        }
                )
            },

            dismissButton = {

                TextButton(

                    onClick = {

                        deleteConfirmationVisible =
                            false
                    }

                ) {

                    Text(
                        text =
                            "Annuler"
                    )
                }
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        deleteConfirmationVisible =
                            false

                        deleteSelectedConversations()
                    }

                ) {

                    Text(

                        text =
                            "Supprimer",

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }
            }
        )
    }
}


/*
 * ============================================================
 * ÉLÉMENT D'UNE CONVERSATION
 * ============================================================
 */
@Composable
fun ConversationItem(

    conversation: Conversation,

    isSelected: Boolean = false,

    onClick: () -> Unit,

    onLongPress: () -> Unit = {}

) {

    val isProgrammed =
        conversation.status.equals(
            "message programmé",
            ignoreCase = true
        )

    val isFailed =
        conversation.status.equals(
            "échec",
            ignoreCase = true
        )

    val statusColor =
        MaterialTheme
            .colorScheme
            .primary

    val backgroundColor =

        if (isSelected) {

            MaterialTheme
                .colorScheme
                .secondaryContainer

        } else {

            Color.Transparent
        }

    Column(

        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        backgroundColor
                )
                .pointerInput(
                    conversation.phoneNumber
                ) {

                    detectTapGestures(

                        onLongPress = {

                            onLongPress()
                        },

                        onTap = {

                            onClick()
                        }
                    )
                }
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * ==================================================
             * AVATAR
             * ==================================================
             */
            Box(

                modifier =
                    Modifier.size(48.dp)
            ) {

                Box(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,

                                shape =
                                    CircleShape
                            ),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(

                        text =
                            conversation.contact
                                .take(1)
                                .uppercase(),

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                /*
                 * BADGE NON LUS
                 */
                if (
                    conversation.unreadCount > 0
                ) {

                    Box(

                        modifier =
                            Modifier
                                .size(18.dp)
                                .background(

                                    color =
                                        Color(
                                            0xFFE53935
                                        ),

                                    shape =
                                        CircleShape
                                )
                                .align(
                                    Alignment.TopStart
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(

                            text =

                                if (
                                    conversation.unreadCount > 99
                                ) {

                                    "99+"

                                } else {

                                    conversation
                                        .unreadCount
                                        .toString()
                                },

                            color =
                                Color.White,

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall,

                            fontSize =
                                10.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                }

                /*
                 * ÉPINGLÉ
                 */
                if (conversation.isPinned) {

                    Icon(

                        imageVector =
                            Icons.Default.PushPin,

                        contentDescription =
                            "Conversation épinglée",

                        modifier =
                            Modifier
                                .size(16.dp)
                                .align(
                                    Alignment.TopEnd
                                ),

                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                /*
                 * SOURDINE
                 */
                if (conversation.isMuted) {

                    Text(

                        text =
                            "🔕",

                        fontSize =
                            13.sp,

                        modifier =
                            Modifier
                                .align(
                                    Alignment.BottomEnd
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(16.dp)
            )

            /*
             * ==================================================
             * INFORMATIONS
             * ==================================================
             */
            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        conversation.contact,

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurface
                )

                Spacer(
                    modifier =
                        Modifier.height(2.dp)
                )

                /*
                 * MESSAGE PROGRAMMÉ
                 */
                if (isProgrammed) {

                    Text(

                        text =
                            "🕐 Programmé",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            statusColor,

                        maxLines =
                            1
                    )

                    Text(

                        text =
                            conversation.message,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        maxLines =
                            1,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                } else if (isFailed) {

                    /*
                     * MESSAGE EN ÉCHEC
                     */
                    Text(

                        text =
                            "❌ Échec de l'envoi",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        maxLines =
                            1
                    )

                    Text(

                        text =
                            conversation.message,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        maxLines =
                            1,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )

                } else {

                    /*
                     * MESSAGE NORMAL
                     */
                    Text(

                        text =
                            conversation.message,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        maxLines =
                            1,

                        fontWeight =

                            if (
                                conversation.unreadCount > 0
                            ) {

                                FontWeight.Bold

                            } else {

                                FontWeight.Normal
                            },

                        color =

                            if (
                                conversation.unreadCount > 0
                            ) {

                                MaterialTheme
                                    .colorScheme
                                    .onSurface

                            } else {

                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.size(8.dp)
            )

            /*
             * ==================================================
             * HEURE
             * ==================================================
             */
            Column(

                horizontalAlignment =
                    Alignment.End
            ) {

                if (isProgrammed) {

                    Text(

                        text =
                            conversation.time,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            statusColor,

                        fontWeight =
                            FontWeight.Bold
                    )

                } else {

                    Text(

                        text =
                            conversation.time,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        fontWeight =
                            FontWeight.Normal
                    )
                }
            }
        }

        HorizontalDivider(

            color =
                MaterialTheme
                    .colorScheme
                    .outlineVariant
                    .copy(
                        alpha = 0.5f
                    )
        )
    }
}


/*
 * ============================================================
 * CHARGER TOUS LES CONTACTS
 * ============================================================
 */
private fun loadContactNames(
    context: Context
): Map<String, String> {

    val result =
        mutableMapOf<String, String>()

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

                return emptyMap()
            }

            while (
                cursor.moveToNext()
            ) {

                val name =
                    cursor.getString(
                        nameIndex
                    )?.trim()

                val number =
                    cursor.getString(
                        numberIndex
                    )?.trim()

                if (
                    !name.isNullOrBlank() &&
                    !number.isNullOrBlank()
                ) {

                    val normalizedNumber =
                        normalizePhoneNumber(
                            number
                        )

                    if (
                        normalizedNumber.isNotBlank()
                    ) {

                        result[
                            normalizedNumber
                        ] = name
                    }
                }
            }
        }

        result

    } catch (
        exception: Exception
    ) {

        exception.printStackTrace()

        emptyMap()
    }
}


/*
 * ============================================================
 * NORMALISER UN NUMÉRO
 * ============================================================
 */
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

    if (
        number.startsWith("+225")
    ) {

        number =
            number.removePrefix(
                "+225"
            )

    } else if (

        number.startsWith("225") &&
        number.length > 10

    ) {

        number =
            number.removePrefix(
                "225"
            )
    }

    return number.filter {
        it.isDigit()
    }
}


/*
 * ============================================================
 * FORMATAGE DE L'HEURE
 * ============================================================
 */
private fun formatTime(
    timestamp: Long
): String {

    val diff =
        System.currentTimeMillis() -
                timestamp

    val minutes =
        diff / (1000 * 60)

    val hours =
        minutes / 60

    return when {

        minutes < 1 ->

            "À l'instant"

        minutes < 60 ->

            "Il y a $minutes min"

        hours < 24 -> {

            val formatter =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            formatter.format(
                Date(timestamp)
            )
        }

        else -> {

            val formatter =
                SimpleDateFormat(
                    "dd/MM",
                    Locale.getDefault()
                )

            formatter.format(
                Date(timestamp)
            )
        }
    }
}


/*
 * ============================================================
 * APERÇU
 * ============================================================
 */
@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HomeScreenPreview() {

    SMSPlannerTheme {

        HomeScreen()
    }
}
