package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import net.nicochristmann.revivetendo.admin.net.ReviewEntry

@Composable
fun ReviewScreen(onBack: () -> Unit) {
    val titles = rememberGameTitles()
    var entries by remember { mutableStateOf<List<ReviewEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                entries = withContext(Dispatchers.IO) { AdminApi.review() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    SectionScaffold("Review queue", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            items(entries) { e ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(e.pnid ?: e.pid.toString(), style = MaterialTheme.typography.titleSmall)
                        Text("${titles[e.gameServerId] ?: e.gameServerId} · ${e.attempts} attempts")
                        Row {
                            Button(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AdminApi.approveReview(e.pid, e.gameServerId, null) }
                                    refresh()
                                }
                            }) { Text("Approve") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AdminApi.dismissReview(e.pid, e.gameServerId) }
                                    refresh()
                                }
                            }) { Text("Dismiss") }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) { AdminApi.addBan(e.pid, "denied via review queue") }
                                    withContext(Dispatchers.IO) { AdminApi.dismissReview(e.pid, e.gameServerId) }
                                    refresh()
                                }
                            }) { Text("Ban") }
                        }
                    }
                }
            }
        }
    }
}
