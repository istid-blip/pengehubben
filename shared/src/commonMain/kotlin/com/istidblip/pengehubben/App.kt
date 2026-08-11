package com.istidblip.pengehubben

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.istidblip.pengehubben.auth.AuthRepository
import com.istidblip.pengehubben.ui.LoginScreen
import com.istidblip.pengehubben.ui.ModularDashboard
import com.istidblip.pengehubben.ui.StockSearch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.ui.tooling.preview.Preview

@Serializable
sealed interface Route : NavKey

@Serializable
data object LoginRoute : Route

@Serializable
data object DashboardRoute : Route

@Serializable
data object SearchRoute : Route

private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(LoginRoute::class, LoginRoute.serializer())
            subclass(DashboardRoute::class, DashboardRoute.serializer())
            subclass(SearchRoute::class, SearchRoute.serializer())
        }
    }
}

@Composable
@Preview
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val authRepository = remember { AuthRepository() }
            val isAuthenticated by authRepository.isAuthenticated.collectAsState(initial = false)

            val viewModel: DashboardViewModel = viewModel { DashboardViewModel() }
            val backStack = rememberNavBackStack(config, if (isAuthenticated) DashboardRoute else LoginRoute)

            LaunchedEffect(isAuthenticated) {
                if (!isAuthenticated) {
                    // Navigate to Login if not authenticated
                    if (backStack.all { it !is LoginRoute }) {
                        while (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                        backStack.add(LoginRoute)
                    }
                }
            }

            NavDisplay(
                backStack = backStack
            ) { key ->
                NavEntry(key) { k: NavKey ->
                    when (k) {
                        is LoginRoute -> LoginScreen(
                            onSignIn = { email, pass -> authRepository.signIn(email, pass) },
                            onSignUp = { email, pass -> authRepository.signUp(email, pass) },
                            onLoginSuccess = { backStack.add(DashboardRoute) }
                        )
                        is DashboardRoute -> ModularDashboard(
                            viewModel = viewModel,
                            onNavigateToSearch = { backStack.add(SearchRoute) }
                        )
                        is SearchRoute -> StockSearch(
                            viewModel = viewModel,
                            onBack = { backStack.removeAt(backStack.size - 1) }
                        )
                    }
                }
            }
        }
    }
}
