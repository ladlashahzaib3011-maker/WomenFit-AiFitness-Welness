package com.example.data

import com.example.model.DailyGoalItem
import com.example.model.MacroNutrients
import com.example.model.MealCategory
import com.example.model.Recipe

object NutritionData {
    val sampleRecipes: List<Recipe> = listOf(
        Recipe(
            id = "rec_01",
            title = "Hormone Harmony Berry Chia Parfait",
            category = MealCategory.BREAKFAST,
            prepTimeMinutes = 10,
            macros = MacroNutrients(calories = 340, proteinGrams = 24, carbsGrams = 32, fatGrams = 12),
            femaleBenefits = listOf("Hormone Balance", "Estrogen Clearance", "Gut Health", "High Fiber"),
            ingredients = listOf(
                "1 cup Organic Greek yogurt (or coconut yogurt)",
                "2 tbsp Whole chia seeds",
                "1/2 cup Wild antioxidant blueberries & raspberries",
                "1 tbsp Ground flaxseed (lignans for estrogen metabolism)",
                "1 tsp Raw organic honey or pure maple syrup",
                "Handful sliced raw almonds"
            ),
            steps = listOf(
                "Stir Greek yogurt and chia seeds together in a mason jar or glass bowl.",
                "Let sit for 5 minutes to allow chia seeds to gently expand and hydrate.",
                "Layer with wild blueberries, fresh raspberries, and ground flaxseed.",
                "Drizzle raw honey and top with crunchy toasted almond slices. Enjoy chilled."
            )
        ),
        Recipe(
            id = "rec_02",
            title = "Golden Smashed Avocado & Poached Egg Toast",
            category = MealCategory.BREAKFAST,
            prepTimeMinutes = 12,
            macros = MacroNutrients(calories = 380, proteinGrams = 18, carbsGrams = 28, fatGrams = 22),
            femaleBenefits = listOf("Healthy Fats", "Brain Health", "Progesterone Support", "Biotin Rich"),
            ingredients = listOf(
                "2 slices Artisanal sourdough or sprouted grain bread",
                "1 ripe Hass avocado",
                "2 Free-range pasture-raised eggs",
                "1 tsp Lemon juice & pinch of sea salt",
                "Pinch of red pepper flakes & everything bagel seasoning",
                "Handful organic baby arugula"
            ),
            steps = listOf(
                "Toast the sourdough bread slices until golden and crisp.",
                "In a small bowl, mash the avocado with fresh lemon juice and sea salt.",
                "Poach or softly boil the eggs until yolks are luxuriously jammy (approx. 4 mins).",
                "Spread avocado generously over toast, top with arugula and warm eggs.",
                "Sprinkle with chili flakes and everything seasoning before serving."
            )
        ),
        Recipe(
            id = "rec_03",
            title = "Mediterranean Quinoa & Lemon-Herb Grilled Bowl",
            category = MealCategory.LUNCH,
            prepTimeMinutes = 15,
            macros = MacroNutrients(calories = 460, proteinGrams = 38, carbsGrams = 42, fatGrams = 16),
            femaleBenefits = listOf("Lean Muscle Repair", "Satiety & Steady Glucose", "Iron Rich", "Low GI"),
            ingredients = listOf(
                "150g Grilled organic chicken breast (or spiced chickpeas)",
                "3/4 cup Cooked fluffy tri-color quinoa",
                "1/2 cup English cucumber, diced",
                "1/2 cup Cherry tomatoes, halved",
                "2 tbsp Crumbled Kalamata olives & goat feta",
                "1 tbsp Extra virgin olive oil & fresh lemon dressing"
            ),
            steps = listOf(
                "Warm the quinoa and arrange as base in a wide lunch bowl.",
                "Slice the grilled chicken and arrange adjacent to crisp cucumbers and cherry tomatoes.",
                "Sprinkle salty crumbled feta and sliced olives across the bowl.",
                "Whisk olive oil, lemon juice, dried oregano, and drizzle generously over top."
            )
        ),
        Recipe(
            id = "rec_04",
            title = "Anti-Bloat Green Goddess Salmon Crunch Bowl",
            category = MealCategory.LUNCH,
            prepTimeMinutes = 18,
            macros = MacroNutrients(calories = 490, proteinGrams = 40, carbsGrams = 26, fatGrams = 24),
            femaleBenefits = listOf("Omega-3 Fatty Acids", "Cortisol Reduction", "Anti-inflammatory", "Skin Glow"),
            ingredients = listOf(
                "1 fillet (160g) Wild Atlantic salmon, pan-seared",
                "2 cups Mixed baby greens & ribboned zucchini",
                "1/2 cup Steamed edamame beans",
                "1/4 cup Pickled red onions",
                "Dressing: Tahini, apple cider vinegar, fresh dill & garlic"
            ),
            steps = listOf(
                "Season salmon with sea salt, black pepper, and sear in a hot skillet for 4 mins each side.",
                "Toss greens, zucchini ribbons, and edamame with light tahini-dill dressing.",
                "Top salad with warm flaky salmon and tang from pickled onions."
            )
        ),
        Recipe(
            id = "rec_05",
            title = "Baked Wild Salmon with Sweet Potato Mash & Asparagus",
            category = MealCategory.DINNER,
            prepTimeMinutes = 25,
            macros = MacroNutrients(calories = 520, proteinGrams = 42, carbsGrams = 38, fatGrams = 20),
            femaleBenefits = listOf("Deep Sleep Support", "Magnesium Boost", "Collagen Synthesis", "Thyroid Health"),
            ingredients = listOf(
                "180g Wild salmon fillet",
                "1 medium Japanese sweet potato, baked and mashed",
                "1 bunch Tender young asparagus spears",
                "1 tbsp Grass-fed ghee or extra virgin olive oil",
                "Garlic clove minced, fresh rosemary, lemon wedge"
            ),
            steps = listOf(
                "Preheat oven to 400°F (200°C).",
                "Toss asparagus in olive oil, garlic, and place on a sheet pan with salmon.",
                "Bake for 12-14 minutes until salmon flakes easily with a fork.",
                "Plate alongside sweet potato mash topped with a touch of ghee and sea salt."
            )
        ),
        Recipe(
            id = "rec_06",
            title = "Zucchini Noodle Turkey Bolognese with Fresh Basil",
            category = MealCategory.DINNER,
            prepTimeMinutes = 20,
            macros = MacroNutrients(calories = 390, proteinGrams = 36, carbsGrams = 22, fatGrams = 15),
            femaleBenefits = listOf("Low Carbohydrate", "Digestive Ease", "Iron Rich", "Cellular Energy"),
            ingredients = listOf(
                "160g Lean ground turkey breast (93/7)",
                "2 large Green zucchinis, spiralized into noodles",
                "1 cup Crushed San Marzano tomatoes with herbs",
                "1 tbsp Nutritional yeast or shaved pecorino",
                "Fresh sweet basil leaves & cracked black pepper"
            ),
            steps = listOf(
                "Brown ground turkey with garlic and onion in skillet until cooked through.",
                "Add crushed tomato sauce, oregano, and simmer for 8 minutes.",
                "Flash sauté spiralized zucchini noodles for just 90 seconds so they stay crisp.",
                "Serve rich bolognese over noodles, garnished with torn fresh basil leaves."
            )
        ),
        Recipe(
            id = "rec_07",
            title = "Dark Chocolate Sea Salt Maca Energy Bites",
            category = MealCategory.SNACKS,
            prepTimeMinutes = 12,
            macros = MacroNutrients(calories = 160, proteinGrams = 6, carbsGrams = 18, fatGrams = 8),
            femaleBenefits = listOf("Adrenal Support", "Endorphin Lift", "Craving Control", "Magnesium"),
            ingredients = listOf(
                "1 cup Rolled gluten-free oats",
                "1/2 cup Natural creamy almond butter",
                "1/4 cup Dark chocolate chips (70%+ cacao)",
                "1 tbsp Organic Peruvian maca root powder",
                "2 tbsp Pure maple syrup & flaky Maldon sea salt"
            ),
            steps = listOf(
                "Mix oats, almond butter, maca powder, and maple syrup in a bowl until dough forms.",
                "Fold in dark chocolate chips.",
                "Roll into 10 bite-sized spheres, sprinkle with flaky sea salt.",
                "Chill in refrigerator for 20 minutes before enjoying for mid-afternoon energy."
            )
        ),
        Recipe(
            id = "rec_08",
            title = "Glow Green Collagen Recovery Smoothie",
            category = MealCategory.SMOOTHIES,
            prepTimeMinutes = 5,
            macros = MacroNutrients(calories = 240, proteinGrams = 22, carbsGrams = 24, fatGrams = 5),
            femaleBenefits = listOf("Skin Elasticity", "Joint Recovery", "Liver Detox", "Hydration"),
            ingredients = listOf(
                "1 scoop Hydrolyzed grass-fed collagen peptides (or plant protein)",
                "1 cup Baby spinach & fresh cucumber slices",
                "1/2 Frozen banana & 1/2 green apple",
                "1 cup Pure coconut water",
                "Squeeze of fresh lime & mint leaves"
            ),
            steps = listOf(
                "Add spinach, cucumber, coconut water, and lime juice to high-speed blender.",
                "Add frozen banana, apple chunks, and collagen powder.",
                "Blend on high for 45 seconds until silky smooth, vibrant green, and frosty."
            )
        )
    )

    val defaultGoals: List<DailyGoalItem> = listOf(
        DailyGoalItem(id = 1, title = "Morning 15-min core & posture stretch", isCompleted = true, category = "Movement"),
        DailyGoalItem(id = 2, title = "Drink 2.5L water throughout the day", isCompleted = false, category = "Hydration"),
        DailyGoalItem(id = 3, title = "Reach 8,000 steps daily target", isCompleted = false, category = "Activity"),
        DailyGoalItem(id = 4, title = "Eat high-protein lunch with leafy greens", isCompleted = true, category = "Nutrition"),
        DailyGoalItem(id = 5, title = "5-minute evening box breathing", isCompleted = false, category = "Mindfulness")
    )

    /**
     * Calculates personalized water recommendation based on weight (kg) and workout duration (mins)
     */
    fun calculateRecommendedWater(weightKg: Float, workoutMinutes: Int): Int {
        // Baseline: ~35ml per kg of bodyweight + ~12ml per active workout minute
        val baseMl = (weightKg * 35).toInt().coerceIn(1800, 3600)
        val workoutExtra = (workoutMinutes * 12).coerceIn(0, 800)
        return ((baseMl + workoutExtra) / 50) * 50 // round to nearest 50ml
    }
}
