package com.smsplanner.app.ui.theme

import android.content.Context

object TextSizePreference {

    private const val PREFERENCES_NAME =
        "smsplanner_text_preferences"

    private const val TEXT_SIZE_KEY =
        "message_text_size"

    const val SMALL = 14f
    const val NORMAL = 16f
    const val LARGE = 20f
    const val EXTRA_LARGE = 24f

    const val MIN_SIZE = 12f
    const val MAX_SIZE = 32f

    fun getTextSize(
        context: Context
    ): Float {

        return context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getFloat(
                TEXT_SIZE_KEY,
                NORMAL
            )
            .coerceIn(
                MIN_SIZE,
                MAX_SIZE
            )
    }

    fun setTextSize(
        context: Context,
        size: Float
    ) {

        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putFloat(
                TEXT_SIZE_KEY,
                size.coerceIn(
                    MIN_SIZE,
                    MAX_SIZE
                )
            )
            .apply()
    }

}
