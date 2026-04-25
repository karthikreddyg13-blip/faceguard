package com.faceguard.app

data class AppRule(
    val id: Int = 0,
    val profileId: Int,
    val packageName: String,
    val appName: String,
    val shouldHide: Boolean = true
)