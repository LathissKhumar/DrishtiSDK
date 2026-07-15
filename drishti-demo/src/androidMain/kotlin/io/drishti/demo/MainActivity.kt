/*
 * Copyright 2026 DrishtiSTEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.drishti.demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import io.drishti.android.DrishtiClient
import io.drishti.core.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var client: DrishtiClient
    internal var previewView: PreviewView? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCameraWithPermission()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
            }
        }

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
                DrishtiDemoScreen(
                    client = client,
                    onStartCamera = { startCameraWithPermission() },
                    onStopCamera = { client.stop() }
                )
            }
        }
    }

    private fun startCameraWithPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            client.startCamera(previewView)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.stop()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrishtiDemoScreen(
    client: DrishtiClient,
    onStartCamera: () -> Unit,
    onStopCamera: () -> Unit
) {
    var status by remember { mutableStateOf("Ready") }
    var lastContent by remember { mutableStateOf<ContentItem?>(null) }
    var lastSummary by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drishti Demo") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { preview ->
                        (ctx as? MainActivity)?.previewView = preview
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            // Status
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Detected content
            lastContent?.let { content ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Detected", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = when (content) {
                                is GraphContent -> "${content.graphType.name.lowercase().replaceFirstChar { it.uppercase() }} — ${content.dataPoints.size} points"
                                is FormulaContent -> "${content.formulaType.name.lowercase().replaceFirstChar { it.uppercase() }}: ${content.expression}"
                                is MoleculeContent -> "Molecule: ${content.name.ifEmpty { "Unknown" }} — ${content.atoms.size} atoms"
                                else -> content.contentType.name
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (lastSummary.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(lastSummary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        status = "Starting camera..."
                        onStartCamera()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Start")
                }

                OutlinedButton(
                    onClick = {
                        onStopCamera()
                        status = "Stopped"
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }

                Button(
                    onClick = {
                        scope.launch {
                            status = "Processing..."
                            val diagram = client.read(
                                Frame(
                                    width = 640,
                                    height = 480,
                                    format = FrameFormat.GRAYSCALE,
                                    data = ByteArray(0),
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                            if (diagram != null) {
                                lastContent = diagram.contentItems.firstOrNull()
                                lastSummary = diagram.summary().text
                                status = "Found ${diagram.contentItems.size} item(s)"
                            } else {
                                status = "No content detected"
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Read")
                }
            }
        }
    }
}
