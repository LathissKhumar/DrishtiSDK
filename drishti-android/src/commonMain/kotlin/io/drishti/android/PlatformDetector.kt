package io.drishti.android

/**
 * Detects device capabilities for adaptive SDK behavior.
 *
 * Platform implementations query the actual hardware and OS to determine
 * supported haptic, audio, NPU, and threading capabilities.
 */
interface PlatformDetector {
    /**
     * Detect and return the current device capabilities.
     */
    fun detect(): DeviceCapabilities
}
