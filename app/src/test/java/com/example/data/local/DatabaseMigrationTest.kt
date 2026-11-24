package com.example.data.local

import android.content.ContentValues
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private val dbName = "migration_test_db"

    @Test
    fun `migration from version 1 to 2 preserves user data and adds cycle tracking columns`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create version 1 schema of 'users' table
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `email` TEXT NOT NULL,
                            `avatarId` INTEGER NOT NULL,
                            `fitnessGoal` TEXT NOT NULL,
                            `fitnessLevel` TEXT NOT NULL,
                            `age` INTEGER NOT NULL,
                            `heightCm` REAL NOT NULL,
                            `currentWeightKg` REAL NOT NULL,
                            `targetWeightKg` REAL NOT NULL,
                            `dailyWaterGoalMl` INTEGER NOT NULL,
                            `dailyCalorieBurnGoal` INTEGER NOT NULL,
                            `soundEnabled` INTEGER NOT NULL,
                            `reminderNotifications` INTEGER NOT NULL,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )

                    // Create version 1 schema for weight logs
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `weight_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `weightKg` REAL NOT NULL,
                            `dateTimestamp` INTEGER NOT NULL,
                            `notes` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    // No-op for v1 setup
                }
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v1Db = helper.writableDatabase

        // 1. Insert existing user data in Version 1
        val userValues = ContentValues().apply {
            put("id", "user_sarah_01")
            put("name", "Sarah Miller")
            put("email", "sarah.miller@womanfit.app")
            put("avatarId", 1)
            put("fitnessGoal", "Tone & Sculpt")
            put("fitnessLevel", "Intermediate")
            put("age", 28)
            put("heightCm", 168.0f)
            put("currentWeightKg", 62.5f)
            put("targetWeightKg", 58.0f)
            put("dailyWaterGoalMl", 2500)
            put("dailyCalorieBurnGoal", 450)
            put("soundEnabled", 1)
            put("reminderNotifications", 1)
        }
        val userRowId = v1Db.insert("users", 0, userValues)
        assertTrue("User record should be inserted in v1", userRowId != -1L)

        // 2. Insert sample weight log in Version 1
        val weightValues = ContentValues().apply {
            put("weightKg", 62.5f)
            put("dateTimestamp", System.currentTimeMillis())
            put("notes", "Baseline measurement")
        }
        val weightRowId = v1Db.insert("weight_logs", 0, weightValues)
        assertTrue("Weight log should be inserted in v1", weightRowId != -1L)

        // 3. Execute Migration 1 -> 2
        AppDatabase.MIGRATION_1_2.migrate(v1Db)

        // 4. Verify all existing columns and user data are intact
        val cursor = v1Db.query("SELECT * FROM users WHERE id = 'user_sarah_01'")
        assertNotNull(cursor)
        assertTrue("User record should still exist post-migration", cursor.moveToFirst())

        val idIndex = cursor.getColumnIndexOrThrow("id")
        val nameIndex = cursor.getColumnIndexOrThrow("name")
        val weightIndex = cursor.getColumnIndexOrThrow("currentWeightKg")
        val targetWeightIndex = cursor.getColumnIndexOrThrow("targetWeightKg")
        val waterGoalIndex = cursor.getColumnIndexOrThrow("dailyWaterGoalMl")
        val cycleLengthIndex = cursor.getColumnIndexOrThrow("cycleLengthDays")
        val lastPeriodIndex = cursor.getColumnIndexOrThrow("lastPeriodStartTimestamp")

        assertEquals("user_sarah_01", cursor.getString(idIndex))
        assertEquals("Sarah Miller", cursor.getString(nameIndex))
        assertEquals(62.5f, cursor.getFloat(weightIndex), 0.01f)
        assertEquals(58.0f, cursor.getFloat(targetWeightIndex), 0.01f)
        assertEquals(2500, cursor.getInt(waterGoalIndex))

        // 5. Verify new columns have expected non-destructive defaults
        assertEquals("Default cycle length should be 28 days", 28, cursor.getInt(cycleLengthIndex))
        assertEquals("Default last period start should be 0 (prompt state)", 0L, cursor.getLong(lastPeriodIndex))

        cursor.close()

        // 6. Verify weight logs were completely untouched
        val weightCursor = v1Db.query("SELECT COUNT(*) FROM weight_logs")
        assertTrue(weightCursor.moveToFirst())
        assertEquals(1, weightCursor.getInt(0))
        weightCursor.close()

        v1Db.close()
    }

    @Test
    fun `migration from version 2 to 3 creates user_accounts table without altering existing data`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val v2DbName = "migration_test_db_v2"
        context.deleteDatabase(v2DbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(v2DbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `users` (
                            `id` TEXT NOT NULL,
                            `name` TEXT NOT NULL,
                            `email` TEXT NOT NULL,
                            `avatarId` INTEGER NOT NULL,
                            `fitnessGoal` TEXT NOT NULL,
                            `fitnessLevel` TEXT NOT NULL,
                            `age` INTEGER NOT NULL,
                            `heightCm` REAL NOT NULL,
                            `currentWeightKg` REAL NOT NULL,
                            `targetWeightKg` REAL NOT NULL,
                            `dailyWaterGoalMl` INTEGER NOT NULL,
                            `dailyCalorieBurnGoal` INTEGER NOT NULL,
                            `soundEnabled` INTEGER NOT NULL,
                            `reminderNotifications` INTEGER NOT NULL,
                            `cycleLengthDays` INTEGER NOT NULL DEFAULT 28,
                            `lastPeriodStartTimestamp` INTEGER NOT NULL DEFAULT 0,
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v2Db = helper.writableDatabase

        val userValues = ContentValues().apply {
            put("id", "user_sarah_01")
            put("name", "Sarah Miller")
            put("email", "sarah.miller@womanfit.app")
            put("avatarId", 1)
            put("fitnessGoal", "Tone & Sculpt")
            put("fitnessLevel", "Intermediate")
            put("age", 28)
            put("heightCm", 168.0f)
            put("currentWeightKg", 62.5f)
            put("targetWeightKg", 58.0f)
            put("dailyWaterGoalMl", 2500)
            put("dailyCalorieBurnGoal", 450)
            put("soundEnabled", 1)
            put("reminderNotifications", 1)
            put("cycleLengthDays", 28)
            put("lastPeriodStartTimestamp", 1000L)
        }
        v2Db.insert("users", 0, userValues)

        // Execute Migration 2 -> 3
        AppDatabase.MIGRATION_2_3.migrate(v2Db)

        // Verify users table is completely preserved
        val userCursor = v2Db.query("SELECT * FROM users WHERE id = 'user_sarah_01'")
        assertTrue(userCursor.moveToFirst())
        assertEquals("Sarah Miller", userCursor.getString(userCursor.getColumnIndexOrThrow("name")))
        userCursor.close()

        // Verify user_accounts table was created and supports unique email indexing
        val accountValues = ContentValues().apply {
            put("userId", "test_user_01")
            put("fullName", "Elena Woods")
            put("email", "elena@example.com")
            put("passwordHash", "hashed_pass")
            put("passwordSalt", "salt_123")
            put("createdAtTimestamp", System.currentTimeMillis())
            put("accountStatus", "ACTIVE")
            put("lastLoginTimestamp", 0L)
            put("resetCode", null as String?)
            put("resetCodeExpiry", 0L)
        }
        val rowId = v2Db.insert("user_accounts", 0, accountValues)
        assertTrue("Account record should be inserted in v3", rowId != -1L)

        val accountCursor = v2Db.query("SELECT * FROM user_accounts WHERE email = 'elena@example.com'")
        assertTrue(accountCursor.moveToFirst())
        assertEquals("Elena Woods", accountCursor.getString(accountCursor.getColumnIndexOrThrow("fullName")))
        assertEquals("ACTIVE", accountCursor.getString(accountCursor.getColumnIndexOrThrow("accountStatus")))
        accountCursor.close()

        v2Db.close()
    }

    @Test
    fun `migration from version 3 to 4 adds userId column to user data tables without data loss`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val v3DbName = "migration_test_db_v3"
        context.deleteDatabase(v3DbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(v3DbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `daily_goals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `category` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `weight_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `dateString` TEXT NOT NULL,
                            `weightKg` REAL NOT NULL,
                            `waistCm` REAL,
                            `hipsCm` REAL,
                            `note` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `activity_history` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `workoutTitle` TEXT NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            `caloriesBurned` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `logged_meals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `calories` INTEGER NOT NULL,
                            `protein` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // Insert row in v3
        val goalVal = ContentValues().apply {
            put("title", "Drink 2L Water")
            put("isCompleted", 1)
            put("category", "Hydration")
        }
        v3Db.insert("daily_goals", 0, goalVal)

        // Run Migration 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(v3Db)

        // Verify column added with default value
        val cursor = v3Db.query("SELECT * FROM daily_goals WHERE title = 'Drink 2L Water'")
        assertTrue(cursor.moveToFirst())
        val userColIndex = cursor.getColumnIndexOrThrow("userId")
        assertEquals("user_sarah_01", cursor.getString(userColIndex))
        cursor.close()

        v3Db.close()
    }

    @Test
    fun `migration from version 4 to 5 adds nullable remoteId for cloud sync without data loss`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val v4DbName = "migration_test_db_v4"
        context.deleteDatabase(v4DbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(v4DbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `daily_goals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `isCompleted` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `userId` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `weight_logs` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `dateString` TEXT NOT NULL,
                            `weightKg` REAL NOT NULL,
                            `waistCm` REAL,
                            `hipsCm` REAL,
                            `note` TEXT NOT NULL,
                            `userId` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `activity_history` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `workoutTitle` TEXT NOT NULL,
                            `durationMinutes` INTEGER NOT NULL,
                            `caloriesBurned` INTEGER NOT NULL,
                            `category` TEXT NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `userId` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `logged_meals` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `title` TEXT NOT NULL,
                            `category` TEXT NOT NULL,
                            `calories` INTEGER NOT NULL,
                            `protein` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `userId` TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v4Db = helper.writableDatabase

        val goalVal = ContentValues().apply {
            put("title", "Stretch 10 min")
            put("isCompleted", 0)
            put("category", "Flexibility")
            put("userId", "sb-user-1")
        }
        v4Db.insert("daily_goals", 0, goalVal)

        AppDatabase.MIGRATION_4_5.migrate(v4Db)

        listOf("daily_goals", "weight_logs", "activity_history", "logged_meals").forEach { table ->
            val info = v4Db.query("PRAGMA table_info(`$table`)")
            var hasRemoteId = false
            val nameIdx = info.getColumnIndexOrThrow("name")
            while (info.moveToNext()) {
                if (info.getString(nameIdx) == "remoteId") {
                    hasRemoteId = true
                    break
                }
            }
            info.close()
            assertTrue("$table should have remoteId column after 4->5 migration", hasRemoteId)
        }

        val cursor = v4Db.query("SELECT * FROM daily_goals WHERE title = 'Stretch 10 min'")
        assertTrue(cursor.moveToFirst())
        assertEquals("sb-user-1", cursor.getString(cursor.getColumnIndexOrThrow("userId")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("remoteId")))
        cursor.close()

        v4Db.close()
    }
}
