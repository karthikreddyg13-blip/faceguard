package com.faceguard.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faceguard.data.repository.AppRuleRepository

class AppRuleViewModelFactory(
    private val appRuleRepository: AppRuleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppRuleViewModel::class.java)) {
            return AppRuleViewModel(appRuleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
