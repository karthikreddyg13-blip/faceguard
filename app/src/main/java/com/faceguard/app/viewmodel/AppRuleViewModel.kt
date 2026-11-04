package com.faceguard.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faceguard.data.database.AppRule
import com.faceguard.data.repository.AppRuleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppRuleViewModel(
    private val appRuleRepository: AppRuleRepository
) : ViewModel() {

    // App rules for a specific profile
    private val _appRules = MutableStateFlow<List<AppRule>>(emptyList())
    val appRules: StateFlow<List<AppRule>> = _appRules.asStateFlow()

    // Summary of allowed, blocked, and unconfigured apps
    private val _summary = MutableStateFlow(Triple(0, 0, 0))
    val summary: StateFlow<Triple<Int, Int, Int>> = _summary.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Load app rules for a specific profile
     */
    fun loadRulesForProfile(profileId: Int, totalInstalledApps: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First, get initial data immediately
                val initialRules = appRuleRepository.getByProfileId(profileId).first()
                _appRules.value = initialRules
                val allowedCount = initialRules.count { rule -> rule.isAllowed }
                val blockedCount = initialRules.count { rule -> !rule.isAllowed }
                val unconfiguredCount = totalInstalledApps - allowedCount - blockedCount
                _summary.value = Triple(allowedCount, blockedCount, unconfiguredCount)
                _isLoading.value = false
                
                // Then collect for updates
                appRuleRepository.getByProfileId(profileId).collect { rules ->
                    _appRules.value = rules
                    val newAllowedCount = rules.count { rule -> rule.isAllowed }
                    val newBlockedCount = rules.count { rule -> !rule.isAllowed }
                    val newUnconfiguredCount = totalInstalledApps - newAllowedCount - newBlockedCount
                    _summary.value = Triple(newAllowedCount, newBlockedCount, newUnconfiguredCount)
                }
            } catch (e: Exception) {
                Log.e("AppRuleViewModel", "Failed to load rules for profile $profileId", e)
                _errorMessage.value = "Failed to load app rules: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a new app rule
     */
    fun addRule(profileId: Int, packageName: String, appName: String, isAllowed: Boolean) {
        viewModelScope.launch {
            try {
                val rule = AppRule(
                    profileId = profileId,
                    packageName = packageName,
                    appName = appName,
                    isAllowed = isAllowed
                )
                appRuleRepository.insert(rule)
                Log.d("AppRuleViewModel", "Rule added: $appName for profile $profileId")
            } catch (e: Exception) {
                Log.e("AppRuleViewModel", "Failed to add rule", e)
                _errorMessage.value = "Failed to add rule: ${e.message}"
            }
        }
    }

    /**
     * Update an existing app rule
     */
    fun updateRule(rule: AppRule) {
        viewModelScope.launch {
            try {
                appRuleRepository.update(rule)
                Log.d("AppRuleViewModel", "Rule updated: ${rule.appName} allowed: ${rule.isAllowed}")
            } catch (e: Exception) {
                Log.e("AppRuleViewModel", "Failed to update rule", e)
                _errorMessage.value = "Failed to update rule: ${e.message}"
            }
        }
    }

    /**
     * Delete an app rule
     */
    fun deleteRule(rule: AppRule) {
        viewModelScope.launch {
            try {
                appRuleRepository.delete(rule)
                Log.d("AppRuleViewModel", "Rule deleted: ${rule.appName}")
            } catch (e: Exception) {
                Log.e("AppRuleViewModel", "Failed to delete rule", e)
                _errorMessage.value = "Failed to delete rule: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
