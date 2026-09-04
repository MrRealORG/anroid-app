package com.fbr.ntn.ui.screens

import android.annotation.SuppressLint
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.fbr.ntn.BuildConfig
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginScreen(url: String, failed: Boolean, onBack: () -> Unit, onFailure: () -> Unit, onRetry: () -> Unit, onComplete: (String) -> Unit) {
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler { if (webView?.canGoBack() == true) webView?.goBack() else onBack() }
    Box(Modifier.fillMaxSize().background(Color(0xFFF7F8FC))) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true // Required by the server-hosted authentication page.
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webChromeClient = object : WebChromeClient() { override fun onProgressChanged(view: WebView?, newProgress: Int) { progress = newProgress } }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val uri = request.url
                            if (uri.scheme == BuildConfig.AUTH_CALLBACK_SCHEME && uri.host == BuildConfig.AUTH_CALLBACK_HOST && uri.path == "/complete") {
                                onComplete(uri.getQueryParameter("token") ?: "cookie-session")
                                return true
                            }
                            return false
                        }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) { if (request.isForMainFrame) onFailure() }
                    }
                    loadUrl(url)
                }
            },
            update = { webView = it },
            modifier = Modifier.fillMaxSize().padding(top = 92.dp)
        )
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).glass(RoundedCornerShape(0.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Go back", tint = Ink) }
            Column { Text("Secure sign in", style = MaterialTheme.typography.titleLarge, color = Ink); Text("Protected connection", style = MaterialTheme.typography.labelMedium, color = InkMuted) }
        }
        if (progress in 1..99 && !failed) LinearProgressIndicator(progress = { progress / 100f }, Modifier.fillMaxWidth().statusBarsPadding().padding(top = 67.dp).height(3.dp), color = AccentBlue, trackColor = Color.Transparent)
        if (failed) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF4F6FC)), contentAlignment = Alignment.Center) {
                GlassCard(Modifier.padding(24.dp)) {
                    Icon(Icons.Rounded.CloudOff, null, tint = InkMuted, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(14.dp))
                    Text("Couldn't load sign in", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(6.dp))
                    Text("Check your connection and try again.", style = MaterialTheme.typography.bodyMedium, color = InkMuted); Spacer(Modifier.height(20.dp))
                    PrimaryButton("Try again") { onRetry(); webView?.reload() }
                }
            }
        }
    }
}
