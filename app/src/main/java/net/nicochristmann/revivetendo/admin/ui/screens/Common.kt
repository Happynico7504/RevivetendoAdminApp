package net.nicochristmann.revivetendo.admin.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LabeledField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier, singleLine = true)
}

// Mirrors relay-admin's gameServerTitles map (main.go) - purely a display
// label lookup, the server is the source of truth for which IDs are valid.
val gameServerTitles = mapOf(
    "00003200" to "Friends / Presence",
    "1005A000" to "WiiU Chat",
    "1010EB00" to "Mario Kart 8",
    "1012F100" to "Wii Sports Club",
    "10145E00" to "Angry Birds Star Wars",
    "10176A00" to "Super Mario Maker",
    "100E4B00" to "Super Smash Bros.",
    "1014B700" to "Minecraft: WiiU Edition",
    "10138B00" to "Pokemon Art Academy",
    "10104E00" to "Animal Crossing: amiibo Festival",
    "1019EC00" to "Yo-Kai Watch Blasters",
    "10189B00" to "Pokémon Rumble World",
)

/**
 * Dropdown over the known game server IDs instead of a free-text hex field.
 * When [allowBlank] is true, an extra "None" option maps to an empty string
 * (used by the redirects form, where a game server ID is optional).
 */
@Composable
fun GameServerDropdown(
    selected: String,
    onSelected: (String) -> Unit,
    label: String,
    allowBlank: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (allowBlank) listOf("" to "None") + gameServerTitles.toList() else gameServerTitles.toList()
    val displayText = when {
        selected.isBlank() -> "None"
        else -> gameServerTitles[selected]?.let { "$it ($selected)" } ?: selected
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
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
