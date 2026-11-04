package com.faceguard.data.repository

import com.faceguard.data.database.AppRule
import com.faceguard.data.database.AppRuleDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRuleRepository(private val appRuleDao: AppRuleDao) {

    fun getAll(): Flow<List<AppRule>> {
        return appRuleDao.getAll()
    }

    suspend fun getById(id: Int): AppRule? {
        return appRuleDao.getById(id)
    }

    fun getByProfileId(profileId: Int): Flow<List<AppRule>> {
        return appRuleDao.getByProfileId(profileId)
    }

    suspend fun getByPackageName(packageName: String): AppRule? {
        return appRuleDao.getByPackageName(packageName)
    }

    suspend fun insert(appRule: AppRule): Long {
        return appRuleDao.insert(appRule)
    }

    suspend fun update(appRule: AppRule) {
        appRuleDao.update(appRule)
    }

    suspend fun delete(appRule: AppRule) {
        appRuleDao.delete(appRule)
    }

    suspend fun deleteByProfileId(profileId: Int) {
        appRuleDao.deleteByProfileId(profileId)
    }

    suspend fun deleteAll() {
        appRuleDao.deleteAll()
    }

    suspend fun getSummary(profileId: Int): Pair<Int, Int> {
        val rules = appRuleDao.getByProfileId(profileId).first()
        val allowedCount = rules.count { it.isAllowed }
        val blockedCount = rules.count { !it.isAllowed }
        return Pair(allowedCount, blockedCount)
    }
}
