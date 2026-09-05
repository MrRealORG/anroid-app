package com.fbr.ntn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fbr.ntn.data.AppContainer
import com.fbr.ntn.ui.components.AppBackground
import com.fbr.ntn.ui.screens.*
import com.fbr.ntn.ui.theme.ThemeMode
import com.fbr.ntn.viewmodel.*

@Composable
fun FbrNtnApp(container: AppContainer) {
    val model: AppViewModel = viewModel { AppViewModel(container.repository, container.settings) }
    val state by model.state.collectAsState()
    val systemDark = isSystemInDarkTheme()
    LaunchedEffect(state.themeMode, systemDark) {
        ThemeMode.dark = when (state.themeMode) {
            "light" -> false
            "dark" -> true
            else -> systemDark
        }
    }
    AppBackground {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                (slideInHorizontally(spring()) { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally(spring()) { -it / 4 } + fadeOut())
            },
            label = "screen transition"
        ) { screen ->
            when (screen) {
                AppScreen.SPLASH -> SplashScreen()
                AppScreen.NTN -> NtnScreen(state.ntnLoading, state.ntnError, model::checkNtn)
                AppScreen.PIN -> PinScreen(
                    state.ntn,
                    state.pinLoading,
                    state.pinError,
                    model::switchAccount,
                    model::verifyPin
                )
                AppScreen.CONNECT -> ConnectScreen(
                    state.connectLoading,
                    state.connectError,
                    model::switchAccount,
                    model::connect
                )
                AppScreen.HOME -> HomeScreen(
                    state.account, state.pendingItems, state.pendingLoading, state.pendingRefreshing,
                    state.pendingError, { model.loadPending(true) }, { model.loadPending() }, model::openInvoice, model::lock,
                    model::validateInvoice, model::postInvoice, model::openSettings
                )
                AppScreen.DETAIL -> {
                    val invoice = state.pendingItems.find { it.id == state.selectedId }
                    if (invoice != null) InvoiceDetailScreen(invoice, model::closeDetail, { model.validateInvoice(invoice.id) }, { model.postInvoice(invoice.id) })
                }
                AppScreen.SETTINGS -> SettingsScreen(
                    state.themeMode, state.soundsEnabled, state.account?.displayName,
                    model::setThemeMode, model::setSoundsEnabled, model::closeSettings, model::switchAccount
                )
                AppScreen.LOCK -> LockScreen(
                    container.repository.savedUsername(),
                    state.account?.displayName,
                    state.connectLoading,
                    state.connectError,
                    model::unlock,
                    model::switchAccount
                )
            }
        }
    }
}
