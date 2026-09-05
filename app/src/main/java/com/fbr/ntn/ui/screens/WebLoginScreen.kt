package com.fbr.ntn.ui.screens

import android.annotation.SuppressLint
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fbr.ntn.BuildConfig
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebLoginScreen(url: String, failed: Boolean, onBack: () -> Unit, onFailure: () -> Unit, onRetry: () -> Unit, onComplete: (String) -> Unit) {
    var progress by remember { mutableIntStateOf(0) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler { if (webView?.canGoBack() == true) webView?.goBack() else onBack() }
    Box(Modifier.fillMaxSize().background(Paper)) {
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
        Column(Modifier.fillMaxWidth().background(CardWhite)) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(CardWhite).border(1.dp, Line, CircleShape)
                ) { Icon(Icons.Rounded.ArrowBack, "Go back", tint = Ink) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Secure sign in", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("Protected connection", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                }
            }
            HorizontalDivider(color = Line)
        }
        if (progress in 1..99 && !failed) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                Modifier.fillMaxWidth().statusBarsPadding().padding(top = 67.dp).height(4.dp),
                color = AccentDeep,
                trackColor = Line
            )
        }
        if (failed) {
            Box(Modifier.fillMaxSize().background(Paper), contentAlignment = Alignment.Center) {
                Card(Modifier.padding(24.dp), enterDelay = 100) {
                    Icon(Icons.Rounded.CloudOff, null, tint = InkMuted, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("Couldn't load sign in", style = MaterialTheme.typography.titleLarge, color = Ink)
                    Spacer(Modifier.height(6.dp))
                    Text("Check your connection and try again.", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    Spacer(Modifier.height(20.dp))
                    AccentButton("Try again") { onRetry(); webView?.reload() }
                }
            }
        }
    }
}
