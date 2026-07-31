package com.faceguard.data.repository

import com.faceguard.data.database.ActivityLog
import com.faceguard.data.database.ActivityLogDao
import kotlinx.coroutines.flow.Flow

class ActivityLogRepository(private val activityLogDao: ActivityLogDao) {

    fun getAllLogs(): Flow<List<ActivityLog>> = activityLogDao.getAll()

    suspend fun getLogById(id: Int): ActivityLog? = activityLogDao.getById(id)

    suspend fun getLogsByProfileId(profileId: Int): Flow<List<ActivityLog>> = 
        activityLogDao.getByProfileId(profileId)

    suspend fun insertLog(activityLog: ActivityLog): Long = activityLogDao.insert(activityLog)

    suspend fun updateLog(activityLog: ActivityLog) = activityLogDao.update(activityLog)

    suspend fun deleteLog(activityLog: ActivityLog) = activityLogDao.delete(activityLog)

    suspend fun deleteLogsByProfileId(profileId: Int) = activityLogDao.deleteByProfileId(profileId)

    suspend fun deleteAllLogs() = activityLogDao.deleteAll()
}
