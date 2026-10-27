package com.anay.fitnesstracker.data.repository


import com.anay.fitnesstracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object FoodLookupRepository {

    suspend fun fetchProductByBarcode(barcode: String): FoodItem? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "FitnessTrackerApp - Android - Version 1.0")
            }

            if (connection.responseCode == 200) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseString)
                val status = root.optInt("status", 0)

                if (status == 1) {
                    val product = root.getJSONObject("product")
                    val name = product.optString("product_name", "").ifBlank {
                        product.optString("product_name_en", "Scanned Food")
                    }

                    val nutriments = product.optJSONObject("nutriments")
                    val calories = nutriments?.optDouble("energy-kcal_100g", 0.0)
                        ?: nutriments?.optDouble("energy-kcal", 0.0) ?: 0.0
                    val protein = nutriments?.optDouble("proteins_100g", 0.0)
                        ?: nutriments?.optDouble("proteins", 0.0) ?: 0.0
                    val carbs = nutriments?.optDouble("carbohydrates_100g", 0.0)
                        ?: nutriments?.optDouble("carbohydrates", 0.0) ?: 0.0
                    val fat = nutriments?.optDouble("fat_100g", 0.0)
                        ?: nutriments?.optDouble("fat", 0.0) ?: 0.0

                    val servingSize = product.optString("serving_size", "100 g").ifBlank { "100 g" }

                    return@withContext FoodItem(
                        id = barcode,
                        name = name,
                        servingBasis = if (servingSize.contains("g", ignoreCase = true) || servingSize.contains("ml", ignoreCase = true)) "100 g" else servingSize,
                        calories = calories,
                        protein = protein,
                        carbohydrates = carbs,
                        fat = fat
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}