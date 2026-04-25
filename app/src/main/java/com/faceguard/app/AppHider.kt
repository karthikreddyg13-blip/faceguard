package com.faceguard.app

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppHider(private val context: Context) {

    suspend fun applyProfile(profile: Profile) = withContext(Dispatchers.IO) {
        val rules = FaceGuardDatabase.getRulesForProfile(context, profile.id)
        rules.forEach { rule ->
            if (rule.shouldHide) hideApp(rule.packageName)
        }
        Log.d("AppHider", "Applied profile: ${profile.name} — ${rules.size} apps hidden")
    }

    suspend fun applyStrangerMode() = withContext(Dispatchers.IO) {
        val profiles = FaceGuardDatabase.getProfiles(context)
        val allHiddenPackages = mutableSetOf<String>()
        profiles.forEach { profile ->
            FaceGuardDatabase.getRulesForProfile(context, profile.id)
                .forEach { allHiddenPackages.add(it.packageName) }
        }
        allHiddenPackages.forEach { hideApp(it) }
        Log.d("AppHider", "Stranger mode — ${allHiddenPackages.size} apps hidden")
    }

    suspend fun restoreAllApps() = withContext(Dispatchers.IO) {
        val profiles = FaceGuardDatabase.getProfiles(context)
        val allPackages = mutableSetOf<String>()
        profiles.forEach { profile ->
            FaceGuardDatabase.getRulesForProfile(context, profile.id)
                .forEach { allPackages.add(it.packageName) }
        }
        allPackages.forEach { showApp(it) }
        Log.d("AppHider", "Owner detected — all apps restored")
    }

    private fun hideApp(packageName: String) {
        try {
            context.packageManager.setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                0
            )
        } catch (e: Exception) {
            Log.e("AppHider", "Could not hide $packageName", e)
        }
    }

    private fun showApp(packageName: String) {
        try {
            context.packageManager.setApplicationEnabledSetting(
                packageName,
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                0
            )
        } catch (e: Exception) {
            Log.e("AppHider", "Could not show $packageName", e)
        }
    }
}