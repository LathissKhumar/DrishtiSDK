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

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import io.drishti.core.Frame
import io.drishti.core.FrameFormat
import android.util.Log
import java.util.concurrent.Executors

/**
 * CameraX integration for real-time frame capture.
 */
public class CameraCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    @Volatile private var isAnalyzing = false

    private var onFrameCaptured: ((Frame) -> Unit)? = null

    /**
     * Set frame capture callback.
     */
    public fun onFrame(callback: (Frame) -> Unit) {
        onFrameCaptured = callback
    }

    /**
     * Start camera with analysis.
     */
    public fun start(cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraSelector)
        }, context.mainExecutor)
    }

    /**
     * Stop camera analysis.
     */
    public fun stop() {
        isAnalyzing = false
        cameraProvider?.unbindAll()
    }

    /**
     * Start analyzing frames.
     */
    public fun startAnalysis() {
        isAnalyzing = true
    }

    /**
     * Stop analyzing frames.
     */
    public fun stopAnalysis() {
        isAnalyzing = false
    }

    private fun bindCamera(cameraSelector: CameraSelector) {
        val provider = cameraProvider ?: return

        // Preview use case
        val preview = Preview.Builder().build().also {
            previewView?.let { view -> it.setSurfaceProvider(view.surfaceProvider) }
        }

        // Image analysis use case
        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (isAnalyzing) {
                        val frame = imageProxyToFrame(imageProxy)
                        onFrameCaptured?.invoke(frame)
                    }
                    imageProxy.close()
                }
            }

        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
        } catch (e: IllegalArgumentException) {
            Log.e("CameraCapture", "Invalid camera use case configuration", e)
        } catch (e: IllegalStateException) {
            Log.e("CameraCapture", "Camera lifecycle already destroyed", e)
        }
    }

    private fun imageProxyToFrame(imageProxy: androidx.camera.core.ImageProxy): Frame {
        // Extract Y plane only — this is grayscale luminance data.
        // Label as GRAYSCALE to be honest about what the data actually contains,
        // rather than claiming full YUV_420_888 when only the Y plane is extracted.
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return Frame(
            width = imageProxy.width,
            height = imageProxy.height,
            format = FrameFormat.GRAYSCALE,
            data = bytes,
            timestamp = System.currentTimeMillis()
        )
    }
}
