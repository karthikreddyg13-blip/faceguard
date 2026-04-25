package com.faceguard.app

data class ActivityLog(
    val id: Int = 0,
    val profileId: Int? = null,
    val personName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val result: String,
    val intruderPhotoPath: String? = null,
    val googlePhotosUrl: String? = null
)