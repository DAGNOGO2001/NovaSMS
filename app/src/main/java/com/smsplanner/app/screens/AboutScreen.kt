package com.smsplanner.app.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import com.smsplanner.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBackClick: () -> Unit
) {

    val context = LocalContext.current

    // ============================================================
    // INFORMATIONS DE CONTACT
    // ============================================================

    val email = "tchadagnogo@gmail.com"

    val phone = "0709089755"

    val portfolio =
        "https://dagnogotchabouan.rf.gd/"


    // ============================================================
    // OUVRIR EMAIL
    // ============================================================

    fun openEmail() {

        val intent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse(
                "mailto:$email"
            )
        )

        context.startActivity(intent)
    }


    // ============================================================
    // OUVRIR WHATSAPP
    // ============================================================

    fun openWhatsApp() {

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://wa.me/225$phone"
            )
        )

        context.startActivity(intent)
    }


    // ============================================================
    // OUVRIR PORTFOLIO
    // ============================================================

    fun openPortfolio() {

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                portfolio
            )
        )

        context.startActivity(intent)
    }


    // ============================================================
    // INTERFACE
    // ============================================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "À propos"
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
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .background
                    )
            )
        }

    ) { innerPadding ->

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),

            color =
                MaterialTheme
                    .colorScheme
                    .background
        ) {

            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(16.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                // ====================================================
                // LOGO
                // ====================================================

                Image(

                    painter =
                        painterResource(
                            id =
                                R.drawable.ic_sms_planner
                        ),

                    contentDescription =
                        "Logo Nova SMS",

                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp),

                    contentScale =
                        ContentScale.Fit
                )


                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )


                // ====================================================
                // NOM DE L'APPLICATION
                // ====================================================

                Text(

                    text =
                        "NovaSMS",

                    style =
                        MaterialTheme
                            .typography
                            .headlineMedium,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(

                    text =
                        "Planifiez, envoyez et gérez vos SMS facilement.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    textAlign =
                        TextAlign.Center
                )


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                // ====================================================
                // VERSION
                // ====================================================

                Text(

                    text =
                        "Version 1.0.0",

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                // ====================================================
                // À PROPOS
                // ====================================================

                AboutSectionCard(

                    title =
                        "À propos de l'application"
                ) {

                    Text(

                        text =
                            "SMS Planner est une application Android permettant " +
                                    "de gérer facilement vos SMS, d'envoyer des messages " +
                                    "et de programmer leur envoi à une date et une heure précises.",

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
                        Modifier.height(12.dp)
                )


                // ====================================================
                // FONCTIONNALITÉS
                // ====================================================

                AboutSectionCard(

                    title =
                        "Fonctionnalités"
                ) {

                    AboutFeature(

                        icon =
                            Icons.Default.Send,

                        title =
                            "Envoi de SMS",

                        description =
                            "Envoyez rapidement des SMS à vos contacts."
                    )


                    AboutFeature(

                        icon =
                            Icons.Default.Update,

                        title =
                            "Programmation",

                        description =
                            "Programmez vos messages pour un envoi ultérieur."
                    )


                    AboutFeature(

                        icon =
                            Icons.Default.Smartphone,

                        title =
                            "Gestion des conversations",

                        description =
                            "Consultez et gérez vos conversations SMS."
                    )


                    AboutFeature(

                        icon =
                            Icons.Default.Storage,

                        title =
                            "Historique",

                        description =
                            "Retrouvez vos messages envoyés et programmés."
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // ====================================================
                // MESSAGES PROGRAMMÉS
                // ====================================================

                AboutSectionCard(

                    title =
                        "Messages programmés"
                ) {

                    Text(

                        text =
                            "SMS Planner vous permet de préparer vos messages à l'avance " +
                                    "et de choisir précisément la date et l'heure auxquelles ils " +
                                    "doivent être envoyés.",

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
                        Modifier.height(12.dp)
                )


                // ====================================================
                // CONFIDENTIALITÉ ET SÉCURITÉ
                // ====================================================

                AboutSectionCard(

                    title =
                        "Confidentialité et sécurité"
                ) {

                    AboutFeature(

                        icon =
                            Icons.Default.Security,

                        title =
                            "Données locales",

                        description =
                            "Les messages sont enregistrés localement sur votre appareil."
                    )


                    Spacer(
                        modifier =
                            Modifier.height(6.dp)
                    )


                    Text(

                        text =
                            "L'application utilise uniquement les autorisations nécessaires " +
                                    "à son fonctionnement, notamment pour l'envoi et la réception " +
                                    "des SMS.",

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
                        Modifier.height(12.dp)
                )


                // ====================================================
                // INFORMATIONS TECHNIQUES
                // ====================================================

                AboutSectionCard(

                    title =
                        "Informations techniques"
                ) {

                    AboutInfo(

                        icon =
                            Icons.Default.Smartphone,

                        label =
                            "Plateforme",

                        value =
                            "Android"
                    )


                    AboutInfo(

                        icon =
                            Icons.Default.Language,

                        label =
                            "Technologie",

                        value =
                            "Kotlin"
                    )


                    AboutInfo(

                        icon =
                            Icons.Default.Storage,

                        label =
                            "Interface",

                        value =
                            "Jetpack Compose"
                    )


                    AboutInfo(

                        icon =
                            Icons.Default.Update,

                        label =
                            "Version",

                        value =
                            "1.0.0"
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )


                // ====================================================
                // DÉVELOPPEUR
                // ====================================================

                AboutSectionCard(

                    title =
                        "Développeur"
                ) {

                    Text(

                        text =
                            "Tchabouan DAGNOGO",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    Text(

                        text =
                            "Développeur Web & Mobile",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary,

                        fontWeight =
                            FontWeight.Medium
                    )


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    Text(

                        text =
                            "Application développée avec Kotlin et Jetpack Compose.",

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
                        Modifier.height(12.dp)
                )


                // ====================================================
                // CONTACT
                // ====================================================

                AboutSectionCard(

                    title =
                        "Contact"
                ) {

                    Text(

                        text =
                            "Pour toute question, suggestion ou opportunité " +
                                    "professionnelle, vous pouvez me contacter via les moyens suivants :",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )


                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )


                    // ------------------------------------------------
                    // EMAIL
                    // ------------------------------------------------

                    ContactItem(

                        icon =
                            Icons.Default.Email,

                        title =
                            "Email",

                        value =
                            email,

                        onClick = {
                            openEmail()
                        }
                    )


                    // ------------------------------------------------
                    // WHATSAPP
                    // ------------------------------------------------

                    ContactItem(

                        icon =
                            Icons.Default.Phone,

                        title =
                            "Téléphone / WhatsApp",

                        value =
                            phone,

                        onClick = {
                            openWhatsApp()
                        }
                    )


                    // ------------------------------------------------
                    // PORTFOLIO
                    // ------------------------------------------------

                    ContactItem(

                        icon =
                            Icons.Default.Language,

                        title =
                            "Portfolio",

                        value =
                            portfolio,

                        onClick = {
                            openPortfolio()
                        }
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(20.dp)
                )


                // ====================================================
                // COPYRIGHT
                // ====================================================

                Text(

                    text =
                        "SMS Planner",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,

                    fontWeight =
                        FontWeight.Bold
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(

                    text =
                        "© 2026 Tchabouan DAGNOGO",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )


                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )
            }
        }
    }
}


// =====================================================================
// CARTE DE SECTION
// =====================================================================

@Composable
fun AboutSectionCard(

    title: String,

    content: @Composable () -> Unit

) {

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 2.dp
            )
    ) {

        Column(

            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(

                text =
                    title,

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


            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )


            content()
        }
    }
}


// =====================================================================
// FONCTIONNALITÉ
// =====================================================================

@Composable
fun AboutFeature(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    description: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                null,

            modifier =
                Modifier.size(28.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.size(12.dp)
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
                        .bodyLarge,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
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


// =====================================================================
// INFORMATIONS TECHNIQUES
// =====================================================================

@Composable
fun AboutInfo(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    label: String,

    value: String

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                null,

            modifier =
                Modifier.size(24.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.size(12.dp)
        )


        Text(

            text =
                label,

            modifier =
                Modifier.weight(1f),

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
                value,

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            fontWeight =
                FontWeight.Medium
        )
    }
}


// =====================================================================
// CONTACT
// =====================================================================

@Composable
fun ContactItem(

    icon: androidx.compose.ui.graphics.vector.ImageVector,

    title: String,

    value: String,

    onClick: () -> Unit

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .padding(vertical = 9.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(

            imageVector =
                icon,

            contentDescription =
                title,

            modifier =
                Modifier.size(27.dp),

            tint =
                MaterialTheme
                    .colorScheme
                    .primary
        )


        Spacer(
            modifier =
                Modifier.size(12.dp)
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
                        .bodyMedium,

                fontWeight =
                    FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(2.dp)
            )


            Text(

                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall,

                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}