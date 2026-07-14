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

package io.drishti.android

import io.drishti.core.DetectorPlugin
import io.drishti.core.Drishti
import io.drishti.core.DrishtiDiagram
import io.drishti.core.Frame
import io.drishti.core.RendererPlugin

/**
 * High-level Drishti client for Android.
 */
public class DrishtiClient(
    private val context: android.content.Context,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    private var drishti: Drishti? = null
    private var cameraCapture: CameraCapture? = null

    /**
     * Initialize Drishti with plugins.
     *
     * If already initialized, stops the previous instance first to prevent resource leaks.
     */
    public fun initialize(
        detectors: List<DetectorPlugin> = emptyList(),
        renderers: List<RendererPlugin> = emptyList()
    ): DrishtiClient {
        stop()
        val builder = Drishti.Builder()
        detectors.forEach { builder.addDetector(it) }
        renderers.forEach { builder.addRenderer(it) }
        drishti = builder.build()
        return this
    }

    /**
     * Start camera capture.
     */
    public fun startCamera(
        previewView: androidx.camera.view.PreviewView? = null,
        onFrame: (Frame) -> Unit = {}
    ): DrishtiClient {
        cameraCapture = CameraCapture(context, lifecycleOwner, previewView)
        cameraCapture?.onFrame(onFrame)
        cameraCapture?.start()
        cameraCapture?.startAnalysis()
        return this
    }

    /**
     * Read a single frame.
     */
    public suspend fun read(frame: Frame): DrishtiDiagram? {
        return drishti?.readAsync(frame)
    }

    /**
     * Stop camera and cleanup all resources.
     */
    public fun stop() {
        cameraCapture?.stop()
        cameraCapture = null
        drishti = null
    }
}
