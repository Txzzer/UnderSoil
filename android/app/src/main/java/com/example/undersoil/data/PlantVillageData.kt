package com.example.undersoil.data

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.example.undersoil.R

/** A growing condition shown on plant cards, with the same icon styling as the plant details screen. */
enum class GrowingCondition(
    val label: String,
    @DrawableRes val iconResId: Int,
    @DrawableRes val backgroundResId: Int,
    @ColorRes val tintResId: Int
) {
    WATER("Water", R.drawable.ic_drop, R.drawable.bg_square_water, R.color.us_aqua),
    SUNLIGHT("Sunlight", R.drawable.ic_sun, R.drawable.bg_square_sunlight, R.color.us_violet),
    FERTILIZER("Fertilizer", R.drawable.ic_leaf_scan, R.drawable.bg_square_fertilizer, R.color.us_danger),
    HUMIDITY("Humidity", R.drawable.ic_humidity, R.drawable.bg_square_humidity, R.color.us_amber)
}

data class Plant(
    val id: Int,
    val name: String,
    val scientificName: String,
    val description: String,
    val waterAmount: String,
    val sunlight: String,
    val fertilizer: String,
    val humidity: String,
    val imageResId: Int = android.R.drawable.ic_menu_gallery, // Placeholder icon
    /** The two conditions this crop depends on most, shown as icons on its garden card. */
    val favouredConditions: List<GrowingCondition> = listOf(GrowingCondition.WATER, GrowingCondition.SUNLIGHT)
)

object PlantVillageData {
    val catalog = listOf(
        Plant(1, "Tomato", "Solanum lycopersicum", "Tomatoes add elegance to indoor spaces and carry a positive symbolic message.", "250 ml", "Normal", "70 ml", "54%", R.drawable.tomato,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.FERTILIZER)),
        Plant(2, "Potato", "Solanum tuberosum", "Potatoes are versatile root vegetables that thrive in loose, well-drained soil.", "200 ml", "High", "50 ml", "40%", R.drawable.potato,
            listOf(GrowingCondition.WATER, GrowingCondition.FERTILIZER)),
        Plant(3, "Apple", "Malus domestica", "Apple trees require careful pruning and consistent watering to produce high yields.", "300 ml", "High", "100 ml", "50%", R.drawable.apple,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.WATER)),
        Plant(4, "Corn", "Zea mays", "Corn is a staple cereal crop that requires significant space and full sun.", "400 ml", "High", "80 ml", "45%", R.drawable.corn,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.WATER)),
        Plant(5, "Grape", "Vitis vinifera", "Grapes grow on vigorous vines and need strong trellising for support.", "150 ml", "High", "40 ml", "60%", R.drawable.grape,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.HUMIDITY)),
        Plant(7, "Cherry", "Prunus avium", "Cherry trees produce sweet stone fruits and prefer well-drained soil.", "250 ml", "High", "60 ml", "50%", R.drawable.cherry,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.WATER)),
        Plant(9, "Peach", "Prunus persica", "Peaches prefer temperate climates and require winter chilling hours.", "250 ml", "High", "70 ml", "50%", R.drawable.peach,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.FERTILIZER)),
        Plant(10, "Pepper", "Capsicum annuum", "Peppers thrive in warm weather and produce better yields with consistent moisture.", "200 ml", "High", "60 ml", "45%", R.drawable.pepper_bell,
            listOf(GrowingCondition.SUNLIGHT, GrowingCondition.WATER)),
        Plant(14, "Strawberry", "Fragaria × ananassa", "Strawberries are low-growing perennials that multiply via runners.", "150 ml", "Normal", "30 ml", "60%", R.drawable.strawberry,
            listOf(GrowingCondition.WATER, GrowingCondition.HUMIDITY))
    )
}