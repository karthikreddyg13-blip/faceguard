package com.faceguard.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val profileId: Int?,
    val profileName: String?,
    val timestamp: Long,
    val result: String,
    val intruderPhotoPath: String?
)
