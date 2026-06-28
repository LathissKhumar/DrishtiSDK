package io.drishti.android

/**
 * Platform-specific Android device detection.
 *
 * Common declarations for the expect/actual pattern.
 */
expect class AndroidPlatformDetector : PlatformDetector {
    /**
     * Detect and return the current device capabilities.
     */
    override fun detect(): DeviceCapabilities
}
