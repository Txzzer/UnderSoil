package com.example.undersoil.data

data class Plant(
    val id: Int,
    val name: String,
    val scientificName: String,
    val description: String,
    val waterAmount: String,
    val sunlight: String,
    val fertilizer: String,
    val humidity: String,
    val imageResId: Int = android.R.drawable.ic_menu_gallery // Placeholder icon
)

object PlantVillageData {
    val catalog = listOf(
        Plant(1, "Tomato", "Solanum lycopersicum", "Tomatoes add elegance to indoor spaces and carry a positive symbolic message.", "250 ml", "Normal", "70 ml", "54%"),
        Plant(2, "Potato", "Solanum tuberosum", "Potatoes are versatile root vegetables that thrive in loose, well-drained soil.", "200 ml", "High", "50 ml", "40%"),
        Plant(3, "Apple", "Malus domestica", "Apple trees require careful pruning and consistent watering to produce high yields.", "300 ml", "High", "100 ml", "50%"),
        Plant(4, "Corn", "Zea mays", "Corn is a staple cereal crop that requires significant space and full sun.", "400 ml", "High", "80 ml", "45%"),
        Plant(5, "Grape", "Vitis vinifera", "Grapes grow on vigorous vines and need strong trellising for support.", "150 ml", "High", "40 ml", "60%"),
        Plant(6, "Blueberry", "Cyanococcus", "Blueberries require highly acidic soil to properly absorb nutrients.", "200 ml", "High", "30 ml", "60%"),
        Plant(7, "Cherry", "Prunus avium", "Cherry trees produce sweet stone fruits and prefer well-drained soil.", "250 ml", "High", "60 ml", "50%"),
        Plant(8, "Orange", "Citrus × sinensis", "Oranges are evergreen citrus trees sensitive to frost.", "300 ml", "High", "90 ml", "55%"),
        Plant(9, "Peach", "Prunus persica", "Peaches prefer temperate climates and require winter chilling hours.", "250 ml", "High", "70 ml", "50%"),
        Plant(10, "Pepper", "Capsicum annuum", "Peppers thrive in warm weather and produce better yields with consistent moisture.", "200 ml", "High", "60 ml", "45%"),
        Plant(11, "Raspberry", "Rubus idaeus", "Raspberries are perennial with woody stems that need regular pruning.", "150 ml", "Normal", "40 ml", "55%"),
        Plant(12, "Soybean", "Glycine max", "Soybeans are legumes that naturally fix nitrogen into the soil.", "300 ml", "High", "50 ml", "60%"),
        Plant(13, "Squash", "Cucurbita", "Squash grow on sprawling vines and require deep, infrequent watering.", "350 ml", "High", "80 ml", "50%"),
        Plant(14, "Strawberry", "Fragaria × ananassa", "Strawberries are low-growing perennials that multiply via runners.", "150 ml", "Normal", "30 ml", "60%")
    )
}