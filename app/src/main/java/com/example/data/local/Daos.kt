package com.example.data.local

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserProfileById(userId: String): Flow<UserProfile?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserDirectById(userId: String): UserProfile?

    @Query("SELECT * FROM users LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUserDirect(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserProfile)
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM user_accounts WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAccountByEmail(email: String): UserAccount?

    @Query("SELECT * FROM user_accounts WHERE userId = :userId LIMIT 1")
    suspend fun getAccountById(userId: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: UserAccount)

    @Update
    suspend fun updateAccount(account: UserAccount)

    @Query("UPDATE user_accounts SET lastLoginTimestamp = :timestamp WHERE userId = :userId")
    suspend fun updateLastLogin(userId: String, timestamp: Long)

    @Query("UPDATE user_accounts SET resetCode = :code, resetCodeExpiry = :expiry WHERE LOWER(email) = LOWER(:email)")
    suspend fun setResetCode(email: String, code: String, expiry: Long)

    @Query("UPDATE user_accounts SET passwordHash = :newHash, passwordSalt = :newSalt, resetCode = NULL, resetCodeExpiry = 0 WHERE userId = :userId")
    suspend fun updatePassword(userId: String, newHash: String, newSalt: String)

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun getAccountCount(): Int
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM daily_progress WHERE date = :date LIMIT 1")
    fun getDailyProgress(date: String): Flow<DailyProgress?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDailyProgress(progress: DailyProgress)

    @Query("UPDATE daily_progress SET waterIntakeMl = waterIntakeMl + :amount WHERE date = :date")
    suspend fun incrementWater(date: String, amount: Int)

    @Query("UPDATE daily_progress SET waterIntakeMl = 0 WHERE date = :date")
    suspend fun resetWater(date: String)

    @Query("SELECT * FROM daily_progress WHERE date LIKE :userKeyPrefix || '%'")
    suspend fun getProgressForUserDirect(userKeyPrefix: String): List<DailyProgress>
}

@Dao
interface DailyGoalDao {
    @Query("SELECT * FROM daily_goals WHERE userId = :userId ORDER BY id ASC")
    fun getGoalsByUserId(userId: String): Flow<List<DailyGoalItem>>

    @Query("SELECT * FROM daily_goals WHERE userId = :userId ORDER BY id ASC")
    suspend fun getGoalsByUserIdDirect(userId: String): List<DailyGoalItem>

    @Query("SELECT * FROM daily_goals ORDER BY id ASC")
    fun getAllGoals(): Flow<List<DailyGoalItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: DailyGoalItem)

    @Update
    suspend fun updateGoal(goal: DailyGoalItem)

    @Delete
    suspend fun deleteGoal(goal: DailyGoalItem)

    @Query("DELETE FROM daily_goals WHERE userId = :userId")
    suspend fun clearGoalsByUserId(userId: String)

    @Query("DELETE FROM daily_goals")
    suspend fun clearAllGoals()
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY id DESC")
    fun getWeightLogsByUserId(userId: String): Flow<List<WeightLogEntry>>

    @Query("SELECT * FROM weight_logs ORDER BY id DESC")
    fun getAllWeightLogs(): Flow<List<WeightLogEntry>>

    @Query("SELECT * FROM weight_logs WHERE userId = :userId ORDER BY id ASC")
    suspend fun getWeightLogsByUserIdDirect(userId: String): List<WeightLogEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: WeightLogEntry)

    @Update
    suspend fun updateLog(entry: WeightLogEntry)

    @Delete
    suspend fun deleteLog(entry: WeightLogEntry)
}

@Dao
interface ActivityRecordDao {
    @Query("SELECT * FROM activity_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getRecordsByUserId(userId: String): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_history ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ActivityRecord>>

    @Query("SELECT * FROM activity_history WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getRecordsByUserIdDirect(userId: String): List<ActivityRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ActivityRecord)

    @Update
    suspend fun updateRecord(record: ActivityRecord)
}

@Dao
interface LoggedMealDao {
    @Query("SELECT * FROM logged_meals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMealsByUserId(userId: String): Flow<List<LoggedMeal>>

    @Query("SELECT * FROM logged_meals ORDER BY timestamp DESC")
    fun getAllLoggedMeals(): Flow<List<LoggedMeal>>

    @Query("SELECT * FROM logged_meals WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getMealsByUserIdDirect(userId: String): List<LoggedMeal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: LoggedMeal)

    @Update
    suspend fun updateMeal(meal: LoggedMeal)

    @Query("DELETE FROM logged_meals WHERE userId = :userId")
    suspend fun clearMealsByUserId(userId: String)

    @Query("DELETE FROM logged_meals")
    suspend fun clearMeals()
}
