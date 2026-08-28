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
import net.nicochristmann.revivetendo.admin.net.UserAccessEntry

@Composable
fun UsersScreen(onBack: () -> Unit) {
    var game by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<UserAccessEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pid by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() {
        if (game.isBlank()) return
        scope.launch {
            loading = true
            error = null
            try {
                users = withContext(Dispatchers.IO) { AdminApi.users(game) }
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(game) { refresh() }

    SectionScaffold("Game whitelist", onBack, error, loading) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            item {
                GameServerDropdown(game, { game = it }, "Game", allowBlank = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                if (game.isBlank()) {
                    Text("Pick a game to see its whitelist.", style = MaterialTheme.typography.bodyMedium)
                }
            }
            items(if (game.isBlank()) emptyList() else users) { u ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(u.pnid ?: u.pid.toString(), style = MaterialTheme.typography.titleSmall)
                        u.note?.let { Text(it) }
                        OutlinedButton(onClick = {
                            scope.launch {
                                withContext(Dispatchers.IO) { AdminApi.deleteUser(game, u.pid) }
                                refresh()
                            }
                        }) { Text("Remove") }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("Add user", style = MaterialTheme.typography.titleMedium)
                LabeledField(pid, { pid = it }, "PID", Modifier.fillMaxWidth())
                LabeledField(note, { note = it }, "Label (optional)", Modifier.fillMaxWidth())
                Button(
                    enabled = game.isNotBlank(),
                    onClick = {
                        val pidLong = pid.toLongOrNull()
                        if (pidLong == null) {
                            error = "Invalid PID"
                            return@Button
                        }
                        scope.launch {
                            try {
                                users = withContext(Dispatchers.IO) { AdminApi.addUser(game, pidLong, note.ifBlank { null }) }
                                pid = ""; note = ""
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    },
                ) { Text("Add") }
            }
        }
    }
}
