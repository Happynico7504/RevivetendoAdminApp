package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import net.nicochristmann.revivetendo.admin.net.AdminApi
import net.nicochristmann.revivetendo.admin.net.BannedUser

@Composable
fun BansScreen(onBack: () -> Unit) {
    var bans by remember { mutableStateOf<List<BannedUser>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pid by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                bans = withContext(Dispatchers.IO) { AdminApi.bans() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SectionScaffold("Bans", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(bans) { b ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(b.pnid ?: b.pid.toString(), style = MaterialTheme.typography.titleSmall)
                        Text(b.reason ?: "No reason given")
                        OutlinedButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { AdminApi.removeBan(b.pid) }
                                refresh()
                            }
                        }) { Text("Unban") }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Ban user", style = MaterialTheme.typography.titleMedium)
                LabeledField(pid, { pid = it }, "PID", Modifier.fillMaxWidth())
                LabeledField(reason, { reason = it }, "Reason (optional)", Modifier.fillMaxWidth())
                Button(onClick = {
                    val pidLong = pid.toLongOrNull()
                    if (pidLong == null) {
                        error = "Invalid PID"
                        return@Button
                    }
                    scope.launch {
                        try {
                            bans = withContext(Dispatchers.IO) { AdminApi.addBan(pidLong, reason.ifBlank { null }) }
                            pid = ""; reason = ""
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Ban") }
            }
        }
    }
}
