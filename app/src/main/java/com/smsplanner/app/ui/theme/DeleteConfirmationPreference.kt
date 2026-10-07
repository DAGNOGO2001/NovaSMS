package com.smsplanner.app.ui.theme

import android.content.Context

object DeleteConfirmationPreference {

    private const val PREFERENCES_NAME =
        "smsplanner_delete_preferences"

    private const val DELETE_CONFIRMATION_KEY =
        "delete_confirmation_enabled"

    fun isEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                DELETE_CONFIRMATION_KEY,
                true
            )
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                DELETE_CONFIRMATION_KEY,
                enabled
            )
            .apply()
    }
}