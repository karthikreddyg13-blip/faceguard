package com.faceguard.app

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object FaceGuardDatabase {

    private const val PREFS = "faceguard"
    private val gson = Gson()

    fun saveProfile(context: Context, profile: Profile): Int {
        val list = getProfiles(context).toMutableList()
        val id = (list.maxOfOrNull { it.id } ?: 0) + 1
        list.add(profile.copy(id = id))
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("profiles", gson.toJson(list)).apply()
        return id
    }

    fun getProfiles(context: Context): List<Profile> {
        val json = context.getSharedPreferences(PREFS, 0)
            .getString("profiles", "[]") ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<Profile>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun saveRule(context: Context, rule: AppRule) {
        val list = getRules(context).toMutableList()
        list.add(rule)
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("rules", gson.toJson(list)).apply()
    }

    fun getRulesForProfile(context: Context, profileId: Int): List<AppRule> {
        return getRules(context).filter { it.profileId == profileId }
    }

    private fun getRules(context: Context): List<AppRule> {
        val json = context.getSharedPreferences(PREFS, 0)
            .getString("rules", "[]") ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<AppRule>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }

    fun saveLog(context: Context, log: ActivityLog) {
        val list = getLogs(context).toMutableList()
        list.add(0, log)
        if (list.size > 100) list.removeLastOrNull()
        context.getSharedPreferences(PREFS, 0).edit()
            .putString("logs", gson.toJson(list)).apply()
    }

    fun getLogs(context: Context): List<ActivityLog> {
        val json = context.getSharedPreferences(PREFS, 0)
            .getString("logs", "[]") ?: "[]"
        return try {
            gson.fromJson(json, object : TypeToken<List<ActivityLog>>() {}.type)
        } catch (e: Exception) { emptyList() }
    }
}