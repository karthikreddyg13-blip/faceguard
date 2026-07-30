package com.faceguard.data.repository

import com.faceguard.data.database.Profile
import com.faceguard.data.database.ProfileDao
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {

    fun getAllProfiles(): Flow<List<Profile>> = profileDao.getAll()

    suspend fun getProfileById(id: Int): Profile? = profileDao.getById(id)

    suspend fun getOwner(): Profile? = profileDao.getOwner()

    suspend fun insertProfile(profile: Profile): Long = profileDao.insert(profile)

    suspend fun updateProfile(profile: Profile) = profileDao.update(profile)

    suspend fun deleteProfile(profile: Profile) = profileDao.delete(profile)

    suspend fun deleteAllProfiles() = profileDao.deleteAll()
}
