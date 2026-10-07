package com.smsplanner.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*

* ============================================================
* ÉCRAN : PROGRAMMER UN SMS
* ============================================================
*
* Cet écran permet de :
*
* * vérifier le destinataire
* * vérifier le message
* * choisir une date
* * choisir une heure
* * confirmer la programmation
*
* Les couleurs sont entièrement gérées par MaterialTheme.
* L'écran suit donc automatiquement le mode clair/sombre
* du téléphone.
  */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgrammerMessageScreen(

    phoneNumber: String = "",

    message: String = "",

    onBackClick: () -> Unit = {},

    onConfirmSchedule: (
        phoneNumber: String,
        message: String,
        dateMillis: Long,
        hour: Int,
        minute: Int
    ) -> Unit = { _, _, _, _, _ -> }

) {

    /*
     * ========================================================
     * COULEURS DU THÈME
     * ========================================================
     */

    val colorScheme = MaterialTheme.colorScheme

    val primaryColor = colorScheme.primary

    val onPrimaryColor = colorScheme.onPrimary

    val disabledContainerColor =
        colorScheme.surfaceVariant

    val disabledContentColor =
        colorScheme.onSurfaceVariant


    /*
     * ========================================================
     * ÉTATS
     * ========================================================
     */

    /*
     * Affichage du calendrier.
     */
    var showDatePicker by remember {
        mutableStateOf(false)
    }

    /*
     * Affichage de l'horloge.
     */
    var showTimePicker by remember {
        mutableStateOf(false)
    }

    /*
     * Date réellement validée.
     */
    var selectedDateMillis by remember {
        mutableStateOf<Long?>(null)
    }

    /*
     * Heure réellement validée.
     */
    var selectedHour by remember {
        mutableStateOf(8)
    }

    var selectedMinute by remember {
        mutableStateOf(0)
    }

    /*
     * Indique si une heure a été validée.
     */
    var timeSelected by remember {
        mutableStateOf(false)
    }


    /*
     * ========================================================
     * ÉTAT DU CALENDRIER
     * ========================================================
     */
    val datePickerState = rememberDatePickerState()


    /*
     * ========================================================
     * ÉTAT DE L'HEURE
     * ========================================================
     */
    val timePickerState = rememberTimePickerState(
        initialHour = 8,
        initialMinute = 0,
        is24Hour = true
    )


    /*
     * ========================================================
     * TEXTE DE LA DATE
     * ========================================================
     */
    val selectedDateText =
        selectedDateMillis?.let { dateMillis ->

            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(
                Date(dateMillis)
            )

        } ?: "Aucune date sélectionnée"


    /*
     * ========================================================
     * TEXTE DE L'HEURE
     * ========================================================
     */
    val selectedTimeText =

        if (timeSelected) {

            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                selectedHour,
                selectedMinute
            )

        } else {

            "Aucune heure sélectionnée"
        }


    /*
     * ========================================================
     * INTERFACE PRINCIPALE
     * ========================================================
     */
    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        text = "Programmer un SMS"
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBackClick
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )
        }

    ) { innerPadding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),

            verticalArrangement = Arrangement.Top

        ) {

            /*
             * =================================================
             * DESTINATAIRE
             * =================================================
             */

            Text(
                text = "Destinataire",

                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =

                    if (phoneNumber.isBlank()) {

                        "Aucun numéro renseigné"

                    } else {

                        phoneNumber
                    },

                style = MaterialTheme.typography.bodyLarge,

                color = MaterialTheme.colorScheme.onSurface
            )


            Spacer(
                modifier = Modifier.height(20.dp)
            )


            /*
             * =================================================
             * MESSAGE
             * =================================================
             */

            Text(
                text = "Message",

                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =

                    if (message.isBlank()) {

                        "Aucun message renseigné"

                    } else {

                        message
                    },

                style = MaterialTheme.typography.bodyLarge,

                color = MaterialTheme.colorScheme.onSurface
            )


            Spacer(
                modifier = Modifier.height(28.dp)
            )


            /*
             * =================================================
             * DATE ET HEURE
             * =================================================
             */

            Text(
                text = "Date et heure d'envoi",

                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )


            /*
             * =================================================
             * BOUTON DATE
             * =================================================
             */

            OutlinedButton(

                onClick = {
                    showDatePicker = true
                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Icon(
                    imageVector = Icons.Default.CalendarMonth,

                    contentDescription = "Choisir une date"
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = selectedDateText
                )
            }


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            /*
             * =================================================
             * BOUTON HEURE
             * =================================================
             */

            OutlinedButton(

                onClick = {
                    showTimePicker = true
                },

                modifier = Modifier.fillMaxWidth()

            ) {

                Icon(
                    imageVector = Icons.Default.Schedule,

                    contentDescription = "Choisir une heure"
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = selectedTimeText
                )
            }


            Spacer(
                modifier = Modifier.height(28.dp)
            )


            /*
             * =================================================
             * CONFIRMER LA PROGRAMMATION
             * =================================================
             */
            Button(

                onClick = {

                    val date = selectedDateMillis

                    if (
                        date != null &&
                        timeSelected
                    ) {

                        onConfirmSchedule(
                            phoneNumber,
                            message,
                            date,
                            selectedHour,
                            selectedMinute
                        )
                    }
                },

                modifier = Modifier.fillMaxWidth(),

                enabled =
                    selectedDateMillis != null &&
                            timeSelected,

                colors = ButtonDefaults.buttonColors(

                    containerColor = primaryColor,

                    contentColor = onPrimaryColor,

                    disabledContainerColor =
                        disabledContainerColor,

                    disabledContentColor =
                        disabledContentColor
                )

            ) {

                Text(
                    text = "Confirmer la programmation"
                )
            }
        }
    }


    /*
     * ========================================================
     * FENÊTRE : DATE
     * ========================================================
     */
    if (showDatePicker) {

        Dialog(

            onDismissRequest = {
                showDatePicker = false
            }

        ) {

            Surface(
                shape = MaterialTheme.shapes.large
            ) {

                Column {

                    /*
                     * Calendrier.
                     */
                    DatePicker(

                        state = datePickerState,

                        showModeToggle = false
                    )


                    /*
                     * Bouton OK.
                     */
                    Row(

                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),

                        horizontalArrangement =
                            Arrangement.End

                    ) {

                        Button(

                            onClick = {

                                selectedDateMillis =
                                    datePickerState.selectedDateMillis

                                showDatePicker = false
                            },

                            enabled =
                                datePickerState.selectedDateMillis != null,

                            colors =
                                ButtonDefaults.buttonColors(

                                    containerColor =
                                        primaryColor,

                                    contentColor =
                                        onPrimaryColor
                                )

                        ) {

                            Text(
                                text = "OK"
                            )
                        }
                    }
                }
            }
        }
    }


    /*
     * ========================================================
     * FENÊTRE : HEURE
     * ========================================================
     */
    if (showTimePicker) {

        Dialog(

            onDismissRequest = {
                showTimePicker = false
            }

        ) {

            Surface(
                shape = MaterialTheme.shapes.large
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    /*
                     * Horloge.
                     */
                    TimePicker(
                        state = timePickerState
                    )


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    /*
                     * Bouton OK.
                     */
                    Row(

                        modifier = Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.End

                    ) {

                        Button(

                            onClick = {

                                selectedHour =
                                    timePickerState.hour

                                selectedMinute =
                                    timePickerState.minute

                                timeSelected = true

                                showTimePicker = false
                            },

                            colors =
                                ButtonDefaults.buttonColors(

                                    containerColor =
                                        primaryColor,

                                    contentColor =
                                        onPrimaryColor
                                )

                        ) {

                            Text(
                                text = "OK"
                            )
                        }
                    }
                }
            }
        }
    }

}
