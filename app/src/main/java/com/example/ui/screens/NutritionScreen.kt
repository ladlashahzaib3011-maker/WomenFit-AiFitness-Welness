package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.NutritionData
import com.example.model.MealCategory
import com.example.model.Recipe
import com.example.ui.FitnessViewModel
import com.example.ui.theme.*

@Composable
fun NutritionScreen(
    viewModel: FitnessViewModel,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(MealCategory.BREAKFAST) }
    var selectedRecipeForDetail by remember { mutableStateOf<Recipe?>(null) }
    var showLogMealDialog by remember { mutableStateOf(false) }

    // Meal log custom inputs
    var customMealTitle by remember { mutableStateOf("") }
    var customMealCalories by remember { mutableStateOf("350") }
    var customMealProtein by remember { mutableStateOf("25") }

    // Hydration calculator states
    var calcWeightInput by remember { mutableStateOf("62") }
    var calcWorkoutMinsInput by remember { mutableStateOf("30") }
    var calculatedWaterMl by remember { mutableIntStateOf(2500) }

    val loggedMeals by viewModel.loggedMeals.collectAsState()
    val totalCaloriesLogged = loggedMeals.sumOf { it.calories }
    val totalProteinLogged = loggedMeals.sumOf { it.protein }

    val filteredRecipes = remember(selectedCategory) {
        viewModel.recipes.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nutrition_healthy),
                    contentDescription = "Nutrition",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Diet & Hormonal Nutrition",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "Whole foods tailored for gut health, estrogen balance & energy",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFFCE7F3))
                    )
                }
            }
        }

        // Daily Food Log Summary & Add Meal
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Logged Nutrition",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${loggedMeals.size} meals recorded today",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }

                        Button(
                            onClick = { showLogMealDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WomanFitViolet),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("log_meal_open_dialog_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = WomanFitLightRose
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🔥 Calories", style = MaterialTheme.typography.labelSmall.copy(color = WomanFitDeepRose))
                                Text(
                                    text = "$totalCaloriesLogged kcal",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = WomanFitLightViolet
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🥩 Protein", style = MaterialTheme.typography.labelSmall.copy(color = WomanFitDarkViolet))
                                Text(
                                    text = "$totalProteinLogged g",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Meal Category Tabs
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MealCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WomanFitRose,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("meal_category_${category.name.lowercase()}")
                    )
                }
            }
        }

        // Curated Recipes List
        items(filteredRecipes) { recipe ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { selectedRecipeForDetail = recipe }
                    .testTag("recipe_card_${recipe.id}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = recipe.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WomanFitLightRose
                        ) {
                            Text(
                                text = "⏱ ${recipe.prepTimeMinutes}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = WomanFitDeepRose,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Macro Pills
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${recipe.macros.calories} kcal",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = WomanFitRose)
                        )
                        Text(text = "•", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = "Protein: ${recipe.macros.proteinGrams}g",
                            style = MaterialTheme.typography.labelSmall.copy(color = WomanFitViolet, fontWeight = FontWeight.Bold)
                        )
                        Text(text = "•", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = "Carbs: ${recipe.macros.carbsGrams}g",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(text = "•", color = Color.Gray, fontSize = 11.sp)
                        Text(
                            text = "Fat: ${recipe.macros.fatGrams}g",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Female Wellness Badges
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(recipe.femaleBenefits) { benefit ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF3E8FF)
                            ) {
                                Text(
                                    text = "✨ $benefit",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = WomanFitDarkViolet,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Hydration Recommendation Calculator
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "💧 ", fontSize = 20.sp)
                        Text(
                            text = "Personalized Hydration Calculator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "Calculates your body's cellular water requirement based on weight and active perspiration.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = calcWeightInput,
                            onValueChange = {
                                calcWeightInput = it
                                val w = it.toFloatOrNull() ?: 60f
                                val m = calcWorkoutMinsInput.toIntOrNull() ?: 30
                                calculatedWaterMl = NutritionData.calculateRecommendedWater(w, m)
                            },
                            label = { Text("Weight (kg)") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = calcWorkoutMinsInput,
                            onValueChange = {
                                calcWorkoutMinsInput = it
                                val w = calcWeightInput.toFloatOrNull() ?: 60f
                                val m = it.toIntOrNull() ?: 30
                                calculatedWaterMl = NutritionData.calculateRecommendedWater(w, m)
                            },
                            label = { Text("Workout (mins)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recommended Intake:",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "$calculatedWaterMl ml / day",
                                style = MaterialTheme.typography.titleLarge.copy(color = Color(0xFF0284C7), fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }
    }

    // Recipe Detail Modal
    selectedRecipeForDetail?.let { recipe ->
        AlertDialog(
            onDismissRequest = { selectedRecipeForDetail = null },
            title = {
                Column {
                    Text(text = recipe.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "${recipe.macros.calories} kcal • ${recipe.macros.proteinGrams}g Protein • ${recipe.prepTimeMinutes}m prep",
                        style = MaterialTheme.typography.labelSmall.copy(color = WomanFitRose)
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(text = "Female Wellness Benefits:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Text(
                            text = recipe.femaleBenefits.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall.copy(color = WomanFitDarkViolet, fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Ingredients Checklist:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    items(recipe.ingredients) { ing ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "• ", color = WomanFitRose, fontWeight = FontWeight.Bold)
                            Text(text = ing, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Preparation Instructions:", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }

                    items(recipe.steps) { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logMeal(recipe.title, recipe.category.label, recipe.macros.calories, recipe.macros.proteinGrams)
                        selectedRecipeForDetail = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose),
                    modifier = Modifier.testTag("log_recipe_to_diary_btn")
                ) {
                    Text("Log to Today's Diary")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedRecipeForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Custom Meal Dialog
    if (showLogMealDialog) {
        AlertDialog(
            onDismissRequest = { showLogMealDialog = false },
            title = { Text("Log Meal or Snack", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customMealTitle,
                        onValueChange = { customMealTitle = it },
                        label = { Text("Meal or Snack Name") },
                        placeholder = { Text("e.g. Greek Yogurt & Berries") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customMealCalories,
                        onValueChange = { customMealCalories = it },
                        label = { Text("Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customMealProtein,
                        onValueChange = { customMealProtein = it },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customMealTitle.isNotBlank()) {
                            val cals = customMealCalories.toIntOrNull() ?: 300
                            val prot = customMealProtein.toIntOrNull() ?: 20
                            viewModel.logMeal(customMealTitle.trim(), selectedCategory.label, cals, prot)
                            customMealTitle = ""
                            showLogMealDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WomanFitRose)
                ) {
                    Text("Save to Diary")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
