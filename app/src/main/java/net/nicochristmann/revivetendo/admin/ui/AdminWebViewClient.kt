package net.nicochristmann.revivetendo.admin.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.webkit.ClientCertRequest
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import net.nicochristmann.revivetendo.admin.net.ApiClient

/**
 * WebViewClient for the single-site admin dashboard WebView. Presents this
 * app's own stored mTLS client cert to the platform's client-cert challenge,
 * keeps navigation scoped to the dashboard's own host, and routes any
 * connection/cert failure to [onCertProblem] instead of leaving a blank
 * broken WebView on screen.
 */
class AdminWebViewClient(
    private val context: Context,
    private val onCertProblem: (String) -> Unit,
) : WebViewClient() {
    private val allowedHost = Uri.parse(ApiClient.BASE_HOST).host

    // Dispatchers.Main.immediate so the terminal ClientCertRequest calls below
    // run on the UI thread as required, while the encrypted-file read + PKCS12
    // parse itself is hopped to Dispatchers.IO - real I/O that must not block
    // the UI thread. The platform suspends the connection while this callback
    // is pending, so this brief hop is safe, not a race.
    private val certScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        certScope.launch {
            val keyAndChain = withContext(Dispatchers.IO) { ClientCertStore.getKeyAndChain() }
            if (keyAndChain != null) {
                request.proceed(keyAndChain.first, keyAndChain.second)
            } else {
                // ignore(), not cancel(): WebView caches proceed()/cancel()
                // decisions per host:port and never re-invokes this callback
                // for that host once cancelled - a user who imports a cert
                // right after this would stay stuck. ignore() doesn't cache.
                request.ignore()
                onCertProblem("No certificate installed")
            }
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host == allowedHost) return false
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
            true
        } catch (e: ActivityNotFoundException) {
            // Nothing on-device can handle it - don't load it into the
            // authenticated dashboard session either.
            true
        }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        if (request.isForMainFrame) {
            onCertProblem("Couldn't connect (${error.errorCode}): ${error.description}")
        }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
        // Defensive: depending on Apache's SSLVerifyClient config, an
        // expired-but-still-presentable cert may surface as an HTTP 4xx from
        // the reverse proxy rather than failing at the TLS layer.
        if (request.isForMainFrame && response.statusCode in 400..499) {
            onCertProblem("Server rejected the request (HTTP ${response.statusCode})")
        }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        // Never proceed() - same "don't blindly trust" posture as
        // buildSslContext()'s default platform TrustManagerFactory.
        handler.cancel()
        onCertProblem("Server certificate problem: ${error.primaryError}")
    }
}
