package com.faceguard.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Profile::class, AppRule::class, ActivityLog::class],
    version = 2,
    exportSchema = false
)
abstract class FaceGuardDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun appRuleDao(): AppRuleDao
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: FaceGuardDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD COLUMN imagePath TEXT")
            }
        }

        fun getDatabase(context: Context): FaceGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceGuardDatabase::class.java,
                    "faceguard_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
