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
import net.nicochristmann.revivetendo.admin.net.SystemMessage

/**
 * Shared UI for both /admin/api/v1/spotpass-wiiu and .../spotpass-3ds-sysmsg -
 * they're the same shape server-side (allWiiUSystemMessages/all3DSSystemMessages
 * share one Go struct), only the underlying table differs.
 */
@Composable
fun SystemMessageScreen(
    title: String,
    onBack: () -> Unit,
    list: suspend () -> List<SystemMessage>,
    add: suspend (subject: String, body: String, region: String?) -> List<SystemMessage>,
    toggle: suspend (id: Int) -> Unit,
    remove: suspend (id: Int) -> Unit,
) {
    var messages by remember { mutableStateOf<List<SystemMessage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var region by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                messages = withContext(Dispatchers.IO) { list() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SectionScaffold(title, onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(messages) { m ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(m.subject, style = MaterialTheme.typography.titleSmall)
                        Text(m.body)
                        Text("Region: ${m.region ?: "All"} · ${if (m.active) "Active" else "Inactive"}")
                        Row {
                            OutlinedButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { toggle(m.id) }
                                    refresh()
                                }
                            }) { Text(if (m.active) "Deactivate" else "Activate") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { remove(m.id) }
                                    refresh()
                                }
                            }) { Text("Delete") }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Send a new message", style = MaterialTheme.typography.titleMedium)
                LabeledField(subject, { subject = it }, "Subject", Modifier.fillMaxWidth())
                LabeledField(body, { body = it }, "Body", Modifier.fillMaxWidth())
                LabeledField(region, { region = it.uppercase() }, "Region (USA/EUR/JPN, blank = all)", Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        try {
                            messages = withContext(Dispatchers.IO) { add(subject, body, region.ifBlank { null }) }
                            subject = ""; body = ""; region = ""
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Send") }
            }
        }
    }
}

@Composable
fun SpotpassWiiUScreen(onBack: () -> Unit) = SystemMessageScreen(
    title = "SpotPass (Wii U)",
    onBack = onBack,
    list = { AdminApi.spotpassWiiU() },
    add = { s, b, r -> AdminApi.addSpotpassWiiU(s, b, r) },
    toggle = { AdminApi.toggleSpotpassWiiU(it) },
    remove = { AdminApi.removeSpotpassWiiU(it) },
)

@Composable
fun Spotpass3DSSysMsgScreen(onBack: () -> Unit) = SystemMessageScreen(
    title = "SpotPass (3DS system messages)",
    onBack = onBack,
    list = { AdminApi.spotpass3DSSysMsg() },
    add = { s, b, r -> AdminApi.addSpotpass3DSSysMsg(s, b, r) },
    toggle = { AdminApi.toggleSpotpass3DSSysMsg(it) },
    remove = { AdminApi.removeSpotpass3DSSysMsg(it) },
)
