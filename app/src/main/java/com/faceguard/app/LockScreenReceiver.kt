package com.faceguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LockScreenReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("FaceGuard", "Lock screen appeared — starting face scan")

            // Start the FaceGuard service which handles the scan
            val serviceIntent = Intent(context, FaceGuardService::class.java)
            serviceIntent.action = FaceGuardService.ACTION_SCAN
            context.startForegroundService(serviceIntent)
        }
    }
}