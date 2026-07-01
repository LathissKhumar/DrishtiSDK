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

package io.drishti.core

import kotlin.reflect.KClass

/**
 * Result of a plugin compatibility validation.
 *
 * @param compatible Whether all registered plugins are compatible.
 * @param issues Human-readable list of compatibility issues found.
 */
public data class ValidationReport(
    val compatible: Boolean,
    val issues: List<String>
)

/**
 * Registry for detector and renderer plugins.
 *
 * Manages registration and lookup of [DetectorPlugin] and [RendererPlugin]
 * instances. Tracks content-type mappings between detectors and renderers
 * so the pipeline can automatically pair them.
 *
 * Usage:
 * ```kotlin
 * val registry = PluginRegistry()
 * registry.registerDetector(graphDetector)
 * registry.registerRenderer(hapticsRenderer)
 * registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)
 * ```
 */
public class PluginRegistry {

    private val lock = Lock()

    /** Registered detectors keyed by the content type they detect. */
    private val detectors = mutableMapOf<ContentType, DetectorPlugin>()

    /** Registered renderers keyed by their implementation class. */
    private val renderers = mutableMapOf<KClass<out RendererPlugin>, RendererPlugin>()

    /**
     * Maps content types to the renderer classes that should handle them.
     *
     * When a detector produces a [ContentItem] of a given [ContentType],
     * the pipeline uses this map to find the appropriate renderer.
     */
    private val contentTypeToRenderers = mutableMapOf<ContentType, MutableSet<KClass<out RendererPlugin>>>()

    /**
     * Tracks which renderers depend on which detectors.
     *
     * A renderer may require a specific detector to have run first
     * (e.g., a haptics renderer needs graph detection data).
     */
    private val rendererDependencies = mutableMapOf<KClass<out RendererPlugin>, Set<ContentType>>()

    // --- Detector registration ---

    /**
     * Register a detector plugin.
     *
     * If a detector for the same [ContentType] already exists, it is
     * replaced and the old instance is returned.
     *
     * @param plugin The detector to register.
     * @return The previously registered detector for this type, or null.
     */
    public fun registerDetector(plugin: DetectorPlugin): DetectorPlugin? = lock.withLock {
        val previous = detectors.put(plugin.contentType, plugin)
        return previous
    }

    // --- Renderer registration ---

    /**
     * Register a renderer plugin.
     *
     * @param plugin The renderer to register.
     */
    public fun registerRenderer(plugin: RendererPlugin): Unit = lock.withLock {
        renderers[plugin::class] = plugin
    }

    // --- Connectivity ---

    /**
     * Link a content type to a renderer class.
     *
     * This tells the pipeline that when a [ContentType] is detected,
     * the specified renderer should be used for output.
     *
     * @param contentType The content type produced by a detector.
     * @param rendererClass The renderer class to handle this content type.
     */
    public fun linkDetectorToRenderer(contentType: ContentType, rendererClass: KClass<out RendererPlugin>): Unit = lock.withLock {
        contentTypeToRenderers.getOrPut(contentType) { mutableSetOf() }.add(rendererClass)
    }

    /**
     * Declare that a renderer depends on certain content types being detected.
     *
     * This is used during validation to ensure all required detectors
     * are registered before a renderer that depends on them.
     *
     * @param rendererClass The renderer class.
     * @param requiredContentTypes The content types this renderer requires.
     */
    public fun declareDependency(rendererClass: KClass<out RendererPlugin>, requiredContentTypes: Set<ContentType>): Unit = lock.withLock {
        rendererDependencies[rendererClass] = requiredContentTypes
    }

    // --- Lookups ---

    /**
     * Retrieve the detector for a given [ContentType].
     *
     * @return The registered detector, or null if none registered.
     */
    public fun getDetector(contentType: ContentType): DetectorPlugin? = lock.withLock {
        detectors[contentType]
    }

    /**
     * Retrieve a renderer by its implementation class.
     *
     * @return The registered renderer cast to [T], or null if not found.
     */
    public fun <T : RendererPlugin> getRenderer(type: KClass<T>): T? = lock.withLock {
        @Suppress("UNCHECKED_CAST")
        return renderers[type] as? T
    }

    /**
     * Find all renderer classes linked to a given [ContentType].
     *
     * @return Set of renderer classes that should handle this content type.
     */
    public fun getRenderersForContentType(contentType: ContentType): Set<KClass<out RendererPlugin>> = lock.withLock {
        return contentTypeToRenderers[contentType]?.toSet() ?: emptySet()
    }

    /**
     * Find all content types that a given renderer class is linked to.
     *
     * @return Set of content types the renderer handles.
     */
    public fun getContentTypesForRenderer(rendererClass: KClass<out RendererPlugin>): Set<ContentType> = lock.withLock {
        return contentTypeToRenderers
            .filter { it.value.contains(rendererClass) }
            .keys
    }

    /**
     * Get the dependency set for a renderer class.
     *
     * @return The set of content types this renderer requires, or empty.
     */
    public fun getDependencies(rendererClass: KClass<out RendererPlugin>): Set<ContentType> = lock.withLock {
        return rendererDependencies[rendererClass] ?: emptySet()
    }

    // --- Bulk lookups ---

    /** Return all registered detectors. */
    public fun getAllDetectors(): List<DetectorPlugin> = lock.withLock {
        detectors.values.toList()
    }

    /** Return all registered renderers. */
    public fun getAllRenderers(): List<RendererPlugin> = lock.withLock {
        renderers.values.toList()
    }

    // --- Validation ---

    /**
     * Validate that all registered renderers have their detector dependencies met.
     *
     * Checks:
     * - Every renderer linked to a content type has a detector for that type.
     * - Every renderer with declared dependencies has those detectors registered.
     * - No detector is registered without a content type.
     *
     * @return A [ValidationReport] with the result and any issues found.
     */
    public fun validate(): ValidationReport = lock.withLock {
        val issues = mutableListOf<String>()

        // Check that every linked content type has a registered detector
        for ((contentType, _) in contentTypeToRenderers) {
            if (contentType !in detectors) {
                issues.add("Content type $contentType is linked to renderers but has no registered detector")
            }
        }

        // Check that every renderer dependency is satisfied
        for ((rendererClass, deps) in rendererDependencies) {
            for (dep in deps) {
                if (dep !in detectors) {
                    issues.add(
                        "Renderer ${rendererClass.simpleName} depends on $dep detector, " +
                            "but no detector is registered for it"
                    )
                }
            }
        }

        // Check that every detector-linked renderer class is actually registered
        for ((_, rendererClasses) in contentTypeToRenderers) {
            for (rendererClass in rendererClasses) {
                if (rendererClass !in renderers) {
                    issues.add("Renderer class ${rendererClass.simpleName} is linked but not registered")
                }
            }
        }

        return ValidationReport(
            compatible = issues.isEmpty(),
            issues = issues
        )
    }
}
