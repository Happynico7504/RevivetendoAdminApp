package net.nicochristmann.revivetendo.admin.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import net.nicochristmann.revivetendo.admin.net.ApiClient

@Composable
fun ImportCertScreen(isReimport: Boolean = false, onImported: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        importing = true
        error = null
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ClientCertStore.importPkcs12(stream.readBytes())
                    } ?: throw IllegalStateException("Couldn't open the selected file.")
                }
                ApiClient.invalidateClient()
                importing = false
                onImported()
            } catch (e: ClientCertStore.InvalidCertException) {
                importing = false
                error = e.message
            } catch (e: Exception) {
                importing = false
                error = "Import failed: ${e.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isReimport) "Replace admin certificate" else "No admin certificate installed",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Fetch the current inkay-admin.p12 from the dashboard (Admin → " +
                    "Download client cert) on a device that already has a valid cert, " +
                    "transfer it to this phone, then import it below. The app renews " +
                    "it automatically on its own from then on.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = { pickFile.launch(arrayOf("*/*")) }, enabled = !importing) {
                Text(if (importing) "Importing…" else "Import certificate (.p12)")
            }
            error?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
