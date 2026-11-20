package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.NutritionData
import com.example.model.HormoneCyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("WomanFit", appName)
  }

  @Test
  fun `hydration calculation logic is valid`() {
    val water = NutritionData.calculateRecommendedWater(60f, 30)
    assertTrue("Water calculation should be healthy and positive", water in 2000..3500)
  }

  @Test
  fun `cycle phase calculation correctly identifies all 4 phases for standard 28-day cycle`() {
    assertEquals(HormoneCyclePhase.MENSTRUAL, HormoneCyclePhase.calculatePhase(currentDay = 2, cycleLength = 28))
    assertEquals(HormoneCyclePhase.FOLLICULAR, HormoneCyclePhase.calculatePhase(currentDay = 9, cycleLength = 28))
    assertEquals(HormoneCyclePhase.OVULATORY, HormoneCyclePhase.calculatePhase(currentDay = 15, cycleLength = 28))
    assertEquals(HormoneCyclePhase.LUTEAL, HormoneCyclePhase.calculatePhase(currentDay = 22, cycleLength = 28))
  }

  @Test
  fun `cycle phase calculation accurately adapts to variable 35-day cycle length`() {
    // In a 35-day cycle, ovulation occurs around day 21 (35 - 14)
    assertEquals(HormoneCyclePhase.MENSTRUAL, HormoneCyclePhase.calculatePhase(currentDay = 4, cycleLength = 35))
    assertEquals(HormoneCyclePhase.FOLLICULAR, HormoneCyclePhase.calculatePhase(currentDay = 16, cycleLength = 35))
    assertEquals(HormoneCyclePhase.OVULATORY, HormoneCyclePhase.calculatePhase(currentDay = 21, cycleLength = 35))
    assertEquals(HormoneCyclePhase.LUTEAL, HormoneCyclePhase.calculatePhase(currentDay = 28, cycleLength = 35))
  }
}
