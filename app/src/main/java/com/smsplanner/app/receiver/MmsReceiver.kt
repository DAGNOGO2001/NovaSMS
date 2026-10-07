package com.smsplanner.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d(
            "SMS_PLANNER",
            "Un MMS a été reçu"
        )
    }
}