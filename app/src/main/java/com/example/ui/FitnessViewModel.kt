package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppContainer
import com.example.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    HOME("Home"),
    CYCLE("Cycle"),
    WORKOUTS("Workouts"),
    NUTRITION("Diet"),
    COACH("AI Coach"),
    PROGRESS("Progress"),
    PROFILE("Profile")
}

class FitnessViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppContainer.getInstance(application).fitnessRepository

    // Navigation and UI state
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Workout & Recipe states
    private val _activeWorkout = MutableStateFlow<WorkoutPlan?>(null)
    val activeWorkout: StateFlow<WorkoutPlan?> = _activeWorkout.asStateFlow()

    private val _activeRecipe = MutableStateFlow<Recipe?>(null)
    val activeRecipe: StateFlow<Recipe?> = _activeRecipe.asStateFlow()

    // Data streams from repository
    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile(id = "", name = "", email = ""))

    val dailyProgress: StateFlow<DailyProgress> = repository.dailyProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyProgress("today"))

    val dailyGoals: StateFlow<List<DailyGoalItem>> = repository.dailyGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightLogs: StateFlow<List<WeightLogEntry>> = repository.weightLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityRecords: StateFlow<List<ActivityRecord>> = repository.activityRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loggedMeals: StateFlow<List<LoggedMeal>> = repository.loggedMeals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<AchievementBadge>> = repository.badges
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workouts: List<WorkoutPlan> = repository.workouts
    val recipes: List<Recipe> = repository.recipes

    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val isCoachThinking: StateFlow<Boolean> = repository.isCoachThinking

    // Action handlers
    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun startWorkout(plan: WorkoutPlan) {
        _activeWorkout.value = plan
    }

    fun closeWorkoutPlayer() {
        _activeWorkout.value = null
    }

    fun finishWorkout(plan: WorkoutPlan) {
        viewModelScope.launch {
            repository.logCompletedWorkout(plan)
        }
    }

    fun viewRecipe(recipe: Recipe) {
        _activeRecipe.value = recipe
    }

    fun closeRecipeDetail() {
        _activeRecipe.value = null
    }

    fun addWater(amount: Int) {
        viewModelScope.launch {
            repository.incrementWater(amount)
        }
    }

    fun resetWater() {
        viewModelScope.launch {
            repository.resetWater()
        }
    }

    fun toggleGoal(goal: DailyGoalItem) {
        viewModelScope.launch {
            repository.toggleGoal(goal)
        }
    }

    fun addGoal(title: String, category: String) {
        viewModelScope.launch {
            repository.addGoal(title, category)
        }
    }

    fun deleteGoal(goal: DailyGoalItem) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun logWeight(weight: Float, waist: Float?, hips: Float?, note: String) {
        viewModelScope.launch {
            repository.logWeight(weight, waist, hips, note)
        }
    }

    fun logMeal(title: String, category: String, calories: Int, protein: Int) {
        viewModelScope.launch {
            repository.logMeal(title, category, calories, protein)
        }
    }

    fun sendMessageToCoach(message: String) {
        if (message.isNotBlank()) {
            repository.askCoachMaya(message.trim())
        }
    }

    fun clearCoachChat() {
        repository.clearChat()
    }

    fun updateProfile(user: UserProfile) {
        viewModelScope.launch {
            repository.updateUser(user)
        }
    }

    fun resetUiStateOnSignOut() {
        _activeWorkout.value = null
        _activeRecipe.value = null
        _currentTab.value = MainTab.HOME
        repository.clearChat()
    }
}
