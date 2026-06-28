package io.drishti.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.drishti.android.DrishtiClient
import io.drishti.core.*

/**
 * DrishtiSTEM Demo App - Showcases the Drishti SDK capabilities.
 */
class MainActivity : ComponentActivity() {
    private lateinit var client: DrishtiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        client = DrishtiClient(this, this)
            .initialize(
                detectors = listOf(
                    io.drishti.graph.GraphPlugin(),
                    io.drishti.formula.FormulaPlugin(),
                    io.drishti.molecule.MoleculePlugin()
                ),
                renderers = listOf(
                    io.drishti.haptics.HapticsPlugin(),
                    io.drishti.audio.AudioPlugin(),
                    io.drishti.voice.VoicePlugin()
                )
            )

        setContent {
            MaterialTheme {
                DrishtiDemoScreen(client)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrishtiDemoScreen(client: DrishtiClient) {
    var status by remember { mutableStateOf("Ready") }
    var lastContent by remember { mutableStateOf<ContentItem?>(null) }
    var outputMode by remember { mutableStateOf("haptic") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DrishtiSTEM Demo") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Status",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Content info card
            lastContent?.let { content ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detected Content",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = when (content) {
                                is GraphContent -> "Graph: ${content.graphType.name}"
                                is FormulaContent -> "Formula: ${content.formulaType.name}"
                                is MoleculeContent -> "Molecule: ${content.name.ifEmpty { "Unknown" }}"
                                else -> content.contentType.name
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Output mode selector
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = outputMode == "haptic",
                    onClick = { outputMode = "haptic" },
                    label = { Text("Haptic") }
                )
                FilterChip(
                    selected = outputMode == "audio",
                    onClick = { outputMode = "audio" },
                    label = { Text("Audio") }
                )
                FilterChip(
                    selected = outputMode == "voice",
                    onClick = { outputMode = "voice" },
                    label = { Text("Voice") }
                )
            }

            // Action buttons
            Button(
                onClick = {
                    status = "Starting camera..."
                    client.startCamera { frame ->
                        status = "Processing frame..."
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Camera")
            }

            OutlinedButton(
                onClick = {
                    client.stop()
                    status = "Stopped"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Camera")
            }
        }
    }
}
