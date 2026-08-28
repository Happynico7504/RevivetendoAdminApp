package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import net.nicochristmann.revivetendo.admin.net.Redirect

@Composable
fun RedirectsScreen(onBack: () -> Unit) {
    var redirects by remember { mutableStateOf<List<Redirect>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf("iosu") }
    var address by remember { mutableStateOf("") }
    var gameServerId by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var fromHost by remember { mutableStateOf("") }
    var toHost by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                redirects = withContext(Dispatchers.IO) { AdminApi.redirects() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SectionScaffold("Redirects", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(redirects) { r ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${r.from_host} → ${r.to_host}", style = MaterialTheme.typography.titleSmall)
                        Text("type=${r.type} mode=${r.access_mode}${r.game_server_id?.let { " game=$it" } ?: ""}")
                        Row {
                            Switch(checked = r.enabled, onCheckedChange = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AdminApi.toggleRedirect(r.id) }
                                    refresh()
                                }
                            })
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AdminApi.deleteRedirect(r.id) }
                                    refresh()
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Add redirect", style = MaterialTheme.typography.titleMedium)
                LabeledField(type, { type = it }, "Type (iosu/dns)", Modifier.fillMaxWidth())
                LabeledField(fromHost, { fromHost = it }, "From host", Modifier.fillMaxWidth())
                LabeledField(toHost, { toHost = it }, "To host", Modifier.fillMaxWidth())
                LabeledField(address, { address = it }, "Address (optional)", Modifier.fillMaxWidth())
                GameServerDropdown(gameServerId, { gameServerId = it }, "Game server (optional)", allowBlank = true, modifier = Modifier.fillMaxWidth())
                LabeledField(port, { port = it }, "Port (optional)", Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        try {
                            redirects = withContext(Dispatchers.IO) {
                                AdminApi.addRedirect(type, address.ifBlank { null }, gameServerId.ifBlank { null }, port.toIntOrNull(), fromHost, toHost)
                            }
                            fromHost = ""; toHost = ""; address = ""; gameServerId = ""; port = ""
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Add") }
            }
        }
    }
}
