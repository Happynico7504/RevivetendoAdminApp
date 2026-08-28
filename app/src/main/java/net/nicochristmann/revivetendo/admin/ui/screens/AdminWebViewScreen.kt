package net.nicochristmann.revivetendo.admin.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import net.nicochristmann.revivetendo.admin.net.ApiClient
import net.nicochristmann.revivetendo.admin.ui.AdminWebViewClient

/**
 * Hosts the real relay-admin dashboard (the same HTML a browser gets at
 * ApiClient.ADMIN_BASE) inside a WebView, mTLS-authenticated via
 * AdminWebViewClient. This is the app's whole UI now beyond cert import -
 * every relay-admin feature/fix shows up here automatically, no app rebuild.
 */
@Composable
fun AdminWebViewScreen(onCertProblem: (String) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }
    }

    // Activity-level lifecycle (app backgrounded/foregrounded), not
    // composition lifecycle - see the onRelease note on AndroidView below for
    // why cleanup lives there instead.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webView.onResume()
                    // Pick up a cert renewed/reimported while backgrounded
                    // instead of reusing WebView's cached per-host:port
                    // client-cert decision from before the change.
                    webView.clearClientCertPreferences {}
                }
                Lifecycle.Event.ON_PAUSE -> webView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        factory = {
            webView.apply {
                webViewClient = AdminWebViewClient(context, onCertProblem)
                loadUrl("${ApiClient.ADMIN_BASE}/")
            }
        },
        modifier = Modifier.fillMaxSize(),
        // Fires when this composable leaves composition - unlike
        // DisposableEffect(lifecycleOwner), which wouldn't fire destroy()
        // here at all: navigating away inside the single-Activity NavHost
        // doesn't trigger ON_DESTROY on the Activity-scoped lifecycle, since
        // the Activity itself is still alive.
        onRelease = { it.destroy() },
    )

    BackHandler(enabled = true) {
        // Evaluate canGoBack() fresh at press time, not once at composition
        // time - there's no Compose state hook for WebView's back/forward
        // list, so a captured value would go stale.
        if (webView.canGoBack()) webView.goBack() else onExit()
    }
}
