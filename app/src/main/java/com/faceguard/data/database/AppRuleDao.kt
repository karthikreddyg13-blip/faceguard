package com.faceguard.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(appRule: AppRule): Long

    @Update
    suspend fun update(appRule: AppRule)

    @Delete
    suspend fun delete(appRule: AppRule)

    @Query("SELECT * FROM app_rules")
    fun getAll(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE id = :id")
    suspend fun getById(id: Int): AppRule?

    @Query("SELECT * FROM app_rules WHERE profileId = :profileId")
    fun getByProfileId(profileId: Int): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName")
    suspend fun getByPackageName(packageName: String): AppRule?

    @Query("DELETE FROM app_rules WHERE profileId = :profileId")
    suspend fun deleteByProfileId(profileId: Int)

    @Query("DELETE FROM app_rules")
    suspend fun deleteAll()
}
