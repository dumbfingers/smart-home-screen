package com.smarthome.launcher.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var haUrl by remember { mutableStateOf(settings.haUrl) }
    var haToken by remember { mutableStateOf(settings.haToken) }
    var frigateUrl by remember { mutableStateOf(settings.frigateUrl) }
    var frigateCameras by remember { mutableStateOf(settings.frigateCameras) }
    var screensaverTimeout by remember { mutableStateOf(settings.screensaverTimeoutMinutes.toString()) }
    var screensaverEnabled by remember { mutableStateOf(settings.screensaverEnabled) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Smart Home Screen Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Home Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = haUrl,
                        onValueChange = { haUrl = it },
                        label = { Text("Home Assistant URL (e.g. http://192.168.1.100:8123)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = haToken,
                        onValueChange = { haToken = it },
                        label = { Text("Long-Lived Access Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Frigate CCTV",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = frigateUrl,
                        onValueChange = { frigateUrl = it },
                        label = { Text("Frigate Server URL (e.g. http://192.168.1.100:5000)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = frigateCameras,
                        onValueChange = { frigateCameras = it },
                        label = { Text("Camera Names (comma separated)") },
                        supportingText = { Text("Example: front_door, driveway, backyard") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Bing Screensaver & Idle Timer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Idle Screensaver", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = screensaverEnabled,
                            onCheckedChange = { screensaverEnabled = it }
                        )
                    }

                    OutlinedTextField(
                        value = screensaverTimeout,
                        onValueChange = { screensaverTimeout = it },
                        label = { Text("Idle timeout in minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            settings.haUrl = haUrl
                            settings.haToken = haToken
                            settings.frigateUrl = frigateUrl
                            settings.frigateCameras = frigateCameras
                            settings.screensaverTimeoutMinutes = screensaverTimeout.toIntOrNull() ?: 2
                            settings.screensaverEnabled = screensaverEnabled
                            onSaved()
                            onDismiss()
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}
