package com.example.undersoil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.example.undersoil.data.GardenDatabase
import com.example.undersoil.ui.GardenViewModel
import com.example.undersoil.ui.screens.ExplorePlantsScreen
import com.example.undersoil.ui.screens.MyGardenScreen
import com.example.undersoil.ui.screens.PlantDetailsScreen

class MainActivity : ComponentActivity() {

    private val targetRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetRouteState.value = intent?.getStringExtra("target_route")

        val db = Room.databaseBuilder(
            applicationContext,
            GardenDatabase::class.java, "garden-database"
        ).build()

        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GardenViewModel(db.gardenDao()) as T
            }
        }

        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val viewModel: GardenViewModel = viewModel(factory = factory)
                MainScreen(viewModel, targetRouteState)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("target_route")?.let { route ->
            targetRouteState.value = route
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Garden : Screen("garden", "Garden", Icons.Default.Eco)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object IoT : Screen("iot", "IoT", Icons.Default.Info)
}

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to UnderSoil",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Home Overview Placeholder",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun MainScreen(viewModel: GardenViewModel, targetRouteState: State<String?> = mutableStateOf(null)) {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current

    val items = listOf(
        Screen.Garden,
        Screen.Home,
        Screen.IoT
    )

    val targetRoute by targetRouteState
    LaunchedEffect(targetRoute) {
        targetRoute?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            (targetRouteState as? MutableState<String?>)?.value = null
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in items.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                if (screen == Screen.IoT) {
                                    context.startActivity(Intent(context, IoTScreen::class.java))
                                } else {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Garden.route, Modifier.padding(innerPadding)) {
            composable(Screen.Garden.route) {
                MyGardenScreen(
                    viewModel = viewModel,
                    onPlantClick = { plantId ->
                        navController.navigate("details/$plantId")
                    },
                    onSearchClick = {
                        navController.navigate("explore")
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen()
            }
            composable("explore") {
                ExplorePlantsScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        navController.navigateUp()
                    },
                    onPlantClick = { plantId ->
                        navController.navigate("details/$plantId")
                    }
                )
            }
            composable("details/{plantId}") { backStackEntry ->
                val plantId = backStackEntry.arguments?.getString("plantId")?.toIntOrNull()
                if (plantId != null) {
                    PlantDetailsScreen(
                        plantId = plantId,
                        viewModel = viewModel,
                        onBackClick = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}
