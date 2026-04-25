package com.faceguard.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class FaceGuardService : Service() {

    companion object {
        const val ACTION_SCAN     = "com.faceguard.ACTION_SCAN"
        const val CHANNEL_ID      = "faceguard_channel"
        const val NOTIFICATION_ID = 1
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var appHider: AppHider

    override fun onCreate() {
        super.onCreate()
        appHider = AppHider(this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d("FaceGuard", "Service started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SCAN) {
            serviceScope.launch { runScanAndApply() }
        }
        return START_STICKY
    }

    private suspend fun runScanAndApply() {
        Log.d("FaceGuard", "Running face scan...")
        appHider.applyStrangerMode()
        FaceGuardDatabase.saveLog(
            this,
            ActivityLog(
                personName = "Scanning...",
                result     = "SCANNING"
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FaceGuard Protection",
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "FaceGuard is protecting your privacy"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("FaceGuard")
            .setContentText("Protecting your privacy")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
            .build()
    }
}