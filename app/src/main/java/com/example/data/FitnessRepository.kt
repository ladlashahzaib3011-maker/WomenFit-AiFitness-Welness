package com.example.data

import com.example.data.local.AppDatabase
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fitness & Wellness Repository.
 *
 * All user data streams (profile, hydration/daily progress, goals, weight logs,
 * workout history, and logged meals) are strictly scoped to the currently
 * authenticated user's ID from AuthRepository.
 */
class FitnessRepository(
    private val database: AppDatabase,
    private val activeUserIdFlow: StateFlow<String?> = MutableStateFlow(null),
    private val coachService: GeminiCoachService = GeminiCoachService(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val onProfileSync: suspend (UserProfile) -> Unit = {},
    /** Supabase womenfit_ai_* table sync. Null = local-only (e.g. unit tests). */
    private val cloudSync: FitnessCloudSync? = null
) {
    private val userDao = database.userDao()
    private val progressDao = database.progressDao()
    private val dailyGoalDao = database.dailyGoalDao()
    private val weightLogDao = database.weightLogDao()
    private val activityRecordDao = database.activityRecordDao()
    private val loggedMealDao = database.loggedMealDao()

    init {
        // Whenever a user signs in (or the app starts with a saved session), do a full
        // two-way sync with the Supabase womenfit_ai_* tables.
        if (cloudSync != null) {
            scope.launch {
                // StateFlow only emits on change (sign-out emits null in between), so this
                // runs once per sign-in, including signing back in to the same account.
                activeUserIdFlow.collect { uid ->
                    if (!uid.isNullOrBlank()) runCatching { cloudSync.syncAll(uid) }
                }
            }
        }
    }

    /** Fire-and-forget upload; never blocks or crashes the UI if the network fails. */
    private fun cloud(block: suspend FitnessCloudSync.() -> Unit) {
        val sync = cloudSync ?: return
        scope.launch {
            try {
                sync.block()
            } catch (e: Exception) {
                android.util.Log.w("FitnessRepository", "cloud push failed: ${e.javaClass.simpleName}")
            }
        }
    }

    private val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // In-memory chat message state for Coach Maya
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "msg_welcome",
                sender = MessageSender.COACH_MAYA,
                text = "Hello gorgeous! 🌸 I'm Coach Maya, your personal female fitness & wellness coach. How can I support your fitness, cycle syncing, nutrition, or motivation today?"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isCoachThinking = MutableStateFlow(false)
    val isCoachThinking: StateFlow<Boolean> = _isCoachThinking.asStateFlow()

    // User Profile - Strictly scoped to authenticated user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val userProfile: Flow<UserProfile> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(UserProfile(id = "", name = "", email = ""))
        } else {
            userDao.getUserProfileById(uid).map {
                it ?: UserProfile(id = uid, name = "WomanFit Member", email = "")
            }
        }
    }

    suspend fun updateUser(user: UserProfile) {
        val targetId = activeUserIdFlow.value ?: return
        val updated = user.copy(id = targetId)
        userDao.insertOrUpdate(updated)
        onProfileSync(updated)
    }

    // Progress - isolated per user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyProgress: Flow<DailyProgress> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(DailyProgress(date = todayString))
        } else {
            val userDateKey = "${uid}_$todayString"
            progressDao.getDailyProgress(userDateKey).map { prog ->
                prog ?: DailyProgress(date = userDateKey, streakDays = 1)
            }
        }
    }

    suspend fun incrementWater(amount: Int) {
        val uid = activeUserIdFlow.value ?: return
        val userDateKey = "${uid}_$todayString"
        val current = progressDao.getDailyProgress(userDateKey).firstOrNull()
            ?: DailyProgress(date = userDateKey, streakDays = 1)
        val updated = current.copy(
            date = userDateKey,
            waterIntakeMl = (current.waterIntakeMl + amount).coerceAtLeast(0)
        )
        progressDao.saveDailyProgress(updated)
        cloud { pushDailyProgress(uid, updated) }
    }

    suspend fun resetWater() {
        val uid = activeUserIdFlow.value ?: return
        val userDateKey = "${uid}_$todayString"
        val current = progressDao.getDailyProgress(userDateKey).firstOrNull()
            ?: DailyProgress(date = userDateKey, streakDays = 1)
        val updated = current.copy(date = userDateKey, waterIntakeMl = 0)
        progressDao.saveDailyProgress(updated)
        cloud { pushDailyProgress(uid, updated) }
    }

    // Goals - isolated per user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyGoals: Flow<List<DailyGoalItem>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            dailyGoalDao.getGoalsByUserId(uid)
        }
    }

    suspend fun toggleGoal(goal: DailyGoalItem) {
        val updated = goal.copy(isCompleted = !goal.isCompleted)
        dailyGoalDao.updateGoal(updated)
        if (updated.remoteId != null) {
            cloud { pushGoal(updated) }
        }
    }

    suspend fun addGoal(title: String, category: String) {
        val uid = activeUserIdFlow.value ?: return
        val goal = DailyGoalItem(
            title = title,
            isCompleted = false,
            category = category,
            userId = uid,
            remoteId = FitnessCloudSync.newRemoteId()
        )
        dailyGoalDao.insertGoal(goal)
        cloud { pushGoal(goal) }
    }

    suspend fun deleteGoal(goal: DailyGoalItem) {
        dailyGoalDao.deleteGoal(goal)
        cloud { this.deleteGoal(goal) }
    }

    // Weight logs - isolated per user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val weightLogs: Flow<List<WeightLogEntry>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            weightLogDao.getWeightLogsByUserId(uid)
        }
    }

    suspend fun logWeight(weight: Float, waist: Float?, hips: Float?, note: String) {
        val uid = activeUserIdFlow.value ?: return
        val dateLabel = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
        val entry = WeightLogEntry(
            dateString = dateLabel,
            weightKg = weight,
            waistCm = waist,
            hipsCm = hips,
            note = note,
            userId = uid,
            remoteId = FitnessCloudSync.newRemoteId()
        )
        weightLogDao.insertLog(entry)
        cloud { pushWeight(entry) }

        val currentProfile = userProfile.first()
        if (currentProfile.id.isNotBlank()) {
            val updatedProfile = currentProfile.copy(currentWeightKg = weight)
            userDao.insertOrUpdate(updatedProfile)
            onProfileSync(updatedProfile)
        }
    }

    // Activity Records - isolated per user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val activityRecords: Flow<List<ActivityRecord>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            activityRecordDao.getRecordsByUserId(uid)
        }
    }

    suspend fun logCompletedWorkout(plan: WorkoutPlan) {
        val uid = activeUserIdFlow.value ?: return
        val record = ActivityRecord(
            workoutTitle = plan.title,
            durationMinutes = plan.durationMinutes,
            caloriesBurned = plan.estimatedCalories,
            category = plan.category.title,
            userId = uid,
            remoteId = FitnessCloudSync.newRemoteId()
        )
        activityRecordDao.insertRecord(record)
        cloud { pushWorkout(record) }

        val userDateKey = "${uid}_$todayString"
        val current = progressDao.getDailyProgress(userDateKey).firstOrNull()
            ?: DailyProgress(date = userDateKey, streakDays = 1)
        val updated = current.copy(
            date = userDateKey,
            caloriesBurned = current.caloriesBurned + plan.estimatedCalories,
            activeMinutes = current.activeMinutes + plan.durationMinutes,
            completedWorkoutsCount = current.completedWorkoutsCount + 1
        )
        progressDao.saveDailyProgress(updated)
        cloud { pushDailyProgress(uid, updated) }
    }

    // Logged Meals - isolated per user ID
    @OptIn(ExperimentalCoroutinesApi::class)
    val loggedMeals: Flow<List<LoggedMeal>> = activeUserIdFlow.flatMapLatest { uid ->
        if (uid.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            loggedMealDao.getMealsByUserId(uid)
        }
    }

    suspend fun logMeal(title: String, category: String, calories: Int, protein: Int) {
        val uid = activeUserIdFlow.value ?: return
        val meal = LoggedMeal(
            title = title,
            category = category,
            calories = calories,
            protein = protein,
            userId = uid,
            remoteId = FitnessCloudSync.newRemoteId()
        )
        loggedMealDao.insertMeal(meal)
        cloud { pushMeal(meal) }
    }

    // Workouts catalog
    val workouts: List<WorkoutPlan> = WorkoutData.sampleWorkouts

    // Nutrition recipes catalog
    val recipes: List<Recipe> = NutritionData.sampleRecipes

    // Badges calculation
    val badges: Flow<List<AchievementBadge>> = combine(
        dailyProgress,
        activityRecords,
        weightLogs
    ) { progress, activities, weights ->
        listOf(
            AchievementBadge(
                id = "badge_streak_7",
                title = "7-Day Queen",
                description = "Completed fitness streak for 7 consecutive days",
                iconEmoji = "👑",
                isUnlocked = progress.streakDays >= 7,
                unlockedDate = if (progress.streakDays >= 7) "Unlocked" else null
            ),
            AchievementBadge(
                id = "badge_workout_first",
                title = "First Step Glow",
                description = "Logged your very first workout in WomanFit",
                iconEmoji = "🌸",
                isUnlocked = activities.isNotEmpty(),
                unlockedDate = if (activities.isNotEmpty()) "Unlocked" else null
            ),
            AchievementBadge(
                id = "badge_hydration_master",
                title = "Hydration Goddess",
                description = "Met your daily personalized water target",
                iconEmoji = "💧",
                isUnlocked = progress.waterIntakeMl >= 2000,
                unlockedDate = if (progress.waterIntakeMl >= 2000) "Unlocked" else null
            ),
            AchievementBadge(
                id = "badge_weight_tracker",
                title = "Body Harmony",
                description = "Consistently logged progress measurements",
                iconEmoji = "✨",
                isUnlocked = weights.size >= 3,
                unlockedDate = if (weights.size >= 3) "Unlocked" else null
            )
        )
    }

    // AI Coach Interaction
    fun askCoachMaya(userQuestion: String) {
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            sender = MessageSender.USER,
            text = userQuestion
        )
        _chatMessages.value = _chatMessages.value + userMsg

        scope.launch {
            _isCoachThinking.value = true
            val currentProfile = userProfile.first()

            val cyclePhase = if (currentProfile.lastPeriodStartTimestamp > 0L) {
                val cycleStatus = CycleCalculator.getStatus(
                    currentProfile.lastPeriodStartTimestamp,
                    currentProfile.cycleLengthDays
                )
                cycleStatus.phase.phaseName
            } else {
                "Regular"
            }

            val coachReplyText = coachService.askCoachMaya(
                userQuestion = userQuestion,
                userGoal = currentProfile.fitnessGoal,
                fitnessLevel = currentProfile.fitnessLevel,
                cyclePhase = cyclePhase
            )

            val replyMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                sender = MessageSender.COACH_MAYA,
                text = coachReplyText
            )
            _chatMessages.value = _chatMessages.value + replyMsg
            _isCoachThinking.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage(
                id = "msg_welcome",
                sender = MessageSender.COACH_MAYA,
                text = "Hello gorgeous! 🌸 Chat cleared. What would you like to focus on now?"
            )
        )
    }
}
