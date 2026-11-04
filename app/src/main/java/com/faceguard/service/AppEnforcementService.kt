package com.faceguard.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.faceguard.data.database.AppRule
import com.faceguard.data.database.FaceGuardDatabase
import com.faceguard.data.database.Profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppEnforcementService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var currentPackage: String? = null
    private var isProcessingBlock = false

    companion object {
        private const val TAG = "AppEnforcementService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "========== AppEnforcementService CONNECTED ==========")
        Log.d(TAG, "Service is now active and will monitor app launches")
        Log.d(TAG, "Active profile from manager: ${ActiveProfileManager.activeProfile?.name ?: "NONE"}")
        Log.d(TAG, "Active profile ID from manager: ${ActiveProfileManager.activeProfile?.id ?: "NONE"}")
        Log.d(TAG, "Service instance: ${this.hashCode()}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        Log.d(TAG, "========== ACCESSIBILITY EVENT ==========")
        Log.d(TAG, "Event type: ${event.eventType}")
        Log.d(TAG, "Event class: ${event.className}")
        Log.d(TAG, "Package name: ${event.packageName}")

        // Process window state changes (app switches) and window content changes
        val isWindowEvent = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                           event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                           event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED

        if (!isWindowEvent) {
            Log.d(TAG, "✗ Event ignored: not a window event")
            return
        }

        val packageName = event.packageName?.toString() ?: run {
            Log.d(TAG, "✗ Event ignored: no package name")
            return
        }

        Log.d(TAG, "✓ Window event detected for package: $packageName")

        // Skip FaceGuard itself to avoid loops
        if (packageName == applicationContext.packageName) {
            Log.d(TAG, "✗ Event ignored: FaceGuard itself")
            return
        }

        // Skip if already processing a block
        if (isProcessingBlock) {
            Log.d(TAG, "✗ Event ignored: already processing block")
            return
        }

        // Skip if same package (debounce)
        if (currentPackage == packageName) {
            Log.d(TAG, "✗ Event ignored: same package (debounce)")
            return
        }

        currentPackage = packageName
        Log.d(TAG, "✓ App switch confirmed: $packageName")
        Log.d(TAG, "Active profile at event time: ${ActiveProfileManager.activeProfile?.name ?: "NONE"}")
        Log.d(TAG, "Active profile ID at event time: ${ActiveProfileManager.activeProfile?.id ?: "NONE"}")

        // Enforce rules
        Log.d(TAG, "Calling enforceAppRules for: $packageName")
        enforceAppRules(packageName)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AppEnforcementService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "AppEnforcementService destroyed")
    }

    private fun enforceAppRules(packageName: String) {
        Log.d(TAG, "========== ENFORCE APP RULES ==========")
        Log.d(TAG, "Package: $packageName")

        serviceScope.launch {
            try {
                val profile = ActiveProfileManager.activeProfile
                Log.d(TAG, "Active profile in enforceAppRules: ${profile?.name ?: "NONE"}")
                Log.d(TAG, "Active profile ID in enforceAppRules: ${profile?.id ?: "NONE"}")

                if (profile == null) {
                    Log.w(TAG, "✗ No active profile - allowing $packageName")
                    return@launch
                }

                Log.d(TAG, "✓ Active profile found: ${profile.name} (ID: ${profile.id})")

                val database = FaceGuardDatabase.getDatabase(applicationContext)
                Log.d(TAG, "Querying database for rule - Profile ID: ${profile.id}, Package: $packageName")

                val rule = try {
                    val result = database.appRuleDao().getByProfileIdAndPackageName(
                        profile.id, packageName
                    ).first()
                    Log.d(TAG, "✓ Rule found in database")
                    result
                } catch (e: Exception) {
                    Log.d(TAG, "✗ No rule found in database: ${e.message}")
                    null
                }

                if (rule == null) {
                    Log.d(TAG, "✗ Unconfigured app - allowing $packageName")
                    return@launch
                }

                Log.d(TAG, "Rule details:")
                Log.d(TAG, "  - App name: ${rule.appName}")
                Log.d(TAG, "  - Package name: ${rule.packageName}")
                Log.d(TAG, "  - Profile ID: ${rule.profileId}")
                Log.d(TAG, "  - Is allowed: ${rule.isAllowed}")

                if (rule.isAllowed) {
                    // Allowed - do nothing
                    Log.d(TAG, "✓ ALLOWED: ${rule.appName} for ${profile.name}")
                } else {
                    // Blocked - show block screen
                    Log.d(TAG, "✗ BLOCKED: ${rule.appName} for ${profile.name}")
                    Log.d(TAG, "Calling showBlockScreen...")
                    showBlockScreen(packageName, rule.appName, profile.name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Rule enforcement failed for $packageName", e)
            }
        }
    }

    private fun showBlockScreen(packageName: String, appName: String, profileName: String) {
        Log.d(TAG, "========== SHOW BLOCK SCREEN ==========")
        Log.d(TAG, "Package: $packageName")
        Log.d(TAG, "App name: $appName")
        Log.d(TAG, "Profile name: $profileName")
        Log.d(TAG, "Setting isProcessingBlock = true")

        isProcessingBlock = true

        try {
            Log.d(TAG, "Creating intent for BlockScreenActivity")
            val intent = Intent(this, com.faceguard.app.BlockScreenActivity::class.java).apply {
                putExtra(com.faceguard.app.BlockScreenActivity.EXTRA_PACKAGE_NAME, packageName)
                putExtra(com.faceguard.app.BlockScreenActivity.EXTRA_APP_NAME, appName)
                putExtra(com.faceguard.app.BlockScreenActivity.EXTRA_PROFILE_NAME, profileName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NO_HISTORY
            }
            Log.d(TAG, "Intent flags: NEW_TASK | CLEAR_TOP | NO_HISTORY")
            Log.d(TAG, "Starting BlockScreenActivity...")
            startActivity(intent)
            Log.d(TAG, "✓ startActivity called successfully")
        } catch (e: Exception) {
            Log.e(TAG, "✗ Failed to launch block screen", e)
        } finally {
            // Reset processing flag after a delay
            Log.d(TAG, "Scheduling isProcessingBlock reset in 2 seconds")
            serviceScope.launch {
                kotlinx.coroutines.delay(2000)
                isProcessingBlock = false
                Log.d(TAG, "Reset isProcessingBlock = false")
            }
        }
    }
}
