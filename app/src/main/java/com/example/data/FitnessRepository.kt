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
    private val onProfileSync: suspend (UserProfile) -> Unit = {}
) {
    private val userDao = database.userDao()
    private val progressDao = database.progressDao()
    private val dailyGoalDao = database.dailyGoalDao()
    private val weightLogDao = database.weightLogDao()
    private val activityRecordDao = database.activityRecordDao()
    private val loggedMealDao = database.loggedMealDao()

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
    }

    suspend fun resetWater() {
        val uid = activeUserIdFlow.value ?: return
        val userDateKey = "${uid}_$todayString"
        val current = progressDao.getDailyProgress(userDateKey).firstOrNull()
            ?: DailyProgress(date = userDateKey, streakDays = 1)
        progressDao.saveDailyProgress(current.copy(waterIntakeMl = 0))
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
        dailyGoalDao.updateGoal(goal.copy(isCompleted = !goal.isCompleted))
    }

    suspend fun addGoal(title: String, category: String) {
        val uid = activeUserIdFlow.value ?: return
        dailyGoalDao.insertGoal(
            DailyGoalItem(
                title = title,
                isCompleted = false,
                category = category,
                userId = uid
            )
        )
    }

    suspend fun deleteGoal(goal: DailyGoalItem) {
        dailyGoalDao.deleteGoal(goal)
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
        weightLogDao.insertLog(
            WeightLogEntry(
                dateString = dateLabel,
                weightKg = weight,
                waistCm = waist,
                hipsCm = hips,
                note = note,
                userId = uid
            )
        )
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
            userId = uid
        )
        activityRecordDao.insertRecord(record)

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
        loggedMealDao.insertMeal(
            LoggedMeal(
                title = title,
                category = category,
                calories = calories,
                protein = protein,
                userId = uid
            )
        )
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
