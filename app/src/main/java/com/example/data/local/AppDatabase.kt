package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*

@Database(
    entities = [
        UserProfile::class,
        UserAccount::class,
        DailyProgress::class,
        DailyGoalItem::class,
        WeightLogEntry::class,
        ActivityRecord::class,
        LoggedMeal::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun progressDao(): ProgressDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun activityRecordDao(): ActivityRecordDao
    abstract fun loggedMealDao(): LoggedMealDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructively add new cycle tracking columns while preserving all existing user logs
                db.execSQL("ALTER TABLE users ADD COLUMN cycleLengthDays INTEGER NOT NULL DEFAULT 28")
                db.execSQL("ALTER TABLE users ADD COLUMN lastPeriodStartTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructively create user_accounts authentication table with unique email index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `userId` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `passwordSalt` TEXT NOT NULL,
                        `createdAtTimestamp` INTEGER NOT NULL,
                        `accountStatus` TEXT NOT NULL,
                        `lastLoginTimestamp` INTEGER NOT NULL,
                        `resetCode` TEXT,
                        `resetCodeExpiry` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_accounts_email` ON `user_accounts` (`email`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Non-destructively add userId columns to isolate user data
                db.execSQL("ALTER TABLE daily_goals ADD COLUMN userId TEXT NOT NULL DEFAULT 'user_sarah_01'")
                db.execSQL("ALTER TABLE weight_logs ADD COLUMN userId TEXT NOT NULL DEFAULT 'user_sarah_01'")
                db.execSQL("ALTER TABLE activity_history ADD COLUMN userId TEXT NOT NULL DEFAULT 'user_sarah_01'")
                db.execSQL("ALTER TABLE logged_meals ADD COLUMN userId TEXT NOT NULL DEFAULT 'user_sarah_01'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "womanfit_database"
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
