package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.RadioButton
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
import net.nicochristmann.revivetendo.admin.net.AccessLevelEntry
import net.nicochristmann.revivetendo.admin.net.AdminApi

@Composable
fun AccessScreen(onBack: () -> Unit) {
    var entries by remember { mutableStateOf<List<AccessLevelEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var pid by remember { mutableStateOf("") }
    var level by remember { mutableStateOf(2) }
    var note by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                entries = withContext(Dispatchers.IO) { AdminApi.access() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SectionScaffold("Access levels", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(entries) { e ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(e.pnid ?: e.pid.toString(), style = MaterialTheme.typography.titleSmall)
                        Text("Level ${e.access_level}${e.note?.let { " · $it" } ?: ""}")
                        OutlinedButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { AdminApi.removeAccess(e.pid) }
                                refresh()
                            }
                        }) { Text("Remove") }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Grant / update access level", style = MaterialTheme.typography.titleMedium)
                LabeledField(pid, { pid = it }, "PID", Modifier.fillMaxWidth())
                Row {
                    RadioButton(selected = level == 2, onClick = { level = 2 })
                    Text("2 · moderator", modifier = Modifier.padding(top = 12.dp, end = 12.dp))
                    RadioButton(selected = level == 3, onClick = { level = 3 })
                    Text("3 · developer", modifier = Modifier.padding(top = 12.dp))
                }
                LabeledField(note, { note = it }, "Note (optional)", Modifier.fillMaxWidth())
                Button(onClick = {
                    val pidLong = pid.toLongOrNull()
                    if (pidLong == null) {
                        error = "Invalid PID"
                        return@Button
                    }
                    scope.launch {
                        try {
                            entries = withContext(Dispatchers.IO) { AdminApi.setAccess(pidLong, level, note.ifBlank { null }) }
                            pid = ""; note = ""
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                }) { Text("Save") }
            }
        }
    }
}
