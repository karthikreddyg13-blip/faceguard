package com.faceguard.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Profile::class, AppRule::class, ActivityLog::class],
    version = 4,
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profiles ADD COLUMN imagePath TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add profileName column to activity_logs table
                db.execSQL("ALTER TABLE activity_logs ADD COLUMN profileName TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add appName column to app_rules table
                db.execSQL("ALTER TABLE app_rules ADD COLUMN appName TEXT DEFAULT ''")
                // Rename shouldHide to isAllowed
                db.execSQL("ALTER TABLE app_rules ADD COLUMN isAllowed INTEGER DEFAULT 1 NOT NULL")
                // Migrate data: shouldHide true -> isAllowed false, shouldHide false -> isAllowed true
                db.execSQL("UPDATE app_rules SET isAllowed = CASE WHEN shouldHide = 1 THEN 0 ELSE 1 END")
                // Drop old column (SQLite doesn't support DROP COLUMN directly, recreate table)
                db.execSQL("""
                    CREATE TABLE app_rules_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profileId INTEGER NOT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        isAllowed INTEGER NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO app_rules_new (id, profileId, packageName, appName, isAllowed)
                    SELECT id, profileId, packageName, appName, isAllowed FROM app_rules
                """)
                db.execSQL("DROP TABLE app_rules")
                db.execSQL("ALTER TABLE app_rules_new RENAME TO app_rules")
            }
        }

        fun getDatabase(context: Context): FaceGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FaceGuardDatabase::class.java,
                    "faceguard_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
