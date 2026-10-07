package com.smsplanner.app.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.smsplanner.app.notification.NotificationHelper
import com.smsplanner.app.ui.theme.DeleteConfirmationPreference
import com.smsplanner.app.ui.theme.TextSizePreference
import com.smsplanner.app.ui.theme.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    onThemeChanged: (String) -> Unit = {},
    onAboutClick: () -> Unit = {}
) {

    val context = LocalContext.current

    // ========================================================
    // NOTIFICATIONS
    // ========================================================

    var notificationsEnabled by remember {

        mutableStateOf(
            NotificationHelper
                .areNotificationsEnabled(
                    context
                )
        )
    }

    // ========================================================
    // VIBRATION
    // ========================================================

    var vibrationEnabled by remember {

        mutableStateOf(
            NotificationHelper
                .isVibrationEnabled(
                    context
                )
        )
    }

    // ========================================================
    // APPARENCE
    // ========================================================

    var selectedTheme by remember {

        mutableStateOf(
            ThemePreference.getTheme(
                context
            )
        )
    }

    var appearanceDialogVisible by remember {

        mutableStateOf(false)
    }

    // ========================================================
    // TAILLE DU TEXTE
    // ========================================================

    var selectedTextSize by remember {

        mutableFloatStateOf(
            TextSizePreference.getTextSize(
                context
            )
        )
    }

    var textSizeDialogVisible by remember {

        mutableStateOf(false)
    }

    // ========================================================
    // CONFIRMATION DE SUPPRESSION
    // ========================================================

    var deleteConfirmationEnabled by remember {

        mutableStateOf(
            DeleteConfirmationPreference
                .isEnabled(
                    context
                )
        )
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Paramètres"
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

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        horizontal = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // ==================================================
            // GÉNÉRAL
            // ==================================================

            item {

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )

                SettingsSectionTitle(
                    title = "Général"
                )
            }

            // ==================================================
            // NOTIFICATIONS
            // ==================================================

            item {

                SettingsSwitchItem(

                    icon =
                        Icons.Default.Notifications,

                    title =
                        "Notifications",

                    description =
                        "Recevoir les notifications des nouveaux SMS",

                    checked =
                        notificationsEnabled,

                    onCheckedChange = { enabled ->

                        notificationsEnabled =
                            enabled

                        NotificationHelper
                            .setNotificationsEnabled(
                                context =
                                    context,

                                enabled =
                                    enabled
                            )
                    }
                )
            }

            // ==================================================
            // VIBRATION
            // ==================================================

            item {

                SettingsSwitchItem(

                    icon =
                        Icons.Default.Notifications,

                    title =
                        "Vibration",

                    description =
                        "Vibrer lors de la réception d'un SMS",

                    checked =
                        vibrationEnabled,

                    onCheckedChange = { enabled ->

                        vibrationEnabled =
                            enabled

                        NotificationHelper
                            .setVibrationEnabled(
                                context =
                                    context,

                                enabled =
                                    enabled
                            )
                    }
                )
            }

            // ==================================================
            // APPARENCE
            // ==================================================

            item {

                SettingsItem(

                    icon =
                        Icons.Default.DarkMode,

                    title =
                        "Apparence",

                    description =
                        getThemeDescription(
                            selectedTheme
                        ),

                    onClick = {

                        appearanceDialogVisible =
                            true
                    }
                )
            }

            // ==================================================
            // TAILLE DU TEXTE
            // ==================================================

            item {

                SettingsItem(

                    icon =
                        Icons.Default.TextFields,

                    title =
                        "Taille du texte",

                    description =
                        getTextSizeDescription(
                            selectedTextSize
                        ),

                    onClick = {

                        textSizeDialogVisible =
                            true
                    }
                )
            }

            // ==================================================
            // MESSAGES
            // ==================================================

            item {

                SettingsSectionTitle(
                    title =
                        "Messages"
                )
            }

            // ==================================================
            // CONFIRMATION DE SUPPRESSION
            // ==================================================

            item {

                SettingsSwitchItem(

                    icon =
                        Icons.Default.Delete,

                    title =
                        "Confirmation de suppression",

                    description =

                        if (
                            deleteConfirmationEnabled
                        ) {

                            "Demander une confirmation avant de supprimer"

                        } else {

                            "Supprimer directement sans confirmation"
                        },

                    checked =
                        deleteConfirmationEnabled,

                    onCheckedChange = { enabled ->

                        deleteConfirmationEnabled =
                            enabled

                        DeleteConfirmationPreference
                            .setEnabled(
                                context =
                                    context,

                                enabled =
                                    enabled
                            )
                    }
                )
            }

            // ==================================================
            // MESSAGES PROGRAMMÉS
            // ==================================================

            item {

                SettingsSectionTitle(
                    title =
                        "Messages programmés"
                )
            }

            item {

                SettingsItem(

                    icon =
                        Icons.Default.Schedule,

                    title =
                        "Messages programmés",

                    description =
                        "Gérer les options liées aux SMS programmés",

                    onClick = {

                        // La fonctionnalité est laissée
                        // de côté pour le moment.
                    }
                )
            }

            // ==================================================
            // À PROPOS
            // ==================================================

            item {

                SettingsSectionTitle(
                    title =
                        "À propos"
                )
            }

            item {

                SettingsItem(

                    icon =
                        Icons.Default.Info,

                    title =
                        "À propos de SMS Planner",

                    description =
                        "Informations sur l'application",

                    onClick = {

                        onAboutClick()
                    }
                )
            }

            // ==================================================
            // VERSION
            // ==================================================

            item {

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(

                    text =
                        "SMS Planner",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(

                    text =
                        "Version 1.0",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )
            }
        }
    }

    // ========================================================
    // DIALOGUE APPARENCE
    // ========================================================

    if (appearanceDialogVisible) {

        AlertDialog(

            onDismissRequest = {

                appearanceDialogVisible =
                    false
            },

            title = {

                Text(
                    text =
                        "Apparence"
                )
            },

            text = {

                Column {

                    // ==================================================
                    // CLAIR
                    // ==================================================

                    ThemeOption(

                        title =
                            "☀️  Clair",

                        description =
                            "Utiliser le thème clair",

                        selected =
                            selectedTheme ==
                                    ThemePreference.LIGHT,

                        onClick = {

                            selectedTheme =
                                ThemePreference.LIGHT

                            ThemePreference
                                .setTheme(
                                    context,
                                    ThemePreference.LIGHT
                                )

                            onThemeChanged(
                                ThemePreference.LIGHT
                            )

                            appearanceDialogVisible =
                                false
                        }
                    )

                    // ==================================================
                    // SOMBRE
                    // ==================================================

                    ThemeOption(

                        title =
                            "🌙  Sombre",

                        description =
                            "Utiliser le thème sombre",

                        selected =
                            selectedTheme ==
                                    ThemePreference.DARK,

                        onClick = {

                            selectedTheme =
                                ThemePreference.DARK

                            ThemePreference
                                .setTheme(
                                    context,
                                    ThemePreference.DARK
                                )

                            onThemeChanged(
                                ThemePreference.DARK
                            )

                            appearanceDialogVisible =
                                false
                        }
                    )

                    // ==================================================
                    // SYSTÈME
                    // ==================================================

                    ThemeOption(

                        title =
                            "⚙️  Système",

                        description =
                            "Suivre le thème du téléphone",

                        selected =
                            selectedTheme ==
                                    ThemePreference.SYSTEM,

                        onClick = {

                            selectedTheme =
                                ThemePreference.SYSTEM

                            ThemePreference
                                .setTheme(
                                    context,
                                    ThemePreference.SYSTEM
                                )

                            onThemeChanged(
                                ThemePreference.SYSTEM
                            )

                            appearanceDialogVisible =
                                false
                        }
                    )
                }
            },

            confirmButton = {}
        )
    }

    // ========================================================
    // DIALOGUE TAILLE DU TEXTE
    // ========================================================

    if (textSizeDialogVisible) {

        AlertDialog(

            onDismissRequest = {

                textSizeDialogVisible =
                    false
            },

            title = {

                Text(
                    text =
                        "Taille du texte"
                )
            },

            text = {

                Column {

                    // ==================================================
                    // PETIT
                    // ==================================================

                    TextSizeOption(

                        title =
                            "Petit",

                        description =
                            "14sp",

                        size =
                            TextSizePreference.SMALL,

                        selected =
                            selectedTextSize ==
                                    TextSizePreference.SMALL,

                        onClick = {

                            selectedTextSize =
                                TextSizePreference.SMALL

                            TextSizePreference
                                .setTextSize(
                                    context =
                                        context,

                                    size =
                                        TextSizePreference.SMALL
                                )

                            textSizeDialogVisible =
                                false
                        }
                    )

                    // ==================================================
                    // NORMAL
                    // ==================================================

                    TextSizeOption(

                        title =
                            "Normal",

                        description =
                            "16sp",

                        size =
                            TextSizePreference.NORMAL,

                        selected =
                            selectedTextSize ==
                                    TextSizePreference.NORMAL,

                        onClick = {

                            selectedTextSize =
                                TextSizePreference.NORMAL

                            TextSizePreference
                                .setTextSize(
                                    context =
                                        context,

                                    size =
                                        TextSizePreference.NORMAL
                                )

                            textSizeDialogVisible =
                                false
                        }
                    )

                    // ==================================================
                    // GRAND
                    // ==================================================

                    TextSizeOption(

                        title =
                            "Grand",

                        description =
                            "20sp",

                        size =
                            TextSizePreference.LARGE,

                        selected =
                            selectedTextSize ==
                                    TextSizePreference.LARGE,

                        onClick = {

                            selectedTextSize =
                                TextSizePreference.LARGE

                            TextSizePreference
                                .setTextSize(
                                    context =
                                        context,

                                    size =
                                        TextSizePreference.LARGE
                                )

                            textSizeDialogVisible =
                                false
                        }
                    )

                    // ==================================================
                    // TRÈS GRAND
                    // ==================================================

                    TextSizeOption(

                        title =
                            "Très grand",

                        description =
                            "24sp",

                        size =
                            TextSizePreference.EXTRA_LARGE,

                        selected =
                            selectedTextSize ==
                                    TextSizePreference.EXTRA_LARGE,

                        onClick = {

                            selectedTextSize =
                                TextSizePreference.EXTRA_LARGE

                            TextSizePreference
                                .setTextSize(
                                    context =
                                        context,

                                    size =
                                        TextSizePreference.EXTRA_LARGE
                                )

                            textSizeDialogVisible =
                                false
                        }
                    )
                }
            },

            confirmButton = {}
        )
    }
}

// ============================================================
// DESCRIPTION DU THÈME
// ============================================================

private fun getThemeDescription(
    theme: String
): String {

    return when (theme) {

        ThemePreference.LIGHT ->
            "Clair"

        ThemePreference.DARK ->
            "Sombre"

        else ->
            "Système"
    }
}

// ============================================================
// DESCRIPTION TAILLE DU TEXTE
// ============================================================

private fun getTextSizeDescription(
    size: Float
): String {

    return when {

        size <= 14f ->
            "Petit"

        size <= 16f ->
            "Normal"

        size <= 20f ->
            "Grand"

        else ->
            "Très grand"
    }
}

// ============================================================
// OPTION DE THÈME
// ============================================================

@Composable
private fun ThemeOption(

    title: String,

    description: String,

    selected: Boolean,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                )
                .clickable {
                    onClick()
                },

        colors =
            CardDefaults.cardColors(

                containerColor =

                    if (selected) {

                        MaterialTheme
                            .colorScheme
                            .primaryContainer

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        description,

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

            if (selected) {

                Text(

                    text =
                        "✓",

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}

// ============================================================
// OPTION TAILLE DU TEXTE
// ============================================================

@Composable
private fun TextSizeOption(

    title: String,

    description: String,

    size: Float,

    selected: Boolean,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 4.dp
                )
                .clickable {
                    onClick()
                },

        colors =
            CardDefaults.cardColors(

                containerColor =

                    if (selected) {

                        MaterialTheme
                            .colorScheme
                            .primaryContainer

                    } else {

                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    }
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(
                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        description,

                    fontSize =
                        size.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }

            if (selected) {

                Text(

                    text =
                        "✓",

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}

// ============================================================
// TITRE DE SECTION
// ============================================================

@Composable
private fun SettingsSectionTitle(
    title: String
) {

    Text(

        text =
            title,

        style =
            MaterialTheme
                .typography
                .titleMedium,

        color =
            MaterialTheme
                .colorScheme
                .primary,

        modifier =
            Modifier.padding(
                top = 8.dp,
                bottom = 4.dp
            )
    )
}

// ============================================================
// ÉLÉMENT DE PARAMÈTRE
// ============================================================

@Composable
private fun SettingsItem(

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    description: String,

    onClick: () -> Unit

) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(

                modifier =
                    Modifier.padding(
                        horizontal = 8.dp
                    )
            )

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(

                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        description,

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
        }
    }
}

// ============================================================
// ÉLÉMENT AVEC SWITCH
// ============================================================

@Composable
private fun SettingsSwitchItem(

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    description: String,

    checked: Boolean,

    onCheckedChange:
        (Boolean) -> Unit

) {

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

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(

                modifier =
                    Modifier.padding(
                        horizontal = 8.dp
                    )
            )

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                Text(

                    text =
                        title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium
                )

                Spacer(

                    modifier =
                        Modifier.height(3.dp)
                )

                Text(

                    text =
                        description,

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

            Switch(

                checked =
                    checked,

                onCheckedChange =
                    onCheckedChange
            )
        }
    }
}
