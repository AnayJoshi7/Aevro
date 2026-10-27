package com.anay.fitnesstracker.Screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anay.fitnesstracker.data.*
import com.anay.fitnesstracker.data.viewmodel.FitnessViewModel
import com.anay.fitnesstracker.ui.theme.appBackgroundGradient
import com.anay.fitnesstracker.util.BarcodeAnalyzer


private val CardDarkBg = Color(0x1A3DDC84)
private val CardBorderColor = Color(0xFF3DDC84).copy(alpha = 0.18f)
private val DropdownCardBg = Color(0xFF161719)
private val PillBackground = Color(0x80FFFFFF)
private val PillBorderColor = Color(0xFFFFFFFF).copy(alpha = 0.5f)
private val PrimaryGreen = Color(0xFF27D07F)
private val TextWhite = Color(0xFFFFFFFF)
private val TextMuted = Color(0xFF9E9E9E)

@Composable
fun AddMealScreen(
    fitnessViewModel: FitnessViewModel,
    onNavigateBack: () -> Unit,
    onNavigateScan: () -> Unit
) {
    val context = LocalContext.current
    val foodsList by fitnessViewModel.foodsList.collectAsState()
    val pendingScannedFood by fitnessViewModel.pendingScannedFood.collectAsState()

    var mealName by remember { mutableStateOf("") }
    val stagedItems = remember { mutableStateListOf<MealItemEntry>() }

    // Current Item Input States
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var foodSearchQuery by remember { mutableStateOf("") }
    var foodDropdownOpen by remember { mutableStateOf(false) }

    var qtyText by remember { mutableStateOf("1") }

    val servingUnits = listOf("grams", "ml", "scoop")
    var selectedServingUnit by remember { mutableStateOf("grams") }
    var unitDropdownOpen by remember { mutableStateOf(false) }

    // Auto-calculated or manually overridden macros
    var proteinText by remember { mutableStateOf("0") }
    var carbsText by remember { mutableStateOf("0") }
    var fatsText by remember { mutableStateOf("0") }
    var caloriesText by remember { mutableStateOf("0") }

    // Manual Edit Mode Toggles
    var manualProtein by remember { mutableStateOf(false) }
    var manualCarbs by remember { mutableStateOf(false) }
    var manualFats by remember { mutableStateOf(false) }
    var manualCalories by remember { mutableStateOf(false) }

    fun enableAllManual() {
        manualProtein = true
        manualCarbs = true
        manualFats = true
        manualCalories = true
    }

    // Auto-populate when returning from QR/Barcode scanner
    LaunchedEffect(pendingScannedFood) {
        pendingScannedFood?.let { scannedItem ->
            selectedFood = scannedItem
            foodSearchQuery = scannedItem.name
            manualProtein = false
            manualCarbs = false
            manualFats = false
            manualCalories = false
            qtyText = "1"
            selectedServingUnit = if (scannedItem.servingBasis.contains("ml", ignoreCase = true)) "ml" else "grams"
            fitnessViewModel.clearPendingScannedFood()
            Toast.makeText(context, "Loaded: ${scannedItem.name}", Toast.LENGTH_SHORT).show()
        }
    }

    fun parseBaseServingSize(basis: String): Double {
        val digits = basis.takeWhile { it.isDigit() || it == '.' }
        if (digits.isNotEmpty()) {
            return digits.toDoubleOrNull() ?: 100.0
        }
        val matched = Regex("""\d+(\.\d+)?""").find(basis)
        return matched?.value?.toDoubleOrNull() ?: 100.0
    }

    // Auto-calculate logic (runs when catalog item is selected and manual override is off)
    LaunchedEffect(selectedFood, qtyText, selectedServingUnit) {
        val food = selectedFood ?: return@LaunchedEffect
        val enteredQty = qtyText.toDoubleOrNull() ?: 0.0

        if (enteredQty <= 0.0) {
            if (!manualProtein) proteinText = "0"
            if (!manualCarbs) carbsText = "0"
            if (!manualFats) fatsText = "0"
            if (!manualCalories) caloriesText = "0"
            return@LaunchedEffect
        }

        val baseServing = parseBaseServingSize(food.servingBasis)
        val isBasisScoop = food.servingBasis.contains("scoop", ignoreCase = true)

        val multiplier: Double = when {
            selectedServingUnit == "scoop" && isBasisScoop -> enteredQty
            selectedServingUnit == "scoop" && !isBasisScoop -> (enteredQty * 30.0) / baseServing
            else -> enteredQty / baseServing
        }

        if (!manualProtein) proteinText = String.format("%.1f", food.protein * multiplier)
        if (!manualCarbs) carbsText = String.format("%.1f", food.carbohydrates * multiplier)
        if (!manualFats) fatsText = String.format("%.1f", food.fat * multiplier)
        if (!manualCalories) caloriesText = "${(food.calories * multiplier).toInt()}"
    }

    fun resetCurrentItem() {
        selectedFood = null
        foodSearchQuery = ""
        qtyText = "1"
        proteinText = "0"
        carbsText = "0"
        fatsText = "0"
        caloriesText = "0"
        manualProtein = false
        manualCarbs = false
        manualFats = false
        manualCalories = false
        foodDropdownOpen = false
    }

    val filteredFoods = remember(foodsList, foodSearchQuery) {
        if (foodSearchQuery.isBlank()) foodsList
        else foodsList.filter { it.name.contains(foodSearchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .appBackgroundGradient()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Back button pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateBack() }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Back",
                    color = PrimaryGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Add Meals",
                color = PrimaryGreen,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Main Container Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(CardDarkBg)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(22.dp))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Name of the meal",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PillInputField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        placeholder = "e.g. Breakfast, Pre-Workout Snack",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Item Header Row with Scan Barcode/QR Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item-${stagedItems.size + 1}",
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Scan Barcode / QR Pill Button
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF162E21))
                                .border(1.dp, PrimaryGreen.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .clickable { onNavigateScan() }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan Barcode or QR",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Scan Code",
                                color = PrimaryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Item-1, Qty, Serving Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Item Selector (Search or Custom Entry Pill)
                        Column(modifier = Modifier.weight(1.3f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PillBackground)
                                    .border(1.dp, PillBorderColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (foodSearchQuery.isEmpty() && selectedFood == null) {
                                            Text(
                                                text = "Name",
                                                color = Color.DarkGray,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        BasicTextField(
                                            value = if (selectedFood != null) selectedFood!!.name else foodSearchQuery,
                                            onValueChange = { query ->
                                                selectedFood = null
                                                foodSearchQuery = query
                                                foodDropdownOpen = query.isNotBlank()
                                            },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                color = Color.Black,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (foodSearchQuery.isNotEmpty() || selectedFood != null) {
                                        IconButton(
                                            onClick = {
                                                selectedFood = null
                                                foodSearchQuery = ""
                                                foodDropdownOpen = false
                                            },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = Color.DarkGray,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { foodDropdownOpen = !foodDropdownOpen },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            tint = Color.Black
                                        )
                                    }
                                }
                            }
                        }

                        // Qty Field
                        Column(
                            modifier = Modifier.weight(0.75f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PillBackground)
                                    .border(1.dp, PillBorderColor, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicTextField(
                                    value = qtyText,
                                    onValueChange = { qtyText = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }

                        // Serving Unit Dropdown Pill
                        Column(
                            modifier = Modifier.weight(1.05f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PillBackground)
                                    .border(1.dp, PillBorderColor, RoundedCornerShape(16.dp))
                                    .clickable { unitDropdownOpen = true }
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedServingUnit,
                                        color = Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = unitDropdownOpen,
                                    onDismissRequest = { unitDropdownOpen = false },
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DropdownCardBg)
                                        .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(16.dp))
                                ) {
                                    servingUnits.forEach { unit ->
                                        DropdownMenuItem(
                                            text = { Text(unit, color = TextWhite, fontSize = 14.sp) },
                                            onClick = {
                                                selectedServingUnit = unit
                                                unitDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Docked Dropdown Menu
                    AnimatedVisibility(
                        visible = foodDropdownOpen && (filteredFoods.isNotEmpty() || foodSearchQuery.isNotBlank()),
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DropdownCardBg)
                                .border(1.dp, Color(0xFFFFFFFF), RoundedCornerShape(16.dp))
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Custom unlisted item option
                            if (foodSearchQuery.isNotBlank() && foodsList.none { it.name.equals(foodSearchQuery, ignoreCase = true) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFood = null
                                            enableAllManual()
                                            foodDropdownOpen = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Use \"$foodSearchQuery\"",
                                                color = PrimaryGreen,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Custom food (fill values manually)",
                                                color = TextMuted,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                HorizontalDivider(
                                    color = Color(0xFF242528),
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }

                            // Catalog Food items list
                            filteredFoods.forEachIndexed { index, food ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFood = food
                                            foodSearchQuery = food.name
                                            manualProtein = false
                                            manualCarbs = false
                                            manualFats = false
                                            manualCalories = false
                                            foodDropdownOpen = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = food.name,
                                            color = TextWhite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Text(
                                            text = "${food.servingBasis} • ${food.calories.toInt()} kcal",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (index < filteredFoods.lastIndex) {
                                    HorizontalDivider(
                                        color = Color(0xFF242528),
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Protein Row
                    MacroEditRow(
                        label = "Protein",
                        unit = "g",
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        isManual = manualProtein,
                        onToggleManual = { manualProtein = !manualProtein }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Carbs Row
                    MacroEditRow(
                        label = "Carbs",
                        unit = "g",
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        isManual = manualCarbs,
                        onToggleManual = { manualCarbs = !manualCarbs }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fats Row
                    MacroEditRow(
                        label = "Fats",
                        unit = "g",
                        value = fatsText,
                        onValueChange = { fatsText = it },
                        isManual = manualFats,
                        onToggleManual = { manualFats = !manualFats }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calories Row
                    MacroEditRow(
                        label = "Calories",
                        unit = "kcals",
                        value = caloriesText,
                        onValueChange = { caloriesText = it },
                        isManual = manualCalories,
                        onToggleManual = { manualCalories = !manualCalories }
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Save item & Add item buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(PrimaryGreen)
                                .clickable {
                                    val itemName = selectedFood?.name ?: foodSearchQuery
                                    if (itemName.isBlank()) {
                                        Toast.makeText(context, "Select or enter an item name", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    stagedItems.add(
                                        MealItemEntry(
                                            name = itemName,
                                            qty = qtyText.toDoubleOrNull() ?: 1.0,
                                            unit = selectedServingUnit,
                                            calories = caloriesText.toDoubleOrNull() ?: 0.0,
                                            protein = proteinText.toDoubleOrNull() ?: 0.0,
                                            carbs = carbsText.toDoubleOrNull() ?: 0.0,
                                            fat = fatsText.toDoubleOrNull() ?: 0.0
                                        )
                                    )
                                    Toast.makeText(context, "Item added to meal!", Toast.LENGTH_SHORT).show()
                                    resetCurrentItem()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save item",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(23.dp))
                                .background(Color(0xFF0F1E16))
                                .border(1.dp, CardBorderColor, RoundedCornerShape(23.dp))
                                .clickable {
                                    val itemName = selectedFood?.name ?: foodSearchQuery
                                    if (itemName.isNotBlank()) {
                                        stagedItems.add(
                                            MealItemEntry(
                                                name = itemName,
                                                qty = qtyText.toDoubleOrNull() ?: 1.0,
                                                unit = selectedServingUnit,
                                                calories = caloriesText.toDoubleOrNull() ?: 0.0,
                                                protein = proteinText.toDoubleOrNull() ?: 0.0,
                                                carbs = carbsText.toDoubleOrNull() ?: 0.0,
                                                fat = fatsText.toDoubleOrNull() ?: 0.0
                                            )
                                        )
                                    }
                                    resetCurrentItem()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Add item",
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add",
                                        tint = Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Staged Items Summary
                    if (stagedItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Items in this meal (${stagedItems.size}):",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        stagedItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}. ${item.name} (${item.qty} ${item.unit})",
                                    color = TextWhite,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f, fill = false),
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "${item.calories.toInt()} kcal",
                                    color = PrimaryGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Meal Final Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(PrimaryGreen)
                            .clickable {
                                val currentItemName = selectedFood?.name ?: foodSearchQuery
                                if (stagedItems.isEmpty() && currentItemName.isBlank()) {
                                    Toast.makeText(context, "Please add at least 1 item", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }

                                if (currentItemName.isNotBlank()) {
                                    stagedItems.add(
                                        MealItemEntry(
                                            name = currentItemName,
                                            qty = qtyText.toDoubleOrNull() ?: 1.0,
                                            unit = selectedServingUnit,
                                            calories = caloriesText.toDoubleOrNull() ?: 0.0,
                                            protein = proteinText.toDoubleOrNull() ?: 0.0,
                                            carbs = carbsText.toDoubleOrNull() ?: 0.0,
                                            fat = fatsText.toDoubleOrNull() ?: 0.0
                                        )
                                    )
                                }

                                val finalCalories = stagedItems.sumOf { it.calories }.toInt()
                                val finalProtein = stagedItems.sumOf { it.protein }
                                val finalCarbs = stagedItems.sumOf { it.carbs }
                                val finalFats = stagedItems.sumOf { it.fat }

                                fitnessViewModel.saveCompleteMeal(
                                    mealName = mealName.ifBlank { "Meal" },
                                    totalCalories = finalCalories,
                                    totalProtein = finalProtein,
                                    totalCarbs = finalCarbs,
                                    totalFat = finalFats,
                                    items = stagedItems.toList(),
                                    onComplete = {
                                        Toast.makeText(context, "Meal saved successfully!", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Save Meal",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // View Saved Meals Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .border(1.dp, CardBorderColor, RoundedCornerShape(27.dp))
                        .background(Color(0xFF14241C))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "View Saved Meals",
                        color = TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MacroEditRow(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    isManual: Boolean,
    onToggleManual: () -> Unit
) {
    Column {
        Text(
            text = label,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PillBackground)
                    .border(1.dp, PillBorderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = isManual,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = unit,
                        color = Color.DarkGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isManual) PrimaryGreen.copy(alpha = 0.3f) else PrimaryGreen)
                    .border(
                        width = 1.dp,
                        color = if (isManual) PrimaryGreen else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onToggleManual() }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isManual) "Done" else "Fill manually",
                    color = if (isManual) PrimaryGreen else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PillInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PillBackground)
            .border(1.dp, PillBorderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = Color.DarkGray,
                fontSize = 14.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}