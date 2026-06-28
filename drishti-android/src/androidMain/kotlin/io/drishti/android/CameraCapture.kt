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
import java.util.concurrent.Executors

/**
 * CameraX integration for real-time frame capture.
 */
class CameraCapture(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView? = null
) {
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var isAnalyzing = false

    private var onFrameCaptured: ((Frame) -> Unit)? = null

    /**
     * Set frame capture callback.
     */
    fun onFrame(callback: (Frame) -> Unit) {
        onFrameCaptured = callback
    }

    /**
     * Start camera with analysis.
     */
    fun start(cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraSelector)
        }, context.mainExecutor)
    }

    /**
     * Stop camera analysis.
     */
    fun stop() {
        isAnalyzing = false
        cameraProvider?.unbindAll()
        analysisExecutor.shutdown()
    }

    /**
     * Start analyzing frames.
     */
    fun startAnalysis() {
        isAnalyzing = true
    }

    /**
     * Stop analyzing frames.
     */
    fun stopAnalysis() {
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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun imageProxyToFrame(imageProxy: androidx.camera.core.ImageProxy): Frame {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        return Frame(
            width = imageProxy.width,
            height = imageProxy.height,
            format = FrameFormat.YUV_420_888,
            data = bytes,
            timestamp = System.currentTimeMillis()
        )
    }
}
