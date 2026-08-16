package com.istidblip.pengehubben

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.istidblip.pengehubben.auth.AuthRepository
import com.istidblip.pengehubben.ui.LoginScreen
import com.istidblip.pengehubben.ui.ModularDashboard
import com.istidblip.pengehubben.ui.StockSearch
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable
sealed interface Route

@Serializable
data object LoginRoute : Route

@Serializable
data object DashboardRoute : Route

@Serializable
data object SearchRoute : Route

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            println("APP_START: !!! THE_FORCE_IS_STRONG_WITH_THIS_ONE !!!")
            val authRepository = remember { AuthRepository() }
            val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)

            val navController = rememberNavController()
            val scope = rememberCoroutineScope()

            LaunchedEffect(isAuthenticated) {
                if (isAuthenticated) {
                    navController.navigate(DashboardRoute) {
                        popUpTo(LoginRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(LoginRoute) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = if (isAuthenticated) DashboardRoute else LoginRoute,
            ) {
                composable<LoginRoute> {
                    LoginScreen(
                        onSignIn = { email, pass -> authRepository.signIn(email, pass) },
                        onSignUp = { email, pass -> authRepository.signUp(email, pass) },
                        onLoginSuccess = {
                            navController.navigate(DashboardRoute) {
                                popUpTo(LoginRoute) { inclusive = true }
                            }
                        },
                    )
                }
                composable<DashboardRoute> {
                    val viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
                    ModularDashboard(
                        viewModel = viewModel,
                        onNavigateToSearch = { navController.navigate(SearchRoute) },
                        onLogout = {
                            scope.launch {
                                authRepository.signOut()
                            }
                        }
                    )
                }
                composable<SearchRoute> {
                    val dashboardEntry = remember(it) {
                        navController.getBackStackEntry(DashboardRoute)
                    }
                    val viewModel: DashboardViewModel = viewModel(dashboardEntry) { DashboardViewModel() }
                    StockSearch(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
