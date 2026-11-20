package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User Profile & Authentication Model
 */
@Entity(tableName = "users")
data class UserProfile(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarId: Int = 1,
    val fitnessGoal: String = "Tone & Sculpt",
    val fitnessLevel: String = "Intermediate",
    val age: Int = 28,
    val heightCm: Float = 168f,
    val currentWeightKg: Float = 62.5f,
    val targetWeightKg: Float = 58.0f,
    val dailyWaterGoalMl: Int = 2500,
    val dailyCalorieBurnGoal: Int = 450,
    val soundEnabled: Boolean = true,
    val reminderNotifications: Boolean = true,
    val cycleLengthDays: Int = 28,
    val lastPeriodStartTimestamp: Long = 0L
)

/**
 * Workout & Exercise Data Models
 */
enum class WorkoutCategory(val title: String, val iconName: String) {
    BEGINNER("Beginner Tone", "SelfImprovement"),
    WEIGHT_LOSS("Weight Loss HIIT", "LocalFireDepartment"),
    YOGA("Hormone Harmony Yoga", "Spa"),
    CARDIO("Dance Cardio", "DirectionsRun"),
    STRENGTH("Hourglass Strength", "FitnessCenter"),
    HOME("Home Living Express", "Home")
}

data class Exercise(
    val id: String,
    val name: String,
    val durationSeconds: Int,
    val restSeconds: Int = 15,
    val repsOrHold: String,
    val targetMuscles: String,
    val instructions: String,
    val formTip: String
)

data class WorkoutPlan(
    val id: String,
    val title: String,
    val category: WorkoutCategory,
    val durationMinutes: Int,
    val estimatedCalories: Int,
    val difficulty: String,
    val description: String,
    val exercises: List<Exercise>,
    val bannerDrawableRes: Int? = null
)

/**
 * Nutrition & Meal Models
 */
enum class MealCategory(val label: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    DINNER("Dinner"),
    SNACKS("Healthy Snacks"),
    SMOOTHIES("Smoothies & Teas")
}

data class MacroNutrients(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

data class Recipe(
    val id: String,
    val title: String,
    val category: MealCategory,
    val prepTimeMinutes: Int,
    val macros: MacroNutrients,
    val femaleBenefits: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val isFavorite: Boolean = false
)

@Entity(tableName = "logged_meals")
data class LoggedMeal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val calories: Int,
    val protein: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "user_sarah_01"
)

/**
 * Progress & Activity Tracking Models
 */
@Entity(tableName = "daily_progress")
data class DailyProgress(
    @PrimaryKey val date: String, // format: "${userId}_YYYY-MM-DD" or "YYYY-MM-DD"
    val waterIntakeMl: Int = 0,
    val caloriesBurned: Int = 0,
    val activeMinutes: Int = 0,
    val completedWorkoutsCount: Int = 0,
    val streakDays: Int = 7
)

@Entity(tableName = "daily_goals")
data class DailyGoalItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val category: String = "Wellness",
    val userId: String = "user_sarah_01"
)

@Entity(tableName = "weight_logs")
data class WeightLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String,
    val weightKg: Float,
    val waistCm: Float? = null,
    val hipsCm: Float? = null,
    val note: String = "",
    val userId: String = "user_sarah_01"
)

@Entity(tableName = "activity_history")
data class ActivityRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutTitle: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "user_sarah_01"
)

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)

/**
 * AI Coach Maya Chat Model
 */
data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    COACH_MAYA
}
