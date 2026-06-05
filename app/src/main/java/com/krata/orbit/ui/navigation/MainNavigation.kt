package com.krata.orbit.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.krata.orbit.ui.budget.BudgetScreen
import com.krata.orbit.ui.coding.CodingScreen
import com.krata.orbit.ui.events.EventsScreen
import com.krata.orbit.ui.habits.HabitsScreen
import com.krata.orbit.ui.home.HomeScreen
import com.krata.orbit.ui.theme.AppTheme
import com.krata.orbit.ui.user.UserScreen
import com.krata.orbit.viewmodel.*

sealed class MainRoute(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Home    : MainRoute("home",    Icons.Filled.Home,                 Icons.Outlined.Home,                 "Home")
    object Events  : MainRoute("events",  Icons.Filled.Event,                Icons.Outlined.CalendarToday,        "Events")
    object Habits  : MainRoute("habits",  Icons.Filled.AutoAwesome,          Icons.Outlined.AutoAwesome,          "Habits")
    object Coding  : MainRoute("coding",  Icons.Filled.Code,                 Icons.Outlined.Code,                 "Coding")
    object Budget  : MainRoute("budget",  Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "Budget")
    object User    : MainRoute("user",    Icons.Filled.Person,               Icons.Outlined.PersonOutline,        "User")
}

private val tabs = listOf(
    MainRoute.Home,
    MainRoute.Events,
    MainRoute.Habits,
    MainRoute.Coding,
    MainRoute.Budget,
    MainRoute.User
)

@Composable
fun MainNavigation(
    startTab: String = MainRoute.Home.route,
    appTheme: AppTheme,
    appViewModel: AppViewModel,
    username: String = "",
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeViewModel:   HomeViewModel   = viewModel()
    val eventsViewModel: EventsViewModel = viewModel()
    val habitsViewModel: HabitsViewModel = viewModel()
    val codingViewModel: CodingViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController       = navController,
            startDestination    = startTab,
            modifier            = Modifier.fillMaxSize(),
            enterTransition     = { EnterTransition.None },
            exitTransition      = { ExitTransition.None },
            popEnterTransition  = { EnterTransition.None },
            popExitTransition   = { ExitTransition.None }
        ) {
            composable(MainRoute.Home.route) {
                HomeScreen(viewModel = homeViewModel, username = username)
            }
            composable(MainRoute.Events.route) {
                EventsScreen(viewModel = eventsViewModel)
            }
            composable(MainRoute.Habits.route) {
                HabitsScreen(viewModel = habitsViewModel)
            }
            composable(MainRoute.Coding.route) {
                CodingScreen(viewModel = codingViewModel)
            }
            composable(MainRoute.Budget.route) {
                BudgetScreen(viewModel = budgetViewModel)
            }
            composable(MainRoute.User.route) {
                UserScreen(appViewModel = appViewModel, currentTheme = appTheme)
            }
        }

        FloatingTabBar(
            tabs          = tabs,
            currentRoute  = currentRoute,
            onTabSelected = { route ->
                navController.navigate(route.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState    = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
    }
}

@Composable
private fun FloatingTabBar(
    tabs: List<MainRoute>,
    currentRoute: String?,
    onTabSelected: (MainRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.route
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer
                        else Color.Transparent
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = if (selected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.label,
                    tint               = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier           = Modifier.size(23.dp)
                )
            }
        }
    }
}
