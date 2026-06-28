package io.drishti.haptics

import io.drishti.core.EdgeType
import io.drishti.core.SceneNode

/**
 * Haptic pattern definitions for VibrationEffect.Composition API (API 30+).
 *
 * Platform-agnostic data structures that the Android HAL translates to
 * actual VibrationEffect calls:
 * - API 31+: VibrationEffect.Composition with primitive effects
 * - API 30: VibrationEffect.createWaveform() fallback
 */

/**
 * Waveform types for haptic effects.
 *
 * Each waveform maps to a distinct vibration character suitable for
 * different types of STEM content relationships.
 */
enum class HapticWaveform {
    /** Steady vibration for spatial proximity connections. */
    PROXIMITY_BUZZ,
    /** Two quick pulses for containment relationships. */
    DOUBLE_TAP,
    /** Single rhythmic beat for semantic connections. */
    PULSE,
    /** Instant high-frequency snap for emphasis and labels. */
    SHARP_CLICK,
    /** Low-frequency gentle vibration for background elements. */
    SOFT_RUB,
    /** Fast repeating pattern for temporal sequences. */
    RAPID_TAP,
    /** Amplitude ramp for gradient relationships like measurements. */
    GRADIENT_SWEEP
}

/**
 * Classification of a SceneNode for haptic waveform selection.
 */
enum class NodeHapticType {
    DATA_POINT,
    TEXT,
    SHAPE,
    AXIS,
    UNKNOWN
}

/**
 * A single haptic event within a pattern.
 *
 * Maps directly to VibrationEffect.Composition primitive on API 31+,
 * or to a waveform segment on API 30.
 *
 * @param intensity Amplitude 0.0-1.0 (maps to VibrationEffect amplitude 1-255)
 * @param duration Duration in milliseconds
 * @param waveform Waveform type determining vibration character
 * @param spatialX Horizontal position 0.0-1.0 (left/right for dual-motor)
 * @param spatialY Vertical position 0.0-1.0 (top/bottom)
 * @param delay Delay before this event in milliseconds
 */
data class HapticEventSpec(
    val intensity: Float,
    val duration: Long,
    val waveform: HapticWaveform,
    val spatialX: Float = 0.5f,
    val spatialY: Float = 0.5f,
    val delay: Long = 0L
) {
    init {
        require(intensity in 0f..1f) { "Intensity must be 0.0-1.0, was $intensity" }
        require(duration > 0) { "Duration must be positive, was $duration" }
        require(spatialX in 0f..1f) { "spatialX must be 0.0-1.0, was $spatialX" }
        require(spatialY in 0f..1f) { "spatialY must be 0.0-1.0, was $spatialY" }
        require(delay >= 0) { "Delay must be non-negative, was $delay" }
    }
}

/**
 * A complete haptic pattern composed of sequenced events.
 *
 * @param events Ordered list of haptic events
 * @param totalDuration Total pattern duration in milliseconds
 * @param patternName Human-readable pattern identifier
 * @param sourceNodeId Optional SceneGraph node ID this pattern was generated from
 */
data class HapticPatternDefinition(
    val events: List<HapticEventSpec>,
    val totalDuration: Long,
    val patternName: String,
    val sourceNodeId: String? = null
) {
    companion object {
        fun empty(): HapticPatternDefinition = HapticPatternDefinition(
            events = emptyList(),
            totalDuration = 0L,
            patternName = "empty"
        )
    }
}

/**
 * Amplitude range constants for VibrationEffect encoding.
 *
 * Maps from normalized 0.0-1.0 float values to Android integer amplitudes
 * used by VibrationEffect.createOneShot() and VibrationEffect.createWaveform().
 */
object HapticAmplitudeRange {
    /** Minimum perceptible amplitude for VibrationEffect (API 30+). */
    const val MIN = 1
    /** Maximum amplitude for VibrationEffect. */
    const val MAX = 255

    /** Convert normalized intensity (0.0-1.0) to VibrationEffect amplitude (1-255). */
    fun toAmplitude(intensity: Float): Int =
        (MIN + (intensity.coerceIn(0f, 1f) * (MAX - MIN))).toInt().coerceIn(MIN, MAX)
}

/**
 * Duration presets for edge-type-based haptic patterns (milliseconds).
 *
 * These are base durations; actual duration is modulated by edge weight.
 */
object HapticDurationPresets {
    /** Base duration for spatial proximity buzzes (ms). */
    const val SPATIAL_BASE = 40L
    /** Base duration for containment double-taps (ms). */
    const val CONTAIN_BASE = 30L
    /** Base duration for semantic pulses (ms). */
    const val SEMANTIC_BASE = 60L
    /** Base duration for label taps (ms). */
    const val LABEL_BASE = 25L
    /** Base duration for measure connections (ms). */
    const val MEASURE_BASE = 50L
    /** Base duration for temporal sequence taps (ms). */
    const val TEMPORAL_BASE = 35L
    /** Base duration for generic connections (ms). */
    const val CONNECT_BASE = 45L
}

/**
 * Maps [EdgeType] to [HapticWaveform] and base duration.
 *
 * Each edge type produces a distinct haptic character:
 * - SPATIAL → PROXIMITY_BUZZ (steady vibration for nearby items)
 * - CONTAINS → DOUBLE_TAP (two quick pulses for containment)
 * - SEMANTIC → PULSE (single beat for semantic connections)
 * - CONNECTS → PROXIMITY_BUZZ (steady for spatial connections)
 * - LABELS → SHARP_CLICK (instant snap for labels)
 * - MEASURES → GRADIENT_SWEEP (ramp for measurement relationships)
 * - TEMPORAL → RAPID_TAP (fast sequence for time ordering)
 */
object EdgeWaveformMapper {
    /** Map EdgeType to the appropriate HapticWaveform. */
    fun mapWaveform(edgeType: EdgeType): HapticWaveform = when (edgeType) {
        EdgeType.SPATIAL -> HapticWaveform.PROXIMITY_BUZZ
        EdgeType.CONTAINS -> HapticWaveform.DOUBLE_TAP
        EdgeType.SEMANTIC -> HapticWaveform.PULSE
        EdgeType.CONNECTS -> HapticWaveform.PROXIMITY_BUZZ
        EdgeType.LABELS -> HapticWaveform.SHARP_CLICK
        EdgeType.MEASURES -> HapticWaveform.GRADIENT_SWEEP
        EdgeType.TEMPORAL -> HapticWaveform.RAPID_TAP
    }

    /** Map EdgeType to base duration in milliseconds. */
    fun mapBaseDuration(edgeType: EdgeType): Long = when (edgeType) {
        EdgeType.SPATIAL -> HapticDurationPresets.SPATIAL_BASE
        EdgeType.CONTAINS -> HapticDurationPresets.CONTAIN_BASE
        EdgeType.SEMANTIC -> HapticDurationPresets.SEMANTIC_BASE
        EdgeType.CONNECTS -> HapticDurationPresets.CONNECT_BASE
        EdgeType.LABELS -> HapticDurationPresets.LABEL_BASE
        EdgeType.MEASURES -> HapticDurationPresets.MEASURE_BASE
        EdgeType.TEMPORAL -> HapticDurationPresets.TEMPORAL_BASE
    }
}

/**
 * Maps [SceneNode] types to haptic parameters.
 *
 * Node type determines the base intensity and duration of the haptic pulse:
 * - DATA_POINT → SHARP_CLICK, high intensity (0.9)
 * - TEXT → SOFT_RUB, low intensity (0.6)
 * - SHAPE → PULSE, medium intensity (0.75)
 * - AXIS → PROXIMITY_BUZZ, low intensity (0.5)
 *
 * Node depth modulates frequency: deeper nodes produce shorter, faster pulses.
 */
object NodeWaveformMapper {
    /** Classify a SceneNode into its haptic type. */
    fun classifyNode(node: SceneNode): NodeHapticType = when (node) {
        is SceneNode.DataPointNode -> NodeHapticType.DATA_POINT
        is SceneNode.TextNode -> NodeHapticType.TEXT
        is SceneNode.ShapeNode -> NodeHapticType.SHAPE
        is SceneNode.AxisNode -> NodeHapticType.AXIS
    }

    /** Map NodeHapticType to a base duration for the node's haptic pulse. */
    fun mapBaseDuration(type: NodeHapticType): Long = when (type) {
        NodeHapticType.DATA_POINT -> 70L
        NodeHapticType.TEXT -> 40L
        NodeHapticType.SHAPE -> 60L
        NodeHapticType.AXIS -> 50L
        NodeHapticType.UNKNOWN -> 45L
    }

    /** Map NodeHapticType to an intensity modifier (multiplied with base intensity). */
    fun mapIntensityModifier(type: NodeHapticType): Float = when (type) {
        NodeHapticType.DATA_POINT -> 0.9f
        NodeHapticType.TEXT -> 0.6f
        NodeHapticType.SHAPE -> 0.75f
        NodeHapticType.AXIS -> 0.5f
        NodeHapticType.UNKNOWN -> 0.5f
    }

    /**
     * Depth modifier: deeper nodes produce shorter, faster pulses.
     *
     * - depth 0: 1.0x (full duration)
     * - depth 1: 0.7x
     * - depth 2: 0.5x
     * - depth 3+: 0.3x
     */
    fun depthFrequencyModifier(depth: Int): Float = when {
        depth <= 0 -> 1.0f
        depth == 1 -> 0.7f
        depth == 2 -> 0.5f
        else -> 0.3f
    }

    /** Map NodeHapticType to a HapticWaveform for pattern definitions. */
    fun mapWaveform(type: NodeHapticType): HapticWaveform = when (type) {
        NodeHapticType.DATA_POINT -> HapticWaveform.SHARP_CLICK
        NodeHapticType.TEXT -> HapticWaveform.SOFT_RUB
        NodeHapticType.SHAPE -> HapticWaveform.PULSE
        NodeHapticType.AXIS -> HapticWaveform.PROXIMITY_BUZZ
        NodeHapticType.UNKNOWN -> HapticWaveform.PULSE
    }
}
