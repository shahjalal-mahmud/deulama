package com.appriyo.deulama.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.appriyo.deulama.data.remote.AuthEvent
import com.appriyo.deulama.data.remote.AuthEventBus
import com.appriyo.deulama.presentation.activity.ActivityScreen
import com.appriyo.deulama.presentation.auth.AuthUiState
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.presentation.auth.LoginScreen
import com.appriyo.deulama.presentation.auth.RegisterScreen
import com.appriyo.deulama.presentation.details.DramaDetailsScreen
import com.appriyo.deulama.presentation.discover.DiscoverScreen
import com.appriyo.deulama.presentation.home.HomeScreen
import com.appriyo.deulama.presentation.profile.EditProfileScreen
import com.appriyo.deulama.presentation.profile.ProfileScreen
import com.appriyo.deulama.presentation.recommendations.RecommendationsScreen
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/** Routes that should show the bottom bar. */
private val bottomBarRoutes = BottomTab.entries.map { it.route::class }

@Composable
fun HangugNavGraph(
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    when (authState) {
        AuthUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = HangugColors.Primary)
        }
        else -> HangugNavGraphLoaded(
            authState = authState,
            authViewModel = authViewModel,
        )
    }
}

@Composable
private fun HangugNavGraphLoaded(
    authState: AuthUiState,
    authViewModel: AuthViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val showBottomBar = bottomBarRoutes.any { routeClass ->
        currentDestination?.hierarchy?.any { it.hasRoute(routeClass) } == true
    }

    // Single subscriber to the global auth event bus. AuthInterceptor
    // fires SessionExpired on a 401 of an authenticated request.
    LaunchedEffect(Unit) {
        AuthEventBus.events.collect { event ->
            if (event is AuthEvent.SessionExpired) {
                authViewModel.logout()
                navController.navigateToAuthGraph()
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HangugBottomBar(navController = navController, currentDestination = currentDestination)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (authState is AuthUiState.SignedIn) {
                HangugRoute.Home
            } else {
                HangugRoute.Login
            },
            modifier = Modifier.padding(innerPadding),
        ) {
            // ---- Auth graph ----
            composable<HangugRoute.Login> {
                LoginScreen(
                    onLoginSuccess = { navController.navigateToMainGraph() },
                    onContinueWithoutAccount = { navController.navigateToMainGraph() },
                    onGoToRegister = { navController.navigate(HangugRoute.Register) },
                )
            }
            composable<HangugRoute.Register> {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigateToMainGraph() },
                    onGoToLogin = { navController.popBackStack() },
                )
            }

            // ---- Main graph ----
            composable<HangugRoute.Home> {
                HomeScreen(
                    onOpenDiscover = { navController.navigate(HangugRoute.Discover) },
                    onOpenDramaDetails = { id -> navController.navigate(HangugRoute.DramaDetails(id)) },
                )
            }
            composable<HangugRoute.Discover> {
                DiscoverScreen(
                    onOpenDramaDetails = { id -> navController.navigate(HangugRoute.DramaDetails(id)) },
                )
            }
            composable<HangugRoute.Recommendations> {
                RecommendationsScreen(
                    onOpenDramaDetails = { id -> navController.navigate(HangugRoute.DramaDetails(id)) },
                )
            }
            composable<HangugRoute.Activity> {
                ActivityScreen(
                    onGoToLogin = { navController.navigateToAuthGraph() },
                )
            }
            composable<HangugRoute.Profile> {
                ProfileScreen(
                    onOpenEditProfile = { navController.navigate(HangugRoute.EditProfile) },
                    onLogout = {
                        authViewModel.logout()
                        navController.navigateToAuthGraph()
                    },
                )
            }

            // ---- Pushed routes ----
            composable<HangugRoute.DramaDetails> { entry ->
                val args = entry.toRoute<HangugRoute.DramaDetails>()
                DramaDetailsScreen(
                    dramaId = args.id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<HangugRoute.EditProfile> {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun HangugBottomBar(
    navController: androidx.navigation.NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Standard nav-compose "bottom nav" pattern: pop up to the
                        // graph's start destination to avoid a huge back stack,
                        // save/restore state so switching tabs doesn't lose scroll
                        // position or re-trigger first-composition work.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            BottomTab.HOME -> Icons.Filled.Home
                            BottomTab.DISCOVER -> Icons.Filled.Explore
                            BottomTab.RECOMMENDATIONS -> Icons.Filled.Favorite
                            BottomTab.ACTIVITY -> Icons.Filled.History
                            BottomTab.PROFILE -> Icons.Filled.AccountCircle
                        },
                        contentDescription = tab.label,
                    )
                },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
            )
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToMainGraph() {
    navigate(HangugRoute.Home) {
        popUpTo(HangugRoute.Login) { inclusive = true }
    }
}

private fun androidx.navigation.NavHostController.navigateToAuthGraph() {
    navigate(HangugRoute.Login) {
        popUpTo(graph.findStartDestination().id) { inclusive = true }
    }
}