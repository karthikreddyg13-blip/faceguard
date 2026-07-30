package com.faceguard.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activityLog: ActivityLog): Long

    @Update
    suspend fun update(activityLog: ActivityLog)

    @Delete
    suspend fun delete(activityLog: ActivityLog)

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE id = :id")
    suspend fun getById(id: Int): ActivityLog?

    @Query("SELECT * FROM activity_logs WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun getByProfileId(profileId: Int): Flow<List<ActivityLog>>

    @Query("SELECT * FROM activity_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getByTimeRange(startTime: Long, endTime: Long): Flow<List<ActivityLog>>

    @Query("DELETE FROM activity_logs WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Int)

    @Query("DELETE FROM activity_logs")
    suspend fun deleteAll()
}
