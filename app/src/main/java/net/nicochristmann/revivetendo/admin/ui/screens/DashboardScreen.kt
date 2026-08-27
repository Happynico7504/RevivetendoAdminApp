package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.nicochristmann.revivetendo.admin.cert.CertRenewalWorker
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import net.nicochristmann.revivetendo.admin.net.CertStatusApi
import net.nicochristmann.revivetendo.admin.net.CertStatusData

data class AdminSection(val title: String, val route: String)

val adminSections = listOf(
    AdminSection("Redirects", "redirects"),
    AdminSection("Game whitelist", "users"),
    AdminSection("Bans", "bans"),
    AdminSection("Access levels", "access"),
    AdminSection("SpotPass (Wii U)", "spotpass-wiiu"),
    AdminSection("SpotPass (3DS notes)", "spotpass-3ds"),
    AdminSection("SpotPass (3DS system messages)", "spotpass-3ds-sysmsg"),
    AdminSection("Review queue", "review"),
)

private sealed class CertState {
    object Loading : CertState()
    data class Loaded(val status: CertStatusData) : CertState()
    data class Failed(val message: String) : CertState()
}

@Composable
fun DashboardScreen(onOpenSection: (String) -> Unit, onReimport: () -> Unit) {
    var certState by remember { mutableStateOf<CertState>(CertState.Loading) }
    var renewing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        certState = try {
            CertState.Loaded(withContext(Dispatchers.IO) { CertStatusApi.fetch() })
        } catch (e: Exception) {
            CertState.Failed(e.message ?: "Unknown error")
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text("Revivetendo Admin") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Certificate status", style = MaterialTheme.typography.titleMedium)
                        val localInfo = ClientCertStore.getCertInfo()
                        Text("Days remaining (local): ${localInfo?.daysRemaining() ?: "—"}")
                        when (val s = certState) {
                            is CertState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                            is CertState.Loaded -> {
                                Text("Expires: ${s.status.expiresAt ?: "—"}")
                                Text("Days until next rotation: ${s.status.daysUntilRotation ?: "—"}")
                            }
                            is CertState.Failed -> Text(
                                "Couldn't reach server: ${s.message}",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = !renewing,
                                onClick = {
                                    renewing = true
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) { CertRenewalWorker.renewNow() }
                                            refresh()
                                        } catch (e: Exception) {
                                            certState = CertState.Failed(e.message ?: "Renewal failed")
                                        } finally {
                                            renewing = false
                                        }
                                    }
                                },
                            ) { Text(if (renewing) "Renewing…" else "Renew now") }
                            OutlinedButton(onClick = onReimport) { Text("Replace certificate") }
                        }
                    }
                }
            }
            items(adminSections) { section ->
                ListItem(
                    headlineContent = { Text(section.title) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSection(section.route) },
                )
            }
        }
    }
}
