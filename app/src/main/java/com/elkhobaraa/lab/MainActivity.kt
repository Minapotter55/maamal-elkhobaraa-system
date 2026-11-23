package com.elkhobaraa.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.elkhobaraa.lab.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LabTheme {
                var splash by rememberSaveable { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1600)
                    splash = false
                }
                if (splash) SplashScreen() else AppNav()
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("patients") { PatientsScreen(nav) }
        composable("tests") { TestsScreen(nav) }
        composable("orders") { OrdersScreen(nav) }
        composable(
            "neworder?patientId={patientId}",
            arguments = listOf(navArgument("patientId") { type = NavType.LongType; defaultValue = 0L }),
        ) { NewOrderScreen(nav, it.arguments?.getLong("patientId") ?: 0L) }
        composable("order/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            OrderDetailScreen(nav, it.arguments?.getLong("id") ?: 0L)
        }
        composable("results") { ResultsListScreen(nav) }
        composable("results/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            ResultsScreen(nav, it.arguments?.getLong("id") ?: 0L)
        }
        composable("accounts") { AccountsScreen(nav) }
        composable("settings") { SettingsScreen(nav) }
    }
}
