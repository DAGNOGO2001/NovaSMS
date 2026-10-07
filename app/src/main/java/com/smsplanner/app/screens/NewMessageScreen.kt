package com.smsplanner.app.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.widget.Toast

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.core.content.ContextCompat

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageEntity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


/*
 * ============================================================
 * COULEUR VERTE DE L'APPLICATION
 * ============================================================
 */

private val SendGreen = Color(0xFF22C55E)


/*
 * ============================================================
 * CONTACT POUR LES SUGGESTIONS
 * ============================================================
 */

private data class ContactSuggestion(
    val name: String,
    val phoneNumber: String
)


/*
 * ============================================================
 * ÉCRAN : NOUVEAU MESSAGE
 * ============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    onBackClick: () -> Unit = {},

    onScheduleClick: (
        phoneNumber: String,
        message: String
    ) -> Unit = { _, _ -> },

    onSendClick: () -> Unit = {}
) {

    val context = LocalContext.current

    /*
     * ========================================================
     * ROOM
     * ========================================================
     */

    val database = remember {
        AppDatabase.getDatabase(context)
    }

    val messageDao = remember {
        database.messageDao()
    }

    val coroutineScope = rememberCoroutineScope()


    /*
     * ========================================================
     * CLAVIER
     * ========================================================
     */

    val keyboardController =
        LocalSoftwareKeyboardController.current


    /*
     * ========================================================
     * COULEURS
     * ========================================================
     */

    val colorScheme =
        MaterialTheme.colorScheme

    val backgroundColor =
        colorScheme.background

    val surfaceColor =
        colorScheme.surface

    val inputColor =
        colorScheme.surfaceVariant

    val onBackgroundColor =
        colorScheme.onBackground

    val onSurfaceColor =
        colorScheme.onSurface

    val secondaryTextColor =
        colorScheme.onSurfaceVariant


    /*
     * ========================================================
     * ÉTATS
     * ========================================================
     */

    var phoneNumber by remember {
        mutableStateOf("")
    }

    var selectedContactName by remember {
        mutableStateOf<String?>(null)
    }

    var contactSuggestions by remember {
        mutableStateOf<List<ContactSuggestion>>(
            emptyList()
        )
    }

    var message by remember {
        mutableStateOf("")
    }

    var showAttachmentMenu by remember {
        mutableStateOf(false)
    }

    var showSimDialog by remember {
        mutableStateOf(false)
    }

    var simList by remember {
        mutableStateOf<List<SubscriptionInfo>>(
            emptyList()
        )
    }

    var selectedSim by remember {
        mutableStateOf<SubscriptionInfo?>(null)
    }

    var showEmojiPicker by remember {
        mutableStateOf(false)
    }

    var selectedAttachmentUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedAttachmentName by remember {
        mutableStateOf<String?>(null)
    }

    var capturedPhoto by remember {
        mutableStateOf<Bitmap?>(null)
    }


    /*
     * ========================================================
     * RECHERCHE DES CONTACTS
     * ========================================================
     */

    LaunchedEffect(phoneNumber) {

        val searchText =
            phoneNumber.trim()

        if (
            searchText.isEmpty() ||
            searchText.length < 2
        ) {

            contactSuggestions =
                emptyList()

            selectedContactName =
                null

            return@LaunchedEffect
        }

        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {

            contactSuggestions =
                emptyList()

            return@LaunchedEffect
        }

        contactSuggestions =
            withContext(Dispatchers.IO) {

                searchContacts(
                    context,
                    searchText
                )
            }
    }


    /*
     * ========================================================
     * EMOJIS
     * ========================================================
     */

    val emojis = remember {

        listOf(

            // Visages
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
            "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
            "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜",
            "🤪", "🤨", "🧐", "🤓", "😎", "🤩", "🥳", "😏",
            "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
            "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠",
            "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨",
            "😰", "😥", "😓", "🤗", "🤔", "🫣", "🤭", "🤫",
            "🤥", "😶", "😐", "😑", "😬", "🙄", "😯", "😦",
            "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵",
            "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕",

            // Gestes
            "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙",
            "👈", "👉", "👆", "👇", "☝️", "✋", "🤚", "🖐️",
            "🖖", "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💪",
            "👋", "🤏", "✍️", "💅", "🤳",

            // Cœurs
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "❤️‍🔥", "❤️‍🩹",

            // Personnes
            "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "👩",
            "🧔", "👴", "👵", "🙍", "🙎", "🙅", "🙆", "💁",
            "🙋", "🧏", "🙇", "🤦", "🤷", "👮", "🕵️", "💂",
            "👷", "🤴", "👸", "👳", "👲", "🧕", "🤵", "👰",

            // Animaux
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈",
            "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🦆",
            "🦅", "🦉", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛",
            "🦋", "🐌", "🐞", "🐜", "🕷️", "🐢", "🐍", "🦎",
            "🐙", "🦑", "🦀", "🐠", "🐟", "🐡", "🐬", "🐳",
            "🐋", "🦈", "🐊", "🐘", "🦏", "🦛", "🦒", "🦓",

            // Nourriture
            "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇",
            "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥",
            "🥝", "🍅", "🥑", "🍆", "🥔", "🥕", "🌽", "🌶️",
            "🍔", "🍟", "🍕", "🌭", "🌮", "🌯", "🥪", "🍗",
            "🍖", "🍣", "🍤", "🍚", "🍜", "🍝", "🍰", "🎂",
            "🍩", "🍪", "🍫", "🍿", "🍭", "☕", "🍵", "🥤",

            // Nature
            "🌸", "🌹", "🌺", "🌻", "🌼", "🌷", "🌱", "🌲",
            "🌳", "🌴", "🌵", "🌿", "☘️", "🍀", "🍁", "🍂",
            "🍃", "🌍", "🌎", "🌏", "🌙", "⭐", "🌟", "✨",
            "⚡", "🔥", "🌈", "☀️", "🌤️", "🌧️", "❄️", "☃️",

            // Technologie
            "📱", "💻", "⌨️", "🖥️", "🖨️", "📷", "📸", "📹",
            "☎️", "📞", "📺", "🎧", "🎤", "🎸", "🎹", "🎮",

            // Transport
            "🚗", "🚕", "🚌", "🏎️", "🚓", "🚑", "🚒", "🚲",
            "✈️", "🚀", "🚁", "⛵", "🏠", "🏢", "🏫", "🏥",

            // Objets
            "💡", "🔑", "🔒", "🔓", "💰", "💳", "📚", "✏️",
            "📌", "📎", "📁", "📂", "📅", "⏰", "⌚", "💎",

            // Symboles
            "✅", "❌", "❗", "❓", "‼️", "⁉️", "⚠️", "⭕",
            "❎", "✔️", "➕", "➖", "✖️", "➗", "💯", "🔴",
            "🟠", "🟡", "🟢", "🔵", "🟣", "⚫", "⚪", "🟤",

            // Notifications / sport
            "🔔", "🔕", "🔝", "🔜", "♻️", "☑️", "⚽", "🏀",
            "🏈", "⚾", "🎾", "🏆", "🥇", "🥈", "🥉", "🎉",
            "🎊", "🎁", "🎈", "🎂", "🎵", "🎶"
        )
    }


    /*
     * ========================================================
     * CHARGEMENT DES SIM
     * ========================================================
     */

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

                selectedSim =
                    subscriptions.first()
            }

        } catch (e: SecurityException) {

            simList = emptyList()
        }
    }


    /*
     * ========================================================
     * PERMISSION SIM
     * ========================================================
     */

    val simPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadSimCards()
            }
        }


    /*
     * ========================================================
     * CHARGEMENT AUTOMATIQUE DES SIM
     * ========================================================
     */

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
     * ========================================================
     * CONTACTS : SÉLECTEUR ANDROID
     * ========================================================
     */

    val contactPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (
                result.resultCode == Activity.RESULT_OK
            ) {

                val contactUri =
                    result.data?.data

                if (contactUri != null) {

                    val cursor =
                        context.contentResolver.query(
                            contactUri,
                            arrayOf(
                                ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .DISPLAY_NAME,

                                ContactsContract
                                    .CommonDataKinds
                                    .Phone
                                    .NUMBER
                            ),
                            null,
                            null,
                            null
                        )

                    cursor?.use {

                        if (it.moveToFirst()) {

                            val nameIndex =
                                it.getColumnIndex(
                                    ContactsContract
                                        .CommonDataKinds
                                        .Phone
                                        .DISPLAY_NAME
                                )

                            val numberIndex =
                                it.getColumnIndex(
                                    ContactsContract
                                        .CommonDataKinds
                                        .Phone
                                        .NUMBER
                                )

                            if (numberIndex >= 0) {

                                phoneNumber =
                                    it.getString(
                                        numberIndex
                                    )
                            }

                            if (nameIndex >= 0) {

                                selectedContactName =
                                    it.getString(
                                        nameIndex
                                    )
                            }

                            contactSuggestions =
                                emptyList()
                        }
                    }
                }
            }
        }


    /*
     * ========================================================
     * FICHIER
     * ========================================================
     */

    val filePickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                capturedPhoto = null

                selectedAttachmentUri =
                    uri

                selectedAttachmentName =
                    uri.lastPathSegment
                        ?: "Fichier sélectionné"
            }
        }


    /*
     * ========================================================
     * GALERIE
     * ========================================================
     */

    val galleryPickerLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            if (uri != null) {

                capturedPhoto = null

                selectedAttachmentUri =
                    uri

                selectedAttachmentName =
                    uri.lastPathSegment
                        ?: "Image sélectionnée"
            }
        }


    /*
     * ========================================================
     * CAMÉRA
     * ========================================================
     */

    val cameraLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicturePreview()
        ) { bitmap ->

            if (bitmap != null) {

                selectedAttachmentUri = null

                capturedPhoto = bitmap

                selectedAttachmentName =
                    "Photo prise avec la caméra"
            }
        }


    /*
     * ========================================================
     * PERMISSION CAMÉRA
     * ========================================================
     */

    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                cameraLauncher.launch(null)
            }
        }


    /*
     * ========================================================
     * OUVRIR LA CAMÉRA
     * ========================================================
     */

    fun openCamera() {

        val permission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            )

        if (
            permission ==
            PackageManager.PERMISSION_GRANTED
        ) {

            cameraLauncher.launch(null)

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    /*
     * ========================================================
     * INTERFACE PRINCIPALE
     * ========================================================
     */

    Scaffold(

        modifier =
            Modifier.fillMaxSize(),

        containerColor =
            backgroundColor,

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Nouveau message",
                        color = onSurfaceColor,
                        fontWeight =
                            FontWeight.Bold
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
                                "Retour",

                            tint =
                                onSurfaceColor
                        )
                    }
                },

                actions = {

                    TextButton(

                        onClick = {

                            if (
                                phoneNumber.isNotBlank() &&
                                message.isNotBlank()
                            ) {

                                onScheduleClick(
                                    phoneNumber.trim(),
                                    message.trim()
                                )

                            } else {

                                Toast.makeText(
                                    context,
                                    "Veuillez saisir un numéro et un message.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {

                        Text(
                            text = "Programmer",
                            color = SendGreen,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }
                },

                colors =
                    TopAppBarDefaults
                        .topAppBarColors(

                            containerColor =
                                backgroundColor,

                            titleContentColor =
                                onSurfaceColor,

                            navigationIconContentColor =
                                onSurfaceColor,

                            actionIconContentColor =
                                SendGreen
                        )
            )
        }

    ) { innerPadding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),

            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                /*
                 * =================================================
                 * DESTINATAIRE / RECHERCHE CONTACT
                 * =================================================
                 */

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            )
                ) {

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "À :",
                            color =
                                secondaryTextColor,
                            fontSize = 16.sp
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        BasicTextField(

                            value =
                                phoneNumber,

                            onValueChange = {

                                phoneNumber = it

                                selectedContactName =
                                    null
                            },

                            singleLine = true,

                            keyboardOptions =
                                KeyboardOptions(
                                    keyboardType =
                                        KeyboardType.Text
                                ),

                            textStyle =
                                TextStyle(
                                    color =
                                        onBackgroundColor,
                                    fontSize =
                                        16.sp
                                ),

                            cursorBrush =
                                SolidColor(
                                    SendGreen
                                ),

                            modifier =
                                Modifier.weight(1f),

                            decorationBox = {
                                    innerTextField ->

                                if (
                                    phoneNumber.isEmpty()
                                ) {

                                    Text(
                                        text =
                                            "Rechercher un contact ou un numéro",

                                        color =
                                            secondaryTextColor,

                                        fontSize =
                                            16.sp
                                    )
                                }

                                innerTextField()
                            }
                        )

                        /*
                         * Icône PERSONNE
                         */
                        IconButton(

                            onClick = {

                                val intent =
                                    Intent(
                                        Intent.ACTION_PICK,
                                        ContactsContract
                                            .CommonDataKinds
                                            .Phone
                                            .CONTENT_URI
                                    )

                                contactPickerLauncher
                                    .launch(intent)
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Person,

                                contentDescription =
                                    "Choisir un contact",

                                tint =
                                    onSurfaceColor
                            )
                        }
                    }

                    /*
                     * Contact sélectionné
                     */
                    if (
                        selectedContactName != null
                    ) {

                        Text(
                            text =
                                selectedContactName!!,

                            color =
                                SendGreen,

                            fontSize =
                                13.sp,

                            modifier =
                                Modifier.padding(
                                    start = 34.dp,
                                    top = 3.dp
                                )
                        )
                    }

                    /*
                     * =================================================
                     * SUGGESTIONS DE CONTACTS
                     * =================================================
                     */

                    if (
                        contactSuggestions.isNotEmpty()
                    ) {

                        Column(

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = 34.dp,
                                        top = 6.dp
                                    )
                                    .clip(
                                        RoundedCornerShape(14.dp)
                                    )
                                    .background(
                                        inputColor
                                    )
                        ) {

                            contactSuggestions.forEach { contact ->

                                Row(

                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {

                                                phoneNumber =
                                                    contact.phoneNumber

                                                selectedContactName =
                                                    contact.name

                                                contactSuggestions =
                                                    emptyList()

                                                keyboardController
                                                    ?.hide()
                                            }
                                            .padding(
                                                horizontal = 12.dp,
                                                vertical = 10.dp
                                            ),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Box(

                                        modifier =
                                            Modifier
                                                .size(42.dp)
                                                .clip(
                                                    CircleShape
                                                )
                                                .background(
                                                    colorScheme
                                                        .secondaryContainer
                                                ),

                                        contentAlignment =
                                            Alignment.Center
                                    ) {

                                        Icon(
                                            imageVector =
                                                Icons.Default.Person,

                                            contentDescription =
                                                null,

                                            tint =
                                                colorScheme
                                                    .onSecondaryContainer,

                                            modifier =
                                                Modifier.size(23.dp)
                                        )
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.width(12.dp)
                                    )

                                    Column(
                                        modifier =
                                            Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text =
                                                contact.name,

                                            color =
                                                onSurfaceColor,

                                            fontSize =
                                                15.sp,

                                            fontWeight =
                                                FontWeight.Medium
                                        )

                                        Text(
                                            text =
                                                contact.phoneNumber,

                                            color =
                                                secondaryTextColor,

                                            fontSize =
                                                13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),

                        color =
                            colorScheme.outlineVariant
                    )
                }


                /*
                 * =================================================
                 * PHOTO PRISE
                 * =================================================
                 */

                if (capturedPhoto != null) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                                .clip(
                                    RoundedCornerShape(14.dp)
                                )
                                .background(
                                    surfaceColor
                                )
                                .padding(12.dp)
                    ) {

                        Image(

                            bitmap =
                                capturedPhoto!!
                                    .asImageBitmap(),

                            contentDescription =
                                "Photo prise",

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(
                                        RoundedCornerShape(12.dp)
                                    )
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.CameraAlt,

                                contentDescription =
                                    null,

                                tint =
                                    SendGreen
                            )

                            Spacer(
                                modifier =
                                    Modifier.width(8.dp)
                            )

                            Text(
                                text =
                                    "Photo prise avec la caméra",

                                color =
                                    onSurfaceColor,

                                fontSize =
                                    14.sp,

                                modifier =
                                    Modifier.weight(1f)
                            )

                            IconButton(

                                onClick = {

                                    capturedPhoto =
                                        null

                                    selectedAttachmentName =
                                        null
                                }
                            ) {

                                Icon(
                                    imageVector =
                                        Icons.Default.Delete,

                                    contentDescription =
                                        "Supprimer la photo",

                                    tint =
                                        colorScheme.error
                                )
                            }
                        }
                    }
                }


                /*
                 * =================================================
                 * FICHIER / GALERIE
                 * =================================================
                 */

                if (
                    selectedAttachmentUri != null
                ) {

                    Row(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                                .clip(
                                    RoundedCornerShape(14.dp)
                                )
                                .background(
                                    surfaceColor
                                )
                                .padding(12.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Box(

                            modifier =
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        colorScheme
                                            .secondaryContainer
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.AttachFile,

                                contentDescription =
                                    null,

                                tint =
                                    colorScheme
                                        .onSecondaryContainer,

                                modifier =
                                    Modifier.size(22.dp)
                            )
                        }

                        Spacer(
                            modifier =
                                Modifier.width(12.dp)
                        )

                        Text(

                            text =
                                selectedAttachmentName
                                    ?: "Pièce jointe sélectionnée",

                            color =
                                onSurfaceColor,

                            fontSize =
                                14.sp,

                            maxLines = 1,

                            modifier =
                                Modifier.weight(1f)
                        )

                        IconButton(

                            onClick = {

                                selectedAttachmentUri =
                                    null

                                selectedAttachmentName =
                                    null
                            }
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Delete,

                                contentDescription =
                                    "Supprimer la pièce jointe",

                                tint =
                                    colorScheme.error
                            )
                        }
                    }
                }


                /*
                 * =================================================
                 * EMOJIS
                 * =================================================
                 */

                if (showEmojiPicker) {

                    Column(

                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    surfaceColor
                                )
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 8.dp
                                )
                    ) {

                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text = "Emojis",

                                color =
                                    onSurfaceColor,

                                fontWeight =
                                    FontWeight.Bold,

                                modifier =
                                    Modifier.weight(1f)
                            )

                            TextButton(

                                onClick = {

                                    showEmojiPicker =
                                        false

                                    keyboardController
                                        ?.show()
                                }
                            ) {

                                Text(
                                    text = "⌨️",
                                    fontSize = 22.sp
                                )
                            }
                        }

                        LazyVerticalGrid(

                            columns =
                                GridCells.Fixed(8),

                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(230.dp)
                        ) {

                            items(emojis) { emoji ->

                                Box(

                                    modifier =
                                        Modifier
                                            .size(44.dp)
                                            .clickable {

                                                message +=
                                                    emoji
                                            },

                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text = emoji,
                                        fontSize = 25.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }


            /*
             * =====================================================
             * BARRE DE SAISIE
             * =====================================================
             */

            Row(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            surfaceColor
                        )
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
                 * +
                 */

                IconButton(

                    onClick = {

                        showAttachmentMenu =
                            true
                    }
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Add,

                        contentDescription =
                            "Ajouter une pièce jointe",

                        tint =
                            onSurfaceColor
                    )
                }


                /*
                 * SIM
                 */

                Box(

                    modifier =
                        Modifier
                            .size(42.dp)
                            .clickable {

                                if (
                                    ContextCompat
                                        .checkSelfPermission(
                                            context,
                                            Manifest.permission
                                                .READ_PHONE_STATE
                                        ) ==
                                    PackageManager
                                        .PERMISSION_GRANTED
                                ) {

                                    loadSimCards()

                                    showSimDialog =
                                        true

                                } else {

                                    simPermissionLauncher
                                        .launch(
                                            Manifest.permission
                                                .READ_PHONE_STATE
                                        )
                                }
                            },

                    contentAlignment =
                        Alignment.Center
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.SimCard,

                        contentDescription =
                            "SIM",

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
                                    .background(
                                        SendGreen
                                    )
                                    .align(
                                        Alignment.TopEnd
                                    ),

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
                                    9.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }
                }


                /*
                 * CHAMP MESSAGE
                 */

                Box(

                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(
                                RoundedCornerShape(24.dp)
                            )
                            .background(
                                inputColor
                            )
                            .padding(
                                horizontal = 15.dp,
                                vertical = 10.dp
                            )
                ) {

                    BasicTextField(

                        value =
                            message,

                        onValueChange = {
                            message = it
                        },

                        textStyle =
                            TextStyle(
                                color =
                                    onSurfaceColor,
                                fontSize =
                                    16.sp
                            ),

                        cursorBrush =
                            SolidColor(
                                SendGreen
                            ),

                        maxLines = 5,

                        modifier =
                            Modifier.fillMaxWidth(),

                        decorationBox = {
                                innerTextField ->

                            if (
                                message.isEmpty()
                            ) {

                                Text(

                                    text =
                                        "Message",

                                    color =
                                        secondaryTextColor,

                                    fontSize =
                                        16.sp
                                )
                            }

                            innerTextField()
                        }
                    )
                }


                /*
                 * EMOJI / CLAVIER
                 */

                IconButton(

                    onClick = {

                        if (showEmojiPicker) {

                            showEmojiPicker =
                                false

                            keyboardController
                                ?.show()

                        } else {

                            keyboardController
                                ?.hide()

                            showEmojiPicker =
                                true
                        }
                    }
                ) {

                    Icon(

                        imageVector =

                            if (showEmojiPicker) {

                                Icons.Default.Keyboard

                            } else {

                                Icons.Default.EmojiEmotions
                            },

                        contentDescription =

                            if (showEmojiPicker) {

                                "Clavier"

                            } else {

                                "Emojis"
                            },

                        tint =
                            onSurfaceColor
                    )
                }


                /*
                 * =================================================
                 * ENVOYER + SAUVEGARDER DANS ROOM
                 * =================================================
                 */

                IconButton(

                    onClick = {

                        if (
                            phoneNumber.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Veuillez saisir un numéro.",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@IconButton
                        }

                        if (
                            message.isBlank()
                        ) {

                            Toast.makeText(
                                context,
                                "Veuillez saisir un message.",
                                Toast.LENGTH_SHORT
                            ).show()

                            return@IconButton
                        }

                        val numberToSend =
                            phoneNumber.trim()

                        val messageToSend =
                            message.trim()

                        val sent =
                            sendSmsDirectly(

                                context =
                                    context,

                                phoneNumber =
                                    numberToSend,

                                message =
                                    messageToSend,

                                selectedSim =
                                    selectedSim
                            )

                        if (sent) {

                            coroutineScope.launch {

                                try {

                                    messageDao.insertMessage(

                                        MessageEntity(

                                            phoneNumber =
                                                numberToSend,

                                            contactName =
                                                selectedContactName,

                                            message =
                                                messageToSend,

                                            timestamp =
                                                System.currentTimeMillis(),

                                            type =
                                                "SENT"
                                        )
                                    )

                                    Toast.makeText(
                                        context,
                                        "Message enregistré.",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    message = ""

                                    onSendClick()

                                } catch (e: Exception) {

                                    Toast.makeText(
                                        context,
                                        "SMS envoyé, mais impossible d'enregistrer le message.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                ) {

                    Box(

                        modifier =
                            Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(

                                    if (
                                        phoneNumber.isNotBlank() &&
                                        message.isNotBlank()
                                    ) {

                                        SendGreen

                                    } else {

                                        colorScheme
                                            .surfaceVariant
                                    }
                                ),

                        contentAlignment =
                            Alignment.Center
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Send,

                            contentDescription =
                                "Envoyer",

                            tint =

                                if (
                                    phoneNumber.isNotBlank() &&
                                    message.isNotBlank()
                                ) {

                                    Color.White

                                } else {

                                    colorScheme
                                        .onSurfaceVariant
                                }
                        )
                    }
                }
            }
        }
    }


    /*
     * ========================================================
     * MENU PIÈCES JOINTES
     * ========================================================
     */

    if (showAttachmentMenu) {

        ModalBottomSheet(

            onDismissRequest = {

                showAttachmentMenu =
                    false
            },

            sheetState =
                rememberModalBottomSheetState(
                    skipPartiallyExpanded = true
                ),

            containerColor =
                surfaceColor,

            tonalElevation = 0.dp
        ) {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 24.dp
                        )
            ) {

                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "Ajouter une pièce jointe",

                            color =
                                onSurfaceColor,

                            fontSize =
                                19.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(

                            text =
                                "Choisis une source pour ton fichier",

                            color =
                                secondaryTextColor,

                            fontSize =
                                13.sp
                        )
                    }

                    IconButton(

                        onClick = {

                            showAttachmentMenu =
                                false
                        }
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Close,

                            contentDescription =
                                "Fermer",

                            tint =
                                onSurfaceColor
                        )
                    }
                }


                Spacer(
                    modifier =
                        Modifier.height(24.dp)
                )


                Row(

                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    AttachmentOption(

                        modifier =
                            Modifier.weight(1f),

                        icon = {

                            Icon(

                                imageVector =
                                    Icons.Default.InsertDriveFile,

                                contentDescription =
                                    null,

                                tint =
                                    SendGreen,

                                modifier =
                                    Modifier.size(30.dp)
                            )
                        },

                        title =
                            "Fichier",

                        onClick = {

                            showAttachmentMenu =
                                false

                            filePickerLauncher.launch(
                                arrayOf("*/*")
                            )
                        }
                    )


                    AttachmentOption(

                        modifier =
                            Modifier.weight(1f),

                        icon = {

                            Icon(

                                imageVector =
                                    Icons.Default.Image,

                                contentDescription =
                                    null,

                                tint =
                                    SendGreen,

                                modifier =
                                    Modifier.size(30.dp)
                            )
                        },

                        title =
                            "Galerie",

                        onClick = {

                            showAttachmentMenu =
                                false

                            galleryPickerLauncher.launch(
                                "image/*"
                            )
                        }
                    )


                    AttachmentOption(

                        modifier =
                            Modifier.weight(1f),

                        icon = {

                            Icon(

                                imageVector =
                                    Icons.Default.CameraAlt,

                                contentDescription =
                                    null,

                                tint =
                                    SendGreen,

                                modifier =
                                    Modifier.size(30.dp)
                            )
                        },

                        title =
                            "Appareil photo",

                        onClick = {

                            showAttachmentMenu =
                                false

                            openCamera()
                        }
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                TextButton(

                    onClick = {

                        showAttachmentMenu =
                            false
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Text(

                        text =
                            "Annuler",

                        color =
                            SendGreen,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }


    /*
     * ========================================================
     * DIALOGUE SIM
     * ========================================================
     */

    if (showSimDialog) {

        AlertDialog(

            onDismissRequest = {

                showSimDialog =
                    false
            },

            title = {

                Text(

                    text =
                        "Choisir une SIM",

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                if (simList.isEmpty()) {

                    Text(

                        text =
                            "Aucune SIM disponible. Vérifie que ta carte SIM est bien insérée et que la permission est autorisée."
                    )

                } else {

                    Column {

                        simList.forEach { sim ->

                            Row(

                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {

                                            selectedSim =
                                                sim

                                            showSimDialog =
                                                false
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
                                                ?: "Opérateur",

                                        color =
                                            onSurfaceColor,

                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(

                                        text =
                                            "SIM ${
                                                sim.simSlotIndex + 1
                                            }",

                                        color =
                                            secondaryTextColor,

                                        fontSize =
                                            13.sp
                                    )
                                }
                            }

                            androidx.compose.material3.HorizontalDivider()
                        }
                    }
                }
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        showSimDialog =
                            false
                    }
                ) {

                    Text(

                        text =
                            "Fermer",

                        color =
                            SendGreen
                    )
                }
            }
        )
    }
}


/*
 * ============================================================
 * RECHERCHE DES CONTACTS DANS LE TÉLÉPHONE
 * ============================================================
 */

private fun searchContacts(
    context: Context,
    searchText: String
): List<ContactSuggestion> {

    val results =
        mutableListOf<ContactSuggestion>()

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

    val cursor =
        try {

            context.contentResolver.query(

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .CONTENT_URI,

                projection,

                null,

                null,

                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .DISPLAY_NAME + " ASC"
            )

        } catch (e: SecurityException) {

            null
        }

    cursor?.use {

        val nameIndex =
            it.getColumnIndex(
                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .DISPLAY_NAME
            )

        val numberIndex =
            it.getColumnIndex(
                ContactsContract
                    .CommonDataKinds
                    .Phone
                    .NUMBER
            )

        while (
            it.moveToNext() &&
            results.size < 8
        ) {

            val name =
                if (nameIndex >= 0) {

                    it.getString(
                        nameIndex
                    )

                } else {

                    null
                }

            val number =
                if (numberIndex >= 0) {

                    it.getString(
                        numberIndex
                    )

                } else {

                    null
                }

            if (
                !name.isNullOrBlank() &&
                !number.isNullOrBlank()
            ) {

                val search =
                    searchText.lowercase(
                        Locale.getDefault()
                    )

                val nameMatches =
                    name.lowercase(
                        Locale.getDefault()
                    ).contains(search)

                val numberMatches =
                    number.contains(search)

                if (
                    nameMatches ||
                    numberMatches
                ) {

                    val alreadyExists =
                        results.any {
                            normalizePhoneNumber(
                                it.phoneNumber
                            ) ==
                                    normalizePhoneNumber(
                                        number
                                    )
                        }

                    if (!alreadyExists) {

                        results.add(
                            ContactSuggestion(
                                name = name,
                                phoneNumber = number
                            )
                        )
                    }
                }
            }
        }
    }

    return results
}


/*
 * ============================================================
 * NORMALISATION NUMÉRO
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
 * ENVOI DIRECT DU SMS
 * ============================================================
 */

private fun sendSmsDirectly(

    context: Context,

    phoneNumber: String,

    message: String,

    selectedSim: SubscriptionInfo?

): Boolean {

    val permissionGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

    if (!permissionGranted) {

        Toast.makeText(
            context,
            "L'autorisation SMS n'est pas activée.",
            Toast.LENGTH_LONG
        ).show()

        return false
    }

    return try {

        val smsManager =

            if (selectedSim != null) {

                SmsManager
                    .getSmsManagerForSubscriptionId(
                        selectedSim.subscriptionId
                    )

            } else {

                SmsManager.getDefault()
            }

        val parts =
            smsManager.divideMessage(
                message
            )

        if (parts.size <= 1) {

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

        Toast.makeText(
            context,
            "SMS envoyé avec succès.",
            Toast.LENGTH_SHORT
        ).show()

        true

    } catch (e: SecurityException) {

        Toast.makeText(
            context,
            "Permission SMS refusée.",
            Toast.LENGTH_LONG
        ).show()

        false

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "Échec de l'envoi : ${e.message ?: "erreur inconnue"}",
            Toast.LENGTH_LONG
        ).show()

        false
    }
}


/*
 * ============================================================
 * COMPOSANT : OPTION PIÈCE JOINTE
 * ============================================================
 */

@Composable
fun AttachmentOption(

    modifier: Modifier = Modifier,

    icon: @Composable () -> Unit,

    title: String,

    onClick: () -> Unit
) {

    val colorScheme =
        MaterialTheme.colorScheme

    Column(

        modifier =
            modifier
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .background(
                    colorScheme.surfaceVariant
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical = 18.dp,
                    horizontal = 8.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Box(

            modifier =
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        colorScheme
                            .secondaryContainer
                    ),

            contentAlignment =
                Alignment.Center
        ) {

            icon()
        }

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        Text(

            text =
                title,

            color =
                colorScheme.onSurfaceVariant,

            fontSize =
                13.sp,

            fontWeight =
                FontWeight.Medium
        )
    }
}


/*
 * ============================================================
 * PREVIEW
 * ============================================================
 */

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun NewMessageScreenPreview() {

    NewMessageScreen()
}