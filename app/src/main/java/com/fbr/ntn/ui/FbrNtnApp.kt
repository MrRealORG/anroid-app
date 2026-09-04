package com.fbr.ntn.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fbr.ntn.data.AppContainer
import com.fbr.ntn.ui.components.GlassBackground
import com.fbr.ntn.ui.screens.*
import com.fbr.ntn.viewmodel.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FbrNtnApp(container: AppContainer) {
    val model: AppViewModel = viewModel { AppViewModel(container.repository) }
    val state by model.state.collectAsState()
    GlassBackground {
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
                AppScreen.MOBILE -> state.account?.let { MobileScreen(it, state.otpSending, state.otpError, model::back, model::sendOtp) }
                AppScreen.OTP -> state.account?.let { OtpScreen(it, state.otpVerifying, state.otpVerified, state.otpError, state.resendSeconds, model::back, model::verifyOtp, model::resendOtp) }
                AppScreen.WEB_LOGIN -> state.loginUrl?.let { WebLoginScreen(it, state.webError, model::back, model::webFailed, model::retryWeb, model::completeLogin) }
                AppScreen.HOME -> HomeScreen(state.account, state.pendingItems, state.pendingLoading, state.pendingRefreshing, state.pendingError, { model.loadPending(true) }, { model.loadPending() })
            }
        }
    }
}
