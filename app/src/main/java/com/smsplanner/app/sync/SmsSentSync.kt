package com.smsplanner.app.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log

import androidx.core.content.ContextCompat

import com.smsplanner.app.data.AppDatabase
import com.smsplanner.app.data.MessageEntity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class SmsSentSync(
    private val context: Context
) {

    private val handler =
        Handler(Looper.getMainLooper())

    private var observer: ContentObserver? = null


    // ============================================================
    // DÉMARRER LA SURVEILLANCE
    // ============================================================

    fun start() {

        if (observer != null) {
            return
        }


        // ========================================================
        // VÉRIFIER READ_SMS
        // ========================================================

        val readSmsGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED


        if (!readSmsGranted) {

            Log.e(
                TAG,
                "READ_SMS n'est PAS accordée. Synchronisation impossible."
            )

            return
        }


        observer =
            object : ContentObserver(handler) {

                override fun onChange(
                    selfChange: Boolean,
                    uri: Uri?
                ) {

                    super.onChange(
                        selfChange,
                        uri
                    )


                    Log.d(
                        TAG,
                        "Modification détectée dans les SMS : uri=$uri"
                    )


                    synchroniserDerniersSms()
                }
            }


        context.contentResolver.registerContentObserver(

            Telephony.Sms.CONTENT_URI,

            true,

            observer!!
        )


        Log.d(
            TAG,
            "Observateur SMS démarré"
        )


        // ========================================================
        // SYNCHRONISATION IMMÉDIATE
        // ========================================================

        synchroniserDerniersSms()


        Log.d(
            TAG,
            "Synchronisation des SMS envoyés démarrée"
        )
    }


    // ============================================================
    // ARRÊTER LA SURVEILLANCE
    // ============================================================

    fun stop() {

        observer?.let {

            context.contentResolver
                .unregisterContentObserver(it)
        }


        observer = null


        Log.d(
            TAG,
            "Synchronisation des SMS envoyés arrêtée"
        )
    }


    // ============================================================
    // SYNCHRONISER LES SMS ENVOYÉS
    // ============================================================

    fun synchroniserDerniersSms() {

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                // =================================================
                // VÉRIFIER READ_SMS
                // =================================================

                val readSmsGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_SMS
                    ) == PackageManager.PERMISSION_GRANTED


                if (!readSmsGranted) {

                    Log.e(
                        TAG,
                        "READ_SMS refusée. Impossible de lire les SMS envoyés."
                    )

                    return@launch
                }


                val database =
                    AppDatabase.getDatabase(
                        context.applicationContext
                    )


                val messageDao =
                    database.messageDao()


                // =================================================
                // PROJECTION
                // =================================================

                val projection = arrayOf(

                    Telephony.Sms._ID,

                    Telephony.Sms.ADDRESS,

                    Telephony.Sms.BODY,

                    Telephony.Sms.DATE
                )


                // =================================================
                // LIRE LES SMS ENVOYÉS
                // =================================================

                context.contentResolver.query(

                    Telephony.Sms.Sent.CONTENT_URI,

                    projection,

                    null,

                    null,

                    "${Telephony.Sms.DATE} DESC"

                )?.use { cursor ->


                    Log.d(
                        TAG,
                        "Nombre de SMS dans la boîte ENVOYÉS : ${cursor.count}"
                    )


                    val idColumn =
                        cursor.getColumnIndex(
                            Telephony.Sms._ID
                        )


                    val addressColumn =
                        cursor.getColumnIndex(
                            Telephony.Sms.ADDRESS
                        )


                    val bodyColumn =
                        cursor.getColumnIndex(
                            Telephony.Sms.BODY
                        )


                    val dateColumn =
                        cursor.getColumnIndex(
                            Telephony.Sms.DATE
                        )


                    if (
                        addressColumn == -1 ||
                        bodyColumn == -1 ||
                        dateColumn == -1
                    ) {

                        Log.e(
                            TAG,
                            "Colonnes SMS introuvables"
                        )

                        return@launch
                    }


                    // =================================================
                    // RÉCUPÉRER LES MESSAGES DE PLANNER
                    // =================================================

                    val messagesLocaux =
                        messageDao
                            .getAllMessages()
                            .first()


                    Log.d(
                        TAG,
                        "Messages actuellement présents dans Planner : ${messagesLocaux.size}"
                    )


                    var compteur = 0


                    // =================================================
                    // PARCOURIR LES SMS
                    // =================================================

                    while (
                        cursor.moveToNext()
                    ) {

                        val smsId =

                            if (
                                idColumn != -1
                            ) {

                                cursor.getLong(
                                    idColumn
                                )

                            } else {

                                -1L
                            }


                        val phoneNumber =
                            cursor.getString(
                                addressColumn
                            )
                                ?.trim()
                                ?: continue


                        val body =
                            cursor.getString(
                                bodyColumn
                            )
                                ?.trim()
                                ?: continue


                        val timestamp =
                            cursor.getLong(
                                dateColumn
                            )


                        if (
                            phoneNumber.isBlank() ||
                            body.isBlank()
                        ) {

                            continue
                        }


                        Log.d(
                            TAG,
                            "SMS trouvé : id=$smsId, numéro=$phoneNumber, message=$body, date=$timestamp"
                        )


                        // =================================================
                        // VÉRIFIER LE DOUBLON
                        // =================================================

                        val existeDeja =

                            messagesLocaux.any { local ->

                                local.type == "SENT" &&

                                        normaliserNumero(
                                            local.phoneNumber
                                        ) ==
                                        normaliserNumero(
                                            phoneNumber
                                        ) &&

                                        local.message.trim() ==
                                        body &&

                                        kotlin.math.abs(
                                            local.timestamp -
                                                    timestamp
                                        ) <=
                                        TIMESTAMP_TOLERANCE
                            }


                        if (
                            existeDeja
                        ) {

                            Log.d(
                                TAG,
                                "SMS déjà présent dans Planner : $phoneNumber"
                            )

                            continue
                        }


                        // =================================================
                        // IMPORTER LE SMS DANS PLANNER
                        // =================================================

                        val messageEntity =

                            MessageEntity(

                                phoneNumber =
                                    phoneNumber,

                                message =
                                    body,

                                timestamp =
                                    timestamp,

                                type =
                                    "SENT",

                                isRead =
                                    true,

                                status =
                                    "envoyé"
                            )


                        val insertedId =
                            messageDao.insertMessage(
                                messageEntity
                            )


                        compteur++


                        Log.d(
                            TAG,
                            "SMS externe IMPORTÉ dans Planner : " +
                                    "idRoom=$insertedId, " +
                                    "smsId=$smsId, " +
                                    "numéro=$phoneNumber"
                        )
                    }


                    Log.d(
                        TAG,
                        "Synchronisation terminée : $compteur SMS importé(s)"
                    )
                }

            } catch (
                exception: SecurityException
            ) {

                Log.e(
                    TAG,
                    "ERREUR : permission READ_SMS refusée par Android",
                    exception
                )

            } catch (
                exception: Exception
            ) {

                Log.e(
                    TAG,
                    "ERREUR de synchronisation des SMS envoyés",
                    exception
                )
            }
        }
    }


    // ============================================================
    // NORMALISER UN NUMÉRO
    // ============================================================

    private fun normaliserNumero(
        numero: String
    ): String {

        var resultat =
            numero
                .replace(
                    " ",
                    ""
                )
                .replace(
                    "-",
                    ""
                )
                .replace(
                    "(",
                    ""
                )
                .replace(
                    ")",
                    ""
                )
                .replace(
                    ".",
                    ""
                )


        if (
            resultat.startsWith("+225")
        ) {

            resultat =
                resultat.substring(4)

        } else if (
            resultat.startsWith("225") &&
            resultat.length > 10
        ) {

            resultat =
                resultat.substring(3)
        }


        return resultat
    }


    companion object {

        private const val TAG =
            "SMS_PLANNER_SYNC"

        private const val TIMESTAMP_TOLERANCE =
            10_000L
    }
}