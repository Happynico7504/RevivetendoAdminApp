package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.nicochristmann.revivetendo.admin.net.GameTitles

@Composable
fun LabeledField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier, singleLine = true)
}

/** Fetches relay-admin's game title map on first use per screen and caches it in [GameTitles] for the session. */
@Composable
fun rememberGameTitles(): Map<String, String> {
    var titles by remember { mutableStateOf(GameTitles.cache) }
    LaunchedEffect(Unit) {
        GameTitles.ensureLoaded()
        titles = GameTitles.cache
    }
    return titles
}

/**
 * Dropdown over the game server IDs relay-admin knows about instead of a
 * free-text hex field. When [allowBlank] is true, an extra "None" option
 * maps to an empty string (used by the redirects form, where a game server
 * ID is optional).
 */
@Composable
fun GameServerDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    label: String,
    allowBlank: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val titles = rememberGameTitles()
    var expanded by remember { mutableStateOf(false) }
    val options = if (allowBlank) listOf("" to "None") + titles.toList() else titles.toList()
    val displayText = when {
        selected.isBlank() -> "None"
        else -> titles[selected]?.let { "$it ($selected)" } ?: selected
    }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(if (id.isBlank()) name else "$name ($id)") },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun SectionScaffold(
    title: String,
    onBack: () -> Unit,
    errorMessage: String?,
    isLoading: Boolean,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←", style = MaterialTheme.typography.headlineSmall) }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                errorMessage != null -> Text(
                    "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
                else -> content()
            }
        }
    }
}
