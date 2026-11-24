package com.example.data

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.remote.WomenFitAiCloudClient
import com.example.data.remote.WomenFitAiCloudClient.Companion.TABLE_DAILY_GOALS
import com.example.data.remote.WomenFitAiCloudClient.Companion.TABLE_DAILY_PROGRESS
import com.example.data.remote.WomenFitAiCloudClient.Companion.TABLE_MEAL_LOGS
import com.example.data.remote.WomenFitAiCloudClient.Companion.TABLE_WEIGHT_LOGS
import com.example.data.remote.WomenFitAiCloudClient.Companion.TABLE_WORKOUT_LOGS
import com.example.model.ActivityRecord
import com.example.model.DailyGoalItem
import com.example.model.DailyProgress
import com.example.model.LoggedMeal
import com.example.model.WeightLogEntry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Keeps the local Room database (used by the UI, works offline) in sync with this app's
 * Supabase tables on the shared WomenFit server:
 *
 *   daily_progress   <-> womenfit_ai_daily_progress  (one row per user per day)
 *   activity_history <-> womenfit_ai_workout_logs
 *   weight_logs      <-> womenfit_ai_weight_logs
 *   logged_meals     <-> womenfit_ai_meal_logs
 *   daily_goals      <-> womenfit_ai_daily_goals
 *
 * (The profile itself syncs to womenfit_ai_users through SupabaseAuthRepository.)
 *
 * - Every new local row gets a client-generated uuid (remoteId) and is pushed right away.
 * - [syncAll] runs on sign-in / app start: uploads all local rows (filling in uuids for
 *   older rows), then downloads rows that exist only on the server (new phone, reinstall).
 * - All failures are logged and swallowed; Room stays the source of truth for the UI and
 *   anything missed is uploaded again on the next [syncAll].
 */
class FitnessCloudSync(
    private val database: AppDatabase,
    private val client: WomenFitAiCloudClient,
    /** Goal deletions not yet confirmed by the server (made offline); retried on each sync. */
    private val pendingGoalDeletes: PendingDeleteStore = PendingDeleteStore.InMemory()
) {
    /** Tiny persistent set of remote ids; SharedPreferences in the app, in-memory in tests. */
    interface PendingDeleteStore {
        fun all(): Set<String>
        fun add(id: String)
        fun remove(id: String)

        class InMemory : PendingDeleteStore {
            private val ids = java.util.Collections.synchronizedSet(mutableSetOf<String>())
            override fun all(): Set<String> = synchronized(ids) { ids.toSet() }
            override fun add(id: String) { ids.add(id) }
            override fun remove(id: String) { ids.remove(id) }
        }

        class Prefs(private val prefs: android.content.SharedPreferences) : PendingDeleteStore {
            private val key = "pending_goal_deletes"
            @Synchronized override fun all(): Set<String> = prefs.getStringSet(key, emptySet())!!.toSet()
            @Synchronized override fun add(id: String) { prefs.edit().putStringSet(key, all() + id).apply() }
            @Synchronized override fun remove(id: String) { prefs.edit().putStringSet(key, all() - id).apply() }
        }
    }

    companion object {
        private const val TAG = "FitnessCloudSync"
        private val ISO_DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        fun newRemoteId(): String = UUID.randomUUID().toString()
    }

    private val progressDao = database.progressDao()
    private val goalDao = database.dailyGoalDao()
    private val weightDao = database.weightLogDao()
    private val activityDao = database.activityRecordDao()
    private val mealDao = database.loggedMealDao()

    private val syncMutex = Mutex()

    // -------------------------------------------------------------
    // Single-item pushes (called right after each local write)
    // -------------------------------------------------------------

    suspend fun pushDailyProgress(userId: String, progress: DailyProgress) {
        val row = progressToJson(userId, progress) ?: return
        client.upsertRows(TABLE_DAILY_PROGRESS, JSONArray().put(row), onConflict = "user_id,log_date")
    }

    suspend fun pushWorkout(record: ActivityRecord) {
        val row = workoutToJson(record) ?: return
        client.upsertRows(TABLE_WORKOUT_LOGS, JSONArray().put(row))
    }

    suspend fun pushWeight(entry: WeightLogEntry) {
        val row = weightToJson(entry) ?: return
        client.upsertRows(TABLE_WEIGHT_LOGS, JSONArray().put(row))
    }

    suspend fun pushMeal(meal: LoggedMeal) {
        val row = mealToJson(meal) ?: return
        client.upsertRows(TABLE_MEAL_LOGS, JSONArray().put(row))
    }

    suspend fun pushGoal(goal: DailyGoalItem) {
        val row = goalToJson(goal) ?: return
        client.upsertRows(TABLE_DAILY_GOALS, JSONArray().put(row))
    }

    suspend fun deleteGoal(goal: DailyGoalItem) {
        val id = goal.remoteId ?: return
        // Remember it first so an offline delete is retried (and never re-downloaded) later.
        pendingGoalDeletes.add(id)
        if (markGoalDeleted(id)) pendingGoalDeletes.remove(id)
    }

    /** Soft-delete on the server (is_deleted = true) so every phone drops the goal. */
    private suspend fun markGoalDeleted(id: String): Boolean =
        client.updateRow(TABLE_DAILY_GOALS, id, JSONObject().put("is_deleted", true))

    // -------------------------------------------------------------
    // Full two-way sync
    // -------------------------------------------------------------

    suspend fun syncAll(userId: String) {
        if (userId.isBlank() || !client.isConfigured()) return
        syncMutex.withLock {
            try {
                syncDailyProgress(userId)
                syncWorkouts(userId)
                syncWeights(userId)
                syncMeals(userId)
                syncGoals(userId)
                Log.i(TAG, "Cloud sync finished")
            } catch (e: Exception) {
                Log.w(TAG, "Cloud sync stopped early: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private suspend fun syncDailyProgress(userId: String) {
        val prefix = "${userId}_"
        val local = progressDao.getProgressForUserDirect(prefix)
        val upload = JSONArray()
        local.mapNotNull { progressToJson(userId, it) }.forEach { upload.put(it) }
        client.upsertRows(TABLE_DAILY_PROGRESS, upload, onConflict = "user_id,log_date")

        val localKeys = local.map { it.date }.toHashSet()
        val remote = client.fetchRows(TABLE_DAILY_PROGRESS, userId) ?: return
        for (i in 0 until remote.length()) {
            val obj = remote.getJSONObject(i)
            val key = prefix + obj.optString("log_date")
            if (key in localKeys) continue
            progressDao.saveDailyProgress(
                DailyProgress(
                    date = key,
                    waterIntakeMl = obj.optInt("water_intake_ml", 0),
                    caloriesBurned = obj.optInt("calories_burned", 0),
                    activeMinutes = obj.optInt("active_minutes", 0),
                    completedWorkoutsCount = obj.optInt("completed_workouts_count", 0),
                    streakDays = obj.optInt("streak_days", 1)
                )
            )
        }
    }

    private suspend fun syncWorkouts(userId: String) {
        val local = activityDao.getRecordsByUserIdDirect(userId).map { record ->
            if (record.remoteId == null) {
                record.copy(remoteId = newRemoteId()).also { activityDao.updateRecord(it) }
            } else record
        }
        client.upsertRows(TABLE_WORKOUT_LOGS, JSONArray().apply {
            local.mapNotNull { workoutToJson(it) }.forEach { put(it) }
        })

        val known = local.mapNotNull { it.remoteId }.toHashSet()
        val remote = client.fetchRows(TABLE_WORKOUT_LOGS, userId, order = "logged_at_ms.asc") ?: return
        for (i in 0 until remote.length()) {
            val obj = remote.getJSONObject(i)
            val id = obj.optString("id")
            if (id.isBlank() || id in known) continue
            activityDao.insertRecord(
                ActivityRecord(
                    workoutTitle = obj.optString("workout_title"),
                    durationMinutes = obj.optInt("duration_minutes", 0),
                    caloriesBurned = obj.optInt("calories_burned", 0),
                    category = obj.optString("category"),
                    timestamp = obj.optLong("logged_at_ms", System.currentTimeMillis()),
                    userId = userId,
                    remoteId = id
                )
            )
        }
    }

    private suspend fun syncWeights(userId: String) {
        val local = weightDao.getWeightLogsByUserIdDirect(userId).map { entry ->
            if (entry.remoteId == null) {
                entry.copy(remoteId = newRemoteId()).also { weightDao.updateLog(it) }
            } else entry
        }
        client.upsertRows(TABLE_WEIGHT_LOGS, JSONArray().apply {
            local.mapNotNull { weightToJson(it) }.forEach { put(it) }
        })

        val known = local.mapNotNull { it.remoteId }.toHashSet()
        // Oldest first so Room's auto-increment ids keep the original order (UI sorts by id).
        val remote = client.fetchRows(TABLE_WEIGHT_LOGS, userId, order = "created_at.asc") ?: return
        for (i in 0 until remote.length()) {
            val obj = remote.getJSONObject(i)
            val id = obj.optString("id")
            if (id.isBlank() || id in known) continue
            weightDao.insertLog(
                WeightLogEntry(
                    dateString = obj.optString("date_label"),
                    weightKg = obj.optDouble("weight_kg", 0.0).toFloat(),
                    waistCm = obj.optNullableFloat("waist_cm"),
                    hipsCm = obj.optNullableFloat("hips_cm"),
                    note = obj.optString("note"),
                    userId = userId,
                    remoteId = id
                )
            )
        }
    }

    private suspend fun syncMeals(userId: String) {
        val local = mealDao.getMealsByUserIdDirect(userId).map { meal ->
            if (meal.remoteId == null) {
                meal.copy(remoteId = newRemoteId()).also { mealDao.updateMeal(it) }
            } else meal
        }
        client.upsertRows(TABLE_MEAL_LOGS, JSONArray().apply {
            local.mapNotNull { mealToJson(it) }.forEach { put(it) }
        })

        val known = local.mapNotNull { it.remoteId }.toHashSet()
        val remote = client.fetchRows(TABLE_MEAL_LOGS, userId, order = "logged_at_ms.asc") ?: return
        for (i in 0 until remote.length()) {
            val obj = remote.getJSONObject(i)
            val id = obj.optString("id")
            if (id.isBlank() || id in known) continue
            mealDao.insertMeal(
                LoggedMeal(
                    title = obj.optString("title"),
                    category = obj.optString("category"),
                    calories = obj.optInt("calories", 0),
                    protein = obj.optInt("protein", 0),
                    timestamp = obj.optLong("logged_at_ms", System.currentTimeMillis()),
                    userId = userId,
                    remoteId = id
                )
            )
        }
    }

    private suspend fun syncGoals(userId: String) {
        // 1. Retry deletions that were made while offline.
        val pending = pendingGoalDeletes.all()
        for (id in pending) {
            if (markGoalDeleted(id)) pendingGoalDeletes.remove(id)
        }

        // 2. Upload local goals. The payload never contains is_deleted, so the upsert can't
        //    undo a deletion made on another phone (merge-duplicates only sets sent columns).
        val local = goalDao.getGoalsByUserIdDirect(userId).map { goal ->
            if (goal.remoteId == null) {
                goal.copy(remoteId = newRemoteId()).also { goalDao.updateGoal(it) }
            } else goal
        }
        client.upsertRows(TABLE_DAILY_GOALS, JSONArray().apply {
            local.mapNotNull { goalToJson(it) }.forEach { put(it) }
        })

        // 3. Download: drop goals deleted elsewhere, add goals created elsewhere.
        val localByRemoteId = local.filter { it.remoteId != null }.associateBy { it.remoteId!! }
        val remote = client.fetchRows(TABLE_DAILY_GOALS, userId, order = "created_at.asc") ?: return
        for (i in 0 until remote.length()) {
            val obj = remote.getJSONObject(i)
            val id = obj.optString("id")
            if (id.isBlank() || id in pending) continue
            if (obj.optBoolean("is_deleted", false)) {
                localByRemoteId[id]?.let { goalDao.deleteGoal(it) }
                continue
            }
            if (id in localByRemoteId) continue
            goalDao.insertGoal(
                DailyGoalItem(
                    title = obj.optString("title"),
                    isCompleted = obj.optBoolean("is_completed", false),
                    category = obj.optString("category", "Wellness"),
                    userId = userId,
                    remoteId = id
                )
            )
        }
    }

    // -------------------------------------------------------------
    // Room entity -> Supabase row JSON
    // (Every row in a bulk upsert must have the same keys, so nulls use JSONObject.NULL.)
    // -------------------------------------------------------------

    private fun progressToJson(userId: String, progress: DailyProgress): JSONObject? {
        val date = progress.date.removePrefix("${userId}_")
        if (!ISO_DATE.matches(date)) return null
        return JSONObject()
            .put("user_id", userId)
            .put("log_date", date)
            .put("water_intake_ml", progress.waterIntakeMl)
            .put("calories_burned", progress.caloriesBurned)
            .put("active_minutes", progress.activeMinutes)
            .put("completed_workouts_count", progress.completedWorkoutsCount)
            .put("streak_days", progress.streakDays)
    }

    private fun workoutToJson(record: ActivityRecord): JSONObject? {
        val id = record.remoteId ?: return null
        return JSONObject()
            .put("id", id)
            .put("user_id", record.userId)
            .put("workout_title", record.workoutTitle)
            .put("duration_minutes", record.durationMinutes)
            .put("calories_burned", record.caloriesBurned)
            .put("category", record.category)
            .put("logged_at_ms", record.timestamp)
    }

    private fun weightToJson(entry: WeightLogEntry): JSONObject? {
        val id = entry.remoteId ?: return null
        return JSONObject()
            .put("id", id)
            .put("user_id", entry.userId)
            .put("date_label", entry.dateString)
            .put("weight_kg", entry.weightKg.toDouble())
            .put("waist_cm", entry.waistCm?.toDouble() ?: JSONObject.NULL)
            .put("hips_cm", entry.hipsCm?.toDouble() ?: JSONObject.NULL)
            .put("note", entry.note)
    }

    private fun mealToJson(meal: LoggedMeal): JSONObject? {
        val id = meal.remoteId ?: return null
        return JSONObject()
            .put("id", id)
            .put("user_id", meal.userId)
            .put("title", meal.title)
            .put("category", meal.category)
            .put("calories", meal.calories)
            .put("protein", meal.protein)
            .put("logged_at_ms", meal.timestamp)
    }

    private fun goalToJson(goal: DailyGoalItem): JSONObject? {
        val id = goal.remoteId ?: return null
        return JSONObject()
            .put("id", id)
            .put("user_id", goal.userId)
            .put("title", goal.title)
            .put("category", goal.category)
            .put("is_completed", goal.isCompleted)
    }

    private fun JSONObject.optNullableFloat(key: String): Float? =
        if (isNull(key)) null else optDouble(key).takeUnless { it.isNaN() }?.toFloat()
}
