package de.odpisz.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import de.odpisz.app.navigation.HistoryRoute
import de.odpisz.app.navigation.LetterRoute
import de.odpisz.app.navigation.OnboardingRoute
import de.odpisz.app.navigation.RedCardRoute
import de.odpisz.app.navigation.ReplyRoute
import de.odpisz.app.navigation.ReviewRoute
import de.odpisz.app.navigation.ScanRoute
import de.odpisz.app.navigation.SettingsRoute
import de.odpisz.core.designsystem.LoadingPane
import de.odpisz.core.designsystem.OdpiszScaffold
import de.odpisz.feature.settings.LockScreen
import de.odpisz.feature.history.HistoryRoute as HistoryScreen
import de.odpisz.feature.letter.LetterRoute as LetterScreen
import de.odpisz.feature.onboarding.OnboardingRoute as OnboardingScreen
import de.odpisz.feature.redcard.RedCardRoute as RedCardScreen
import de.odpisz.feature.reply.ReplyRoute as ReplyScreen
import de.odpisz.feature.review.ReviewRoute as ReviewScreen
import de.odpisz.feature.scan.ScanRoute as ScanScreen
import de.odpisz.feature.settings.SettingsRoute as SettingsScreen

@Composable
fun OdpiszApp(viewModel: AppGateViewModel = hiltViewModel()) {
    val gate by viewModel.gate.collectAsStateWithLifecycle()
    if (!gate.ready) {
        OdpiszScaffold(title = stringResource(R.string.app_name)) { padding ->
            LoadingPane(padding)
        }
        return
    }
    if (gate.locked) {
        LockScreen(onUnlocked = viewModel::unlock)
        return
    }
    val start =
        when (gate.start) {
            AppStart.Onboarding -> OnboardingRoute
            AppStart.History -> HistoryRoute
        }
    OdpiszNavHost(start = start)
}

@Composable
private fun OdpiszNavHost(start: Any) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val destination = entry?.destination
    val showBar =
        destination == null ||
            destination.hasRoute<HistoryRoute>() ||
            destination.hasRoute<ScanRoute>() ||
            destination.hasRoute<SettingsRoute>()
    val bar: @Composable () -> Unit = {
        if (showBar) {
            NavigationBar {
                NavigationBarItem(
                    selected = destination?.hasRoute<HistoryRoute>() == true,
                    onClick = {
                        nav.navigate(HistoryRoute) {
                            popUpTo(HistoryRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Email, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_letters)) },
                    modifier = Modifier.testTag("nav_letters"),
                )
                NavigationBarItem(
                    selected = destination?.hasRoute<ScanRoute>() == true,
                    onClick = {
                        nav.navigate(ScanRoute) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_scan)) },
                    modifier = Modifier.testTag("nav_scan"),
                )
                NavigationBarItem(
                    selected = destination?.hasRoute<SettingsRoute>() == true,
                    onClick = {
                        nav.navigate(SettingsRoute) { launchSingleTop = true }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_settings)) },
                    modifier = Modifier.testTag("nav_settings"),
                )
            }
        }
    }
    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onFinished = {
                    nav.navigate(HistoryRoute) {
                        popUpTo(OnboardingRoute) { inclusive = true }
                    }
                },
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(
                onOpen = { id -> nav.navigate(LetterRoute(id)) },
                bottomBar = bar,
            )
        }
        composable<ScanRoute> {
            val inbox = hiltViewModel<ImportHolderViewModel>()
            val pending by inbox.inbox.pendingUri.collectAsStateWithLifecycle()
            ScanScreen(
                onOpenReview = { id -> nav.navigate(ReviewRoute(id)) },
                bottomBar = bar,
                pendingUri = pending,
                onPendingConsumed = { inbox.inbox.pendingUri.value = null },
            )
        }
        composable<ReviewRoute> {
            ReviewScreen(
                onBack = { nav.popBackStack() },
                onAnalyzed = { id ->
                    nav.navigate(LetterRoute(id)) {
                        popUpTo<ScanRoute>()
                    }
                },
            )
        }
        composable<LetterRoute> { entry ->
            val route = entry.toRoute<LetterRoute>()
            LetterScreen(
                onBack = { nav.popBackStack() },
                onReply = { nav.navigate(ReplyRoute(it)) },
                onRedCard = { nav.navigate(RedCardRoute(route.letterId)) },
            )
        }
        composable<ReplyRoute> { entry ->
            val route = entry.toRoute<ReplyRoute>()
            ReplyScreen(
                onBack = { nav.popBackStack() },
                onBlocked = { nav.navigate(RedCardRoute(it)) },
                letterId = route.letterId,
            )
        }
        composable<RedCardRoute> {
            RedCardScreen(onBack = { nav.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onWiped = {
                    nav.navigate(OnboardingRoute) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                bottomBar = bar,
            )
        }
    }
}

@dagger.hilt.android.lifecycle.HiltViewModel
class ImportHolderViewModel
    @javax.inject.Inject
    constructor(
        val inbox: ImportInbox,
    ) : androidx.lifecycle.ViewModel()
