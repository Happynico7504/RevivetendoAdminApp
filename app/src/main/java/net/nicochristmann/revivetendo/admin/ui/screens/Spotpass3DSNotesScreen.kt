package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import net.nicochristmann.revivetendo.admin.net.SwapdoodleNote

@Composable
fun Spotpass3DSNotesScreen(onBack: () -> Unit) {
    var notes by remember { mutableStateOf<List<SwapdoodleNote>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                notes = withContext(Dispatchers.IO) { AdminApi.spotpass3DSNotes() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    SectionScaffold("SpotPass (3DS notes)", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(notes) { n ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("#${n.data_id} · ${n.size} bytes", style = MaterialTheme.typography.titleSmall)
                        Text("From: ${n.owner_pnid ?: n.owner_pid}")
                        Text("To: ${n.recipient_pnid ?: n.recipient_pid ?: "—"}")
                        Text("${if (n.upload_completed) "Completed" else "Pending"} · ${if (n.read) "Received" else "Not yet"}")
                    }
                }
            }
        }
    }
}
