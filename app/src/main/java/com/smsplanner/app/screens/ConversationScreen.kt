package com.smsplanner.app.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageDao
import com.smsplanner.app.data.MessageEntity
import com.smsplanner.app.notification.NotificationHelper
import com.smsplanner.app.ui.theme.TextSizePreference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

import java.util.Calendar
import java.util.regex.Pattern
import kotlin.math.abs


private val sendMutex = Mutex()

private const val SETTINGS_PREFERENCES = "smsplanner_settings"
private const val DELETE_CONFIRMATION_KEY = "delete_confirmation_enabled"

private val SendGreen = Color(0xFF22C55E)

private const val ANNOTATION_URL = "URL"
private const val ANNOTATION_PHONE = "PHONE"
private const val ANNOTATION_EMAIL = "EMAIL"


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    phoneNumber: String,
    onBackClick: () -> Unit,
    onProgramMessageClick: (String, String) -> Unit
) {

    val context = LocalContext.current

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val messageDao = database.messageDao()

    val messages by messageDao
        .getMessagesForNumber(phoneNumber)
        .collectAsState(initial = emptyList())

    val listState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()

    var text by remember {
        mutableStateOf("")
    }

    var isSending by remember {
        mutableStateOf(false)
    }

    var contactName by remember {
        mutableStateOf<String?>(null)
    }

    var messageTextSize by remember {
        mutableFloatStateOf(
            TextSizePreference.getTextSize(context)
        )
    }

    var isSearchMode by remember {
        mutableStateOf(false)
    }

    var searchText by remember {
        mutableStateOf("")
    }

    val filteredMessages = remember(
        messages,
        searchText
    ) {

        if (searchText.trim().isBlank()) {
            messages
        } else {

            val query = searchText
                .trim()
                .lowercase()

            messages.filter {
                it.message
                    .lowercase()
                    .contains(query)
            }
        }
    }

    var selectedMessageIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    val isSelectionMode =
        selectedMessageIds.isNotEmpty()

    val settingsPreferences = remember {
        context.getSharedPreferences(
            SETTINGS_PREFERENCES,
            Context.MODE_PRIVATE
        )
    }

    var deleteConfirmationEnabled by remember {
        mutableStateOf(
            settingsPreferences.getBoolean(
                DELETE_CONFIRMATION_KEY,
                true
            )
        )
    }

    var deleteConfirmationDialogVisible by remember {
        mutableStateOf(false)
    }


    /*
     * ============================================================
     * SIM
     * ============================================================
     */

    var showSimDialog by remember {
        mutableStateOf(false)
    }

    var simList by remember {
        mutableStateOf<List<SubscriptionInfo>>(emptyList())
    }

    var selectedSim by remember {
        mutableStateOf<SubscriptionInfo?>(null)
    }

    fun loadSimCards() {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {

            val subscriptionManager =
                context.getSystemService(
                    SubscriptionManager::class.java
                )

            val subscriptions =
                subscriptionManager
                    ?.activeSubscriptionInfoList
                    ?: emptyList()

            simList = subscriptions

            if (
                selectedSim == null &&
                subscriptions.isNotEmpty()
            ) {
                selectedSim = subscriptions.first()
            }

        } catch (
            e: SecurityException
        ) {

            simList = emptyList()
        }
    }

    val simPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadSimCards()
            }
        }

    LaunchedEffect(Unit) {

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            loadSimCards()

        } else {

            simPermissionLauncher.launch(
                Manifest.permission.READ_PHONE_STATE
            )
        }
    }


    /*
     * ============================================================
     * CONTACT
     * ============================================================
     */

    LaunchedEffect(phoneNumber) {

        contactName = withContext(Dispatchers.IO) {
            getContactName(
                context,
                phoneNumber
            )
        }
    }


    /*
     * ============================================================
     * MARQUER LES MESSAGES COMME LUS
     * ============================================================
     */

    LaunchedEffect(phoneNumber) {

        withContext(Dispatchers.IO) {

            messageDao.markMessagesAsRead(
                phoneNumber
            )
        }

        NotificationHelper.updateUnreadBadge(
            context
        )
    }


    /*
     * ============================================================
     * SCROLL AUTOMATIQUE
     * ============================================================
     */

    LaunchedEffect(messages.size) {

        if (
            messages.isNotEmpty() &&
            searchText.isBlank()
        ) {

            listState.animateScrollToItem(
                messages.lastIndex
            )
        }
    }


    /*
     * ============================================================
     * RECHERCHE
     * ============================================================
     */

    LaunchedEffect(searchText) {

        if (
            searchText.isNotBlank() &&
            filteredMessages.isNotEmpty()
        ) {

            listState.scrollToItem(0)
        }
    }


    /*
     * ============================================================
     * SUPPRESSION
     * ============================================================
     */

    fun deleteSelectedMessages() {

        val ids = selectedMessageIds

        if (ids.isEmpty()) {
            return
        }

        coroutineScope.launch {

            withContext(Dispatchers.IO) {

                ids.forEach { id ->
                    messageDao.deleteMessage(id)
                }
            }

            selectedMessageIds = emptySet()

            NotificationHelper.updateUnreadBadge(
                context
            )
        }
    }


    /*
     * ============================================================
     * INTERFACE
     * ============================================================
     */

    Scaffold(

        topBar = {

            when {

                /*
                 * MODE SÉLECTION
                 */

                isSelectionMode -> {

                    TopAppBar(

                        title = {

                            Text(
                                "${selectedMessageIds.size} sélectionné(s)"
                            )
                        },

                        navigationIcon = {

                            IconButton(
                                onClick = {
                                    selectedMessageIds =
                                        emptySet()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription =
                                        "Quitter"
                                )
                            }
                        },

                        actions = {

                            /*
                             * COPIER
                             */

                            IconButton(
                                onClick = {

                                    val selectedMessages =
                                        messages
                                            .filter {
                                                selectedMessageIds
                                                    .contains(it.id)
                                            }
                                            .sortedBy {
                                                it.timestamp
                                            }

                                    val copiedText =
                                        selectedMessages.joinToString(
                                            "\n\n"
                                        ) {
                                            it.message
                                        }

                                    if (
                                        copiedText.isNotBlank()
                                    ) {

                                        val clipboard =
                                            context.getSystemService(
                                                Context.CLIPBOARD_SERVICE
                                            ) as ClipboardManager

                                        clipboard.setPrimaryClip(
                                            ClipData.newPlainText(
                                                "SMSPlanner",
                                                copiedText
                                            )
                                        )
                                    }

                                    selectedMessageIds =
                                        emptySet()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.ContentCopy,
                                    contentDescription =
                                        "Copier"
                                )
                            }


                            /*
                             * PARTAGER
                             */

                            IconButton(
                                onClick = {

                                    val selectedMessages =
                                        messages
                                            .filter {
                                                selectedMessageIds
                                                    .contains(it.id)
                                            }
                                            .sortedBy {
                                                it.timestamp
                                            }

                                    val sharedText =
                                        selectedMessages.joinToString(
                                            "\n\n"
                                        ) {
                                            it.message
                                        }

                                    if (
                                        sharedText.isNotBlank()
                                    ) {

                                        val intent =
                                            Intent(
                                                Intent.ACTION_SEND
                                            ).apply {

                                                type =
                                                    "text/plain"

                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    sharedText
                                                )
                                            }

                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                "Partager"
                                            )
                                        )
                                    }

                                    selectedMessageIds =
                                        emptySet()
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Share,
                                    contentDescription =
                                        "Partager"
                                )
                            }


                            /*
                             * SUPPRIMER
                             */

                            IconButton(
                                onClick = {

                                    if (
                                        deleteConfirmationEnabled
                                    ) {

                                        deleteConfirmationDialogVisible =
                                            true

                                    } else {

                                        deleteSelectedMessages()
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
                }


                /*
                 * MODE RECHERCHE
                 */

                isSearchMode -> {

                    TopAppBar(

                        title = {

                            OutlinedTextField(

                                value = searchText,

                                onValueChange = {
                                    searchText = it
                                },

                                modifier =
                                    Modifier.fillMaxWidth(),

                                singleLine = true,

                                placeholder = {
                                    Text(
                                        "Rechercher..."
                                    )
                                },

                                leadingIcon = {

                                    Icon(
                                        imageVector =
                                            Icons.Default.Search,
                                        contentDescription =
                                            "Recherche"
                                    )
                                },

                                trailingIcon = {

                                    if (
                                        searchText.isNotEmpty()
                                    ) {

                                        IconButton(
                                            onClick = {
                                                searchText = ""
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

                                shape =
                                    RoundedCornerShape(24.dp),

                                colors =
                                    OutlinedTextFieldDefaults.colors()
                            )
                        },

                        navigationIcon = {

                            IconButton(
                                onClick = {

                                    isSearchMode = false
                                    searchText = ""
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription =
                                        "Retour"
                                )
                            }
                        }
                    )
                }


                /*
                 * MODE NORMAL
                 */

                else -> {

                    TopAppBar(

                        title = {

                            Column {

                                Text(
                                    text =
                                        contactName
                                            ?: phoneNumber,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    text =
                                        phoneNumber,

                                    style =
                                        MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }
                        },

                        navigationIcon = {

                            IconButton(
                                onClick =
                                    onBackClick
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription =
                                        "Retour"
                                )
                            }
                        },

                        actions = {

                            /*
                             * RECHERCHE
                             */

                            IconButton(
                                onClick = {
                                    isSearchMode = true
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Search,
                                    contentDescription =
                                        "Rechercher"
                                )
                            }


                            /*
                             * APPEL
                             */

                            IconButton(
                                onClick = {

                                    val intent =
                                        Intent(
                                            Intent.ACTION_DIAL
                                        ).apply {

                                            data =
                                                Uri.parse(
                                                    "tel:$phoneNumber"
                                                )
                                        }

                                    context.startActivity(
                                        intent
                                    )
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Call,
                                    contentDescription =
                                        "Appeler"
                                )
                            }
                        }
                    )
                }
            }
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
        ) {

            /*
             * ====================================================
             * NOMBRE DE RÉSULTATS
             * ====================================================
             */

            if (
                isSearchMode &&
                searchText.isNotBlank()
            ) {

                Text(

                    text =
                        if (
                            filteredMessages.isEmpty()
                        ) {
                            "Aucun message trouvé"
                        } else {
                            "${filteredMessages.size} résultat(s)"
                        },

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 6.dp
                        ),

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }


            /*
             * ====================================================
             * LISTE DES MESSAGES
             * ====================================================
             */

            LazyColumn(

                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {

                            detectTransformGestures {

                                    _,
                                    _,
                                    zoom,
                                    _ ->

                                if (
                                    abs(zoom - 1f) > 0.01f
                                ) {

                                    val newSize =
                                        (
                                                messageTextSize * zoom
                                                ).coerceIn(
                                                TextSizePreference.MIN_SIZE,
                                                TextSizePreference.MAX_SIZE
                                            )

                                    messageTextSize =
                                        newSize

                                    TextSizePreference.setTextSize(
                                        context,
                                        newSize
                                    )
                                }
                            }
                        }
                        .padding(
                            horizontal = 12.dp
                        ),

                state =
                    listState,

                verticalArrangement =
                    Arrangement.spacedBy(8.dp)

            ) {

                items(

                    items =
                        filteredMessages,

                    key = {
                        it.id
                    }

                ) { message ->

                    MessageBubble(

                        message =
                            message,

                        textSize =
                            messageTextSize,

                        isSelected =
                            selectedMessageIds
                                .contains(message.id),

                        onLongPress = {

                            selectedMessageIds =

                                if (
                                    selectedMessageIds
                                        .contains(message.id)
                                ) {

                                    selectedMessageIds -
                                            message.id

                                } else {

                                    selectedMessageIds +
                                            message.id
                                }
                        },

                        onTap = {

                            if (
                                isSelectionMode
                            ) {

                                selectedMessageIds =

                                    if (
                                        selectedMessageIds
                                            .contains(message.id)
                                    ) {

                                        selectedMessageIds -
                                                message.id

                                    } else {

                                        selectedMessageIds +
                                                message.id
                                    }
                            }
                        }
                    )
                }
            }


            /*
             * ====================================================
             * BARRE D'ENVOI
             * ====================================================
             */

            if (
                !isSelectionMode
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(
                                horizontal = 6.dp,
                                vertical = 7.dp
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * SIM
                     */

                    Box(

                        modifier =
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .pointerInput(Unit) {

                                    detectTapGestures {

                                        if (
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.READ_PHONE_STATE
                                            ) ==
                                            PackageManager.PERMISSION_GRANTED
                                        ) {

                                            loadSimCards()
                                            showSimDialog = true

                                        } else {

                                            simPermissionLauncher.launch(
                                                Manifest.permission.READ_PHONE_STATE
                                            )
                                        }
                                    }
                                },

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.SimCard,

                            contentDescription =
                                "Choisir la SIM",

                            tint =
                                SendGreen,

                            modifier =
                                Modifier.size(27.dp)
                        )

                        if (
                            selectedSim != null
                        ) {

                            Box(

                                modifier =
                                    Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(SendGreen)
                                        .align(Alignment.TopEnd),

                                contentAlignment =
                                    Alignment.Center
                            ) {

                                Text(

                                    text =
                                        (
                                                selectedSim!!
                                                    .simSlotIndex + 1
                                                ).toString(),

                                    color =
                                        Color.White,

                                    fontSize =
                                        9.sp
                                )
                            }
                        }
                    }


                    Spacer(
                        modifier =
                            Modifier.width(4.dp)
                    )


                    /*
                     * ====================================================
                     * CHAMP TEXTE
                     * ====================================================
                     *
                     * IMPORTANT :
                     * Le contour ne devient plus vert lorsque le champ
                     * reçoit le focus.
                     */

                    OutlinedTextField(

                        value =
                            text,

                        onValueChange = {
                            text = it
                        },

                        modifier =
                            Modifier.weight(1f),

                        enabled =
                            !isSending,

                        placeholder = {
                            Text(
                                "Écrire un message..."
                            )
                        },

                        maxLines = 4,

                        shape =
                            RoundedCornerShape(24.dp),

                        colors =
                            OutlinedTextFieldDefaults.colors(

                                /*
                                 * Fond normal
                                 */

                                unfocusedContainerColor =
                                    Color.Transparent,

                                /*
                                 * Fond lorsque le champ est sélectionné
                                 */

                                focusedContainerColor =
                                    Color.Transparent,

                                /*
                                 * Bordure normale
                                 */

                                unfocusedBorderColor =
                                    MaterialTheme
                                        .colorScheme
                                        .outline,

                                /*
                                 * Bordure lorsqu'on clique :
                                 * même couleur que la bordure normale
                                 */

                                focusedBorderColor =
                                    MaterialTheme
                                        .colorScheme
                                        .outline,

                                /*
                                 * Curseur vert
                                 */

                                cursorColor =
                                    SendGreen
                            )
                    )


                    /*
                     * PROGRAMMER
                     */

                    IconButton(

                        enabled =
                            !isSending &&
                                    text.trim().isNotEmpty(),

                        onClick = {

                            val message =
                                text.trim()

                            if (
                                message.isNotEmpty()
                            ) {

                                onProgramMessageClick(
                                    phoneNumber,
                                    message
                                )
                            }
                        }

                    ) {

                        Icon(
                            imageVector =
                                Icons.Default.Schedule,
                            contentDescription =
                                "Programmer"
                        )
                    }


                    /*
                     * ENVOYER
                     */

                    IconButton(

                        enabled =
                            !isSending &&
                                    text.trim().isNotEmpty(),

                        onClick = {

                            val message =
                                text.trim()

                            if (
                                message.isEmpty() ||
                                isSending
                            ) {
                                return@IconButton
                            }

                            isSending = true

                            coroutineScope.launch {

                                try {

                                    val sent =
                                        envoyerSmsUneSeuleFois(

                                            context =
                                                context,

                                            phoneNumber =
                                                phoneNumber,

                                            message =
                                                message,

                                            messageDao =
                                                messageDao,

                                            selectedSim =
                                                selectedSim
                                        )

                                    if (sent) {
                                        text = ""
                                    }

                                } finally {

                                    delay(300)

                                    isSending = false
                                }
                            }
                        }

                    ) {

                        if (isSending) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.size(22.dp),

                                strokeWidth =
                                    2.dp
                            )

                        } else {

                            Icon(
                                imageVector =
                                    Icons.Default.Send,

                                contentDescription =
                                    "Envoyer"
                            )
                        }
                    }
                }
            }
        }
    }


    /*
     * ============================================================
     * DIALOGUE SUPPRESSION
     * ============================================================
     */

    if (
        deleteConfirmationDialogVisible
    ) {

        AlertDialog(

            onDismissRequest = {
                deleteConfirmationDialogVisible =
                    false
            },

            title = {

                Text(
                    if (
                        selectedMessageIds.size == 1
                    ) {
                        "Supprimer le message ?"
                    } else {
                        "Supprimer les messages ?"
                    }
                )
            },

            text = {

                Text(
                    if (
                        selectedMessageIds.size == 1
                    ) {
                        "Voulez-vous vraiment supprimer ce message ?"
                    } else {
                        "Voulez-vous vraiment supprimer les ${selectedMessageIds.size} messages sélectionnés ?"
                    }
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        deleteConfirmationDialogVisible =
                            false

                        deleteSelectedMessages()
                    }
                ) {

                    Text(
                        "Supprimer"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteConfirmationDialogVisible =
                            false
                    }
                ) {

                    Text(
                        "Annuler"
                    )
                }
            }
        )
    }


    /*
     * ============================================================
     * DIALOGUE SIM
     * ============================================================
     */

    if (showSimDialog) {

        AlertDialog(

            onDismissRequest = {
                showSimDialog = false
            },

            title = {

                Text(
                    "Choisir une SIM"
                )
            },

            text = {

                if (simList.isEmpty()) {

                    Text(
                        "Aucune SIM disponible."
                    )

                } else {

                    Column {

                        simList.forEach { sim ->

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .pointerInput(
                                            sim.subscriptionId
                                        ) {

                                            detectTapGestures {

                                                selectedSim =
                                                    sim

                                                showSimDialog =
                                                    false
                                            }
                                        }
                                        .padding(
                                            vertical = 14.dp
                                        ),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Default.SimCard,

                                    contentDescription =
                                        null,

                                    tint =
                                        SendGreen,

                                    modifier =
                                        Modifier.size(30.dp)
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(14.dp)
                                )

                                Column {

                                    Text(
                                        text =
                                            sim.displayName
                                                ?.toString()
                                                ?: "Opérateur"
                                    )

                                    Text(
                                        text =
                                            "SIM ${
                                                sim.simSlotIndex + 1
                                            }",

                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant,

                                        fontSize =
                                            13.sp
                                    )
                                }
                            }

                            HorizontalDivider()
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showSimDialog = false
                    }
                ) {

                    Text(
                        "Fermer",
                        color = SendGreen
                    )
                }
            }
        )
    }
}


/*
 * ================================================================
 * BULLE MESSAGE
 * ================================================================
 */

@Composable
private fun MessageBubble(

    message: MessageEntity,

    textSize: Float,

    isSelected: Boolean,

    onLongPress: () -> Unit,

    onTap: () -> Unit

) {

    val context =
        LocalContext.current

    val isSent =
        message.type == "SENT"

    val sentBubbleColor =
        Color(0xFF2E7D52)

    val receivedBubbleColor =
        Color(0xFF263238)

    val normalTextColor =
        Color(0xFFE8F5E9)

    val backgroundColor =

        if (isSelected) {

            MaterialTheme
                .colorScheme
                .secondaryContainer

        } else if (isSent) {

            sentBubbleColor

        } else {

            receivedBubbleColor
        }

    val textColor =

        if (isSelected) {

            MaterialTheme
                .colorScheme
                .onSecondaryContainer

        } else {

            normalTextColor
        }

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        horizontalArrangement =

            if (isSent) {
                Arrangement.End
            } else {
                Arrangement.Start
            }

    ) {

        Box(

            modifier =
                Modifier
                    .fillMaxWidth(0.82f)
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart =
                                if (isSent) 18.dp else 4.dp,
                            bottomEnd =
                                if (isSent) 4.dp else 18.dp
                        )
                    )
                    .padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )

        ) {

            Column {

                /*
                 * TEXTE DU MESSAGE
                 */

                ClickableMessageText(

                    text =
                        message.message,

                    textSize =
                        textSize,

                    textColor =
                        textColor,

                    onLongPress =
                        onLongPress,

                    onTap =
                        onTap,

                    onLinkClick = { type, value ->

                        openDetectedLink(
                            context,
                            type,
                            value
                        )
                    }
                )


                /*
                 * HEURE
                 */

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(

                        text =
                            formatTime(
                                message.timestamp
                            ),

                        color =
                            textColor.copy(
                                alpha = 0.70f
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )


                    /*
                     * STATUT
                     */

                    if (isSent) {

                        Spacer(
                            modifier =
                                Modifier.width(6.dp)
                        )

                        Text(

                            text =
                                message.status,

                            color =
                                when (
                                    message.status
                                ) {

                                    "envoyé" ->
                                        Color(0xFF64B5F6)

                                    "échec" ->
                                        Color(0xFFF44336)

                                    "envoi" ->
                                        Color(0xFFFFB74D)

                                    else ->
                                        textColor.copy(
                                            alpha = 0.80f
                                        )
                                },

                            style =
                                MaterialTheme
                                    .typography
                                    .labelSmall
                        )
                    }
                }
            }
        }
    }
}


/*
 * ================================================================
 * TEXTE CLIQUABLE
 * ================================================================
 */

@Composable
private fun ClickableMessageText(

    text: String,

    textSize: Float,

    textColor: Color,

    onLongPress: () -> Unit,

    onTap: () -> Unit,

    onLinkClick: (String, String) -> Unit

) {

    val annotatedText =
        remember(
            text
        ) {

            buildClickableMessage(
                text
            )
        }

    var layoutResult by remember {
        mutableStateOf<TextLayoutResult?>(null)
    }

    BasicText(

        text =
            annotatedText,

        modifier =
            Modifier.pointerInput(
                annotatedText
            ) {

                detectTapGestures(

                    onLongPress = {
                        onLongPress()
                    },

                    onTap = { position ->

                        val layout =
                            layoutResult
                                ?: return@detectTapGestures

                        val offset =
                            layout.getOffsetForPosition(
                                position
                            )

                        val annotations =
                            annotatedText
                                .getStringAnnotations(
                                    start = offset,
                                    end = offset
                                )

                        val annotation =
                            annotations.firstOrNull()

                        if (
                            annotation != null
                        ) {

                            onLinkClick(
                                annotation.tag,
                                annotation.item
                            )

                        } else {

                            onTap()
                        }
                    }
                )
            },

        style =
            MaterialTheme
                .typography
                .bodyLarge
                .copy(
                    color =
                        textColor,

                    fontSize =
                        textSize.sp
                ),

        onTextLayout = {
            layoutResult = it
        }
    )
}


/*
 * ================================================================
 * CONSTRUCTION DES LIENS
 * ================================================================
 */

private fun buildClickableMessage(
    text: String
): AnnotatedString {

    val builder =
        AnnotatedString.Builder()

    val matches =
        mutableListOf<DetectedLink>()


    /*
     * EMAIL
     */

    val emailRegex =
        Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        )

    val emailMatcher =
        emailRegex.matcher(text)

    while (
        emailMatcher.find()
    ) {

        matches.add(
            DetectedLink(
                start =
                    emailMatcher.start(),

                end =
                    emailMatcher.end(),

                type =
                    ANNOTATION_EMAIL,

                value =
                    emailMatcher.group()
            )
        )
    }


    /*
     * URL
     */

    val urlRegex =
        Pattern.compile(
            "(?i)\\b((https?://|www\\.)[^\\s]+)"
        )

    val urlMatcher =
        urlRegex.matcher(text)

    while (
        urlMatcher.find()
    ) {

        var start =
            urlMatcher.start(1)

        var end =
            urlMatcher.end(1)

        val original =
            text.substring(
                start,
                end
            )

        val cleaned =
            original.trimEnd(
                '.',
                ',',
                '!',
                '?',
                ';',
                ':',
                ')',
                ']',
                '}'
            )

        end =
            start + cleaned.length

        if (
            start < end &&
            !hasOverlap(
                matches,
                start,
                end
            )
        ) {

            matches.add(
                DetectedLink(
                    start =
                        start,

                    end =
                        end,

                    type =
                        ANNOTATION_URL,

                    value =
                        cleaned
                )
            )
        }
    }


    /*
     * NUMÉRO DE TÉLÉPHONE
     */

    val phoneRegex =
        Pattern.compile(
            "(?<!\\d)(?:\\+225[\\s.-]?)?0[1257](?:[\\s.-]?\\d{2}){4}(?!\\d)"
        )

    val phoneMatcher =
        phoneRegex.matcher(text)

    while (
        phoneMatcher.find()
    ) {

        val start =
            phoneMatcher.start()

        val end =
            phoneMatcher.end()

        if (
            !hasOverlap(
                matches,
                start,
                end
            )
        ) {

            matches.add(
                DetectedLink(
                    start =
                        start,

                    end =
                        end,

                    type =
                        ANNOTATION_PHONE,

                    value =
                        phoneMatcher.group()
                )
            )
        }
    }


    /*
     * TRI
     */

    matches.sortBy {
        it.start
    }


    /*
     * CONSTRUCTION DU TEXTE
     */

    var currentIndex =
        0

    matches.forEach { link ->

        if (
            link.start > currentIndex
        ) {

            builder.append(
                text.substring(
                    currentIndex,
                    link.start
                )
            )
        }


        builder.pushStringAnnotation(
            tag =
                link.type,

            annotation =
                link.value
        )


        builder.pushStyle(

            SpanStyle(
                color =
                    Color(0xFF64B5F6),

                textDecoration =
                    TextDecoration.Underline
            )
        )


        builder.append(
            text.substring(
                link.start,
                link.end
            )
        )


        builder.pop()

        builder.pop()


        currentIndex =
            link.end
    }


    if (
        currentIndex < text.length
    ) {

        builder.append(
            text.substring(
                currentIndex
            )
        )
    }

    return builder.toAnnotatedString()
}


/*
 * ================================================================
 * VÉRIFIER CHEVAUCHEMENT
 * ================================================================
 */

private fun hasOverlap(

    links: List<DetectedLink>,

    start: Int,

    end: Int

): Boolean {

    return links.any { link ->

        start < link.end &&
                end > link.start
    }
}


/*
 * ================================================================
 * STRUCTURE LIEN
 * ================================================================
 */

private data class DetectedLink(

    val start: Int,

    val end: Int,

    val type: String,

    val value: String
)


/*
 * ================================================================
 * OUVRIR UN LIEN
 * ================================================================
 */

private fun openDetectedLink(

    context: Context,

    type: String,

    value: String

) {

    try {

        when (type) {

            /*
             * NUMÉRO
             */

            ANNOTATION_PHONE -> {

                val intent =
                    Intent(
                        Intent.ACTION_DIAL
                    ).apply {

                        data =
                            Uri.parse(
                                "tel:${Uri.encode(value)}"
                            )
                    }

                context.startActivity(
                    intent
                )
            }


            /*
             * EMAIL
             */

            ANNOTATION_EMAIL -> {

                val intent =
                    Intent(
                        Intent.ACTION_SENDTO
                    ).apply {

                        data =
                            Uri.parse(
                                "mailto:${Uri.encode(value)}"
                            )
                    }

                context.startActivity(
                    Intent.createChooser(
                        intent,
                        "Envoyer un e-mail"
                    )
                )
            }


            /*
             * LIEN INTERNET / WHATSAPP
             */

            ANNOTATION_URL -> {

                val url =

                    if (
                        value.startsWith(
                            "http://",
                            ignoreCase = true
                        ) ||
                        value.startsWith(
                            "https://",
                            ignoreCase = true
                        )
                    ) {

                        value

                    } else {

                        "https://$value"
                    }

                val intent =
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )

                context.startActivity(
                    intent
                )
            }
        }

    } catch (
        e: Exception
    ) {

        e.printStackTrace()
    }
}


/*
 * ================================================================
 * NOM DU CONTACT
 * ================================================================
 */

private fun getContactName(

    context: Context,

    phoneNumber: String

): String? {

    val normalizedNumber =
        phoneNumber
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

    val uri =
        Uri.withAppendedPath(
            ContactsContract
                .PhoneLookup
                .CONTENT_FILTER_URI,

            Uri.encode(
                normalizedNumber
            )
        )

    val projection =
        arrayOf(
            ContactsContract
                .PhoneLookup
                .DISPLAY_NAME
        )

    return try {

        context.contentResolver.query(

            uri,

            projection,

            null,

            null,

            null

        )?.use { cursor ->

            if (
                cursor.moveToFirst()
            ) {

                val index =
                    cursor.getColumnIndex(
                        ContactsContract
                            .PhoneLookup
                            .DISPLAY_NAME
                    )

                if (
                    index >= 0
                ) {

                    cursor.getString(
                        index
                    )

                } else {

                    null
                }

            } else {

                null
            }
        }

    } catch (
        e: SecurityException
    ) {

        null
    }
}


/*
 * ================================================================
 * ENVOYER SMS
 * ================================================================
 */

private suspend fun envoyerSmsUneSeuleFois(

    context: Context,

    phoneNumber: String,

    message: String,

    messageDao: MessageDao,

    selectedSim: SubscriptionInfo?

): Boolean {

    return sendMutex.withLock {

        withContext(Dispatchers.IO) {

            /*
             * ANTI-DOUBLON
             */

            val minimumTimestamp =
                System.currentTimeMillis() - 5000L

            val duplicate =
                messageDao.findRecentSentDuplicate(

                    phoneNumber =
                        phoneNumber,

                    message =
                        message,

                    minimumTimestamp =
                        minimumTimestamp
                )

            if (
                duplicate != null
            ) {

                return@withContext false
            }


            /*
             * SMS MANAGER
             */

            try {

                val smsManager =

                    if (
                        selectedSim != null
                    ) {

                        SmsManager
                            .getSmsManagerForSubscriptionId(
                                selectedSim.subscriptionId
                            )

                    } else {

                        SmsManager.getDefault()
                    }


                /*
                 * DIVISER LE MESSAGE SI NÉCESSAIRE
                 */

                val parts =
                    smsManager.divideMessage(
                        message
                    )


                if (
                    parts.size == 1
                ) {

                    smsManager.sendTextMessage(

                        phoneNumber,

                        null,

                        message,

                        null,

                        null
                    )

                } else {

                    smsManager.sendMultipartTextMessage(

                        phoneNumber,

                        null,

                        parts,

                        null,

                        null
                    )
                }

            } catch (
                e: Exception
            ) {

                e.printStackTrace()

                return@withContext false
            }


            /*
             * ENREGISTRER LE MESSAGE
             */

            messageDao.insertMessage(

                MessageEntity(

                    phoneNumber =
                        phoneNumber,

                    message =
                        message,

                    timestamp =
                        System.currentTimeMillis(),

                    type =
                        "SENT",

                    isRead =
                        true,

                    status =
                        "envoyé"
                )
            )

            true
        }
    }
}


/*
 * ================================================================
 * FORMAT HEURE
 * ================================================================
 */

private fun formatTime(
    timestamp: Long
): String {

    val calendar =
        Calendar.getInstance()

    calendar.timeInMillis =
        timestamp

    val hour =
        calendar.get(
            Calendar.HOUR_OF_DAY
        )

    val minute =
        calendar.get(
            Calendar.MINUTE
        )

    return String.format(
        "%02d:%02d",
        hour,
        minute
    )
}