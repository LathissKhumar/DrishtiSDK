package io.drishti.android

import io.drishti.core.*

/**
 * High-level Drishti client for Android.
 */
class DrishtiClient(
    private val context: android.content.Context,
    private val lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    private var drishti: Drishti? = null
    private var cameraCapture: CameraCapture? = null

    /**
     * Initialize Drishti with plugins.
     */
    fun initialize(
        detectors: List<DetectorPlugin> = emptyList(),
        renderers: List<RendererPlugin> = emptyList()
    ): DrishtiClient {
        val builder = Drishti.Builder()
        detectors.forEach { builder.addDetector(it) }
        renderers.forEach { builder.addRenderer(it) }
        drishti = builder.build()
        return this
    }

    /**
     * Start camera capture.
     */
    fun startCamera(
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
    suspend fun read(frame: Frame): DrishtiDiagram? {
        return drishti?.readAsync(frame)
    }

    /**
     * Stop camera and cleanup.
     */
    fun stop() {
        cameraCapture?.stop()
        cameraCapture = null
    }
}
