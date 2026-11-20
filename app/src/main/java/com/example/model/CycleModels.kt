package com.example.model

import java.util.concurrent.TimeUnit

enum class HormoneCyclePhase(
    val phaseName: String,
    val hormoneStatus: String,
    val energyLevel: String,
    val workoutRecommendation: String,
    val recommendedCategory: WorkoutCategory,
    val nutritionGuidance: String,
    val iconEmoji: String
) {
    MENSTRUAL(
        phaseName = "Menstrual Phase",
        hormoneStatus = "Estrogen & Progesterone are at baseline low",
        energyLevel = "Gentle & Restorative",
        workoutRecommendation = "Cortisol-lowering yoga, pelvic floor releases, gentle walks & hip openers.",
        recommendedCategory = WorkoutCategory.YOGA,
        nutritionGuidance = "Warm stews, iron-rich dark leafy greens, magnesium-rich seeds, and comforting herbal teas.",
        iconEmoji = "🩸"
    ),
    FOLLICULAR(
        phaseName = "Follicular Phase",
        hormoneStatus = "Estrogen is rising steadily; insulin sensitivity peaks",
        energyLevel = "High Energy & Creativeness",
        workoutRecommendation = "Dynamic Dance Cardio, metabolic HIIT circuits, and trying new choreography.",
        recommendedCategory = WorkoutCategory.CARDIO,
        nutritionGuidance = "Fermented foods, sprouted grains, cruciferous veggies for estrogen metabolism, and vibrant citrus.",
        iconEmoji = "🌸"
    ),
    OVULATORY(
        phaseName = "Ovulatory Phase",
        hormoneStatus = "Estrogen and Testosterone peak; maximum power output",
        energyLevel = "Peak Strength & Confidence",
        workoutRecommendation = "Heavy compound strength training, progressive glute loading, and personal record attempts.",
        recommendedCategory = WorkoutCategory.STRENGTH,
        nutritionGuidance = "High-protein lean bowls, zinc-rich seeds, berry smoothies, and antioxidant hydration.",
        iconEmoji = "✨"
    ),
    LUTEAL(
        phaseName = "Luteal Phase",
        hormoneStatus = "Progesterone rises; resting metabolic rate increases by 100-300 kcal",
        energyLevel = "Moderate; sensitive to cortisol and fluid retention",
        workoutRecommendation = "Steady-state resistance, Beginner core & posture tone, and express living room routines.",
        recommendedCategory = WorkoutCategory.BEGINNER,
        nutritionGuidance = "Complex slow-digesting carbs (sweet potatoes, oats), cucumber and potassium for anti-bloat.",
        iconEmoji = "🌙"
    );

    fun getDayRangeDescription(cycleLength: Int = 28): String {
        // Physiologically, ovulation occurs ~14 days prior to next menses
        val ovulationDay = (cycleLength - 14).coerceIn(6, cycleLength - 7)
        val menstrualEnd = 5.coerceAtMost(ovulationDay - 3)
        val follicularStart = menstrualEnd + 1
        val follicularEnd = (ovulationDay - 2).coerceAtLeast(follicularStart)
        val ovulatoryStart = (ovulationDay - 1).coerceAtLeast(follicularEnd + 1)
        val ovulatoryEnd = (ovulationDay + 1).coerceAtMost(cycleLength - 1)
        val lutealStart = ovulatoryEnd + 1

        return when (this) {
            MENSTRUAL -> "Days 1 – $menstrualEnd"
            FOLLICULAR -> "Days $follicularStart – $follicularEnd"
            OVULATORY -> "Days $ovulatoryStart – $ovulatoryEnd"
            LUTEAL -> "Days $lutealStart – $cycleLength"
        }
    }

    companion object {
        fun calculatePhase(currentDay: Int, cycleLength: Int = 28): HormoneCyclePhase {
            val normalizedDay = ((currentDay - 1) % cycleLength) + 1
            // Biological anchor: Luteal phase length is physiologically fixed at ~14 days
            val ovulationDay = (cycleLength - 14).coerceIn(6, cycleLength - 7)
            val menstrualEnd = 5.coerceAtMost(ovulationDay - 3)
            val follicularEnd = (ovulationDay - 2).coerceAtLeast(menstrualEnd + 1)
            val ovulatoryEnd = (ovulationDay + 1).coerceAtMost(cycleLength - 1)

            return when {
                normalizedDay <= menstrualEnd -> MENSTRUAL
                normalizedDay <= follicularEnd -> FOLLICULAR
                normalizedDay <= ovulatoryEnd -> OVULATORY
                else -> LUTEAL
            }
        }
    }
}

data class CycleStatus(
    val currentCycleDay: Int,
    val totalCycleLength: Int,
    val phase: HormoneCyclePhase,
    val daysUntilNextPhase: Int,
    val daysUntilNextPeriod: Int,
    val completionFraction: Float
)

object CycleCalculator {
    fun getStatus(lastPeriodStartTimestamp: Long, cycleLengthDays: Int = 28): CycleStatus {
        val now = System.currentTimeMillis()
        val elapsedMillis = (now - lastPeriodStartTimestamp).coerceAtLeast(0L)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()

        val currentDay = (elapsedDays % cycleLengthDays) + 1
        val phase = HormoneCyclePhase.calculatePhase(currentDay, cycleLengthDays)
        val daysUntilNextPeriod = (cycleLengthDays - currentDay).coerceAtLeast(0)

        // Dynamic phase duration bounds
        val ovulationDay = (cycleLengthDays - 14).coerceIn(6, cycleLengthDays - 7)
        val menstrualEnd = 5.coerceAtMost(ovulationDay - 3)
        val follicularEnd = (ovulationDay - 2).coerceAtLeast(menstrualEnd + 1)
        val ovulatoryEnd = (ovulationDay + 1).coerceAtMost(cycleLengthDays - 1)

        val daysUntilNextPhase = when (phase) {
            HormoneCyclePhase.MENSTRUAL -> (menstrualEnd - currentDay + 1).coerceAtLeast(1)
            HormoneCyclePhase.FOLLICULAR -> (follicularEnd - currentDay + 1).coerceAtLeast(1)
            HormoneCyclePhase.OVULATORY -> (ovulatoryEnd - currentDay + 1).coerceAtLeast(1)
            HormoneCyclePhase.LUTEAL -> daysUntilNextPeriod.coerceAtLeast(1)
        }

        val completionFraction = (currentDay.toFloat() / cycleLengthDays.toFloat()).coerceIn(0f, 1f)

        return CycleStatus(
            currentCycleDay = currentDay,
            totalCycleLength = cycleLengthDays,
            phase = phase,
            daysUntilNextPhase = daysUntilNextPhase,
            daysUntilNextPeriod = daysUntilNextPeriod,
            completionFraction = completionFraction
        )
    }
}
