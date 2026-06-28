package io.drishti.core

import kotlin.test.*

class RegistryTest {

    // --- Existing tests (preserved) ---

    @Test
    fun registerAndRetrieveDetector() {
        val registry = PluginRegistry()
        val detector = StubDetector(ContentType.GRAPH)
        registry.registerDetector(detector)
        val retrieved = registry.getDetector(ContentType.GRAPH)
        assertNotNull(retrieved)
        assertEquals(ContentType.GRAPH, retrieved?.contentType)
    }

    @Test
    fun getDetectorReturnsNullForUnregistered() {
        val registry = PluginRegistry()
        assertNull(registry.getDetector(ContentType.GRAPH))
    }

    @Test
    fun registerAndRetrieveRenderer() {
        val registry = PluginRegistry()
        val renderer = StubHapticsRenderer()
        registry.registerRenderer(renderer)
        val retrieved = registry.getRenderer(StubHapticsRenderer::class)
        assertNotNull(retrieved)
    }

    @Test
    fun getRendererReturnsNullForUnregistered() {
        val registry = PluginRegistry()
        assertNull(registry.getRenderer(StubHapticsRenderer::class))
    }

    @Test
    fun getAllDetectors() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        registry.registerDetector(StubDetector(ContentType.FORMULA))
        val all = registry.getAllDetectors()
        assertEquals(2, all.size)
    }

    @Test
    fun getAllRenderers() {
        val registry = PluginRegistry()
        registry.registerRenderer(StubHapticsRenderer())
        registry.registerRenderer(StubAudioRenderer())
        val all = registry.getAllRenderers()
        assertEquals(2, all.size)
    }

    @Test
    fun registerDetectorOverwritesExisting() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        val all = registry.getAllDetectors()
        assertEquals(1, all.size)
    }

    @Test
    fun registerDetectorReturnsPreviousInstance() {
        val registry = PluginRegistry()
        val first = StubDetector(ContentType.GRAPH)
        val second = StubDetector(ContentType.GRAPH)
        registry.registerDetector(first)
        val previous = registry.registerDetector(second)
        assertNotNull(previous)
        assertEquals(first, previous)
    }

    // --- New tests: detector-renderer linking ---

    @Test
    fun linkDetectorToRendererAndRetrieve() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        registry.registerRenderer(StubHapticsRenderer())
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)

        val renderers = registry.getRenderersForContentType(ContentType.GRAPH)
        assertEquals(1, renderers.size)
        assertTrue(renderers.contains(StubHapticsRenderer::class))
    }

    @Test
    fun multipleRenderersForSameContentType() {
        val registry = PluginRegistry()
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubAudioRenderer::class)

        val renderers = registry.getRenderersForContentType(ContentType.GRAPH)
        assertEquals(2, renderers.size)
    }

    @Test
    fun getRenderersForUnlinkedContentType() {
        val registry = PluginRegistry()
        val renderers = registry.getRenderersForContentType(ContentType.FORMULA)
        assertTrue(renderers.isEmpty())
    }

    @Test
    fun getContentTypesForRenderer() {
        val registry = PluginRegistry()
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)
        registry.linkDetectorToRenderer(ContentType.FORMULA, StubHapticsRenderer::class)

        val types = registry.getContentTypesForRenderer(StubHapticsRenderer::class)
        assertEquals(2, types.size)
        assertTrue(types.contains(ContentType.GRAPH))
        assertTrue(types.contains(ContentType.FORMULA))
    }

    @Test
    fun getContentTypesForUnlinkedRenderer() {
        val registry = PluginRegistry()
        val types = registry.getContentTypesForRenderer(StubAudioRenderer::class)
        assertTrue(types.isEmpty())
    }

    // --- New tests: dependency tracking ---

    @Test
    fun declareAndRetrieveDependencies() {
        val registry = PluginRegistry()
        registry.declareDependency(
            StubHapticsRenderer::class,
            setOf(ContentType.GRAPH, ContentType.FORMULA)
        )

        val deps = registry.getDependencies(StubHapticsRenderer::class)
        assertEquals(2, deps.size)
        assertTrue(deps.contains(ContentType.GRAPH))
        assertTrue(deps.contains(ContentType.FORMULA))
    }

    @Test
    fun getDependenciesForUndeclaredRenderer() {
        val registry = PluginRegistry()
        val deps = registry.getDependencies(StubAudioRenderer::class)
        assertTrue(deps.isEmpty())
    }

    // --- New tests: validation ---

    @Test
    fun validateReturnsCompatibleWhenAllSatisfied() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        registry.registerRenderer(StubHapticsRenderer())
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)

        val report = registry.validate()
        assertTrue(report.compatible)
        assertTrue(report.issues.isEmpty())
    }

    @Test
    fun validateReportsMissingDetector() {
        val registry = PluginRegistry()
        registry.registerRenderer(StubHapticsRenderer())
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)
        // No detector registered for GRAPH

        val report = registry.validate()
        assertFalse(report.compatible)
        assertTrue(report.issues.isNotEmpty())
        assertTrue(report.issues.any { it.contains("GRAPH") })
    }

    @Test
    fun validateReportsUnsatisfiedDependency() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        registry.registerRenderer(StubHapticsRenderer())
        // Declare that haptics renderer depends on FORMULA (which has no detector)
        registry.declareDependency(
            StubHapticsRenderer::class,
            setOf(ContentType.GRAPH, ContentType.FORMULA)
        )

        val report = registry.validate()
        assertFalse(report.compatible)
        assertTrue(report.issues.any { it.contains("FORMULA") })
    }

    @Test
    fun validateReportsUnregisteredRendererClass() {
        val registry = PluginRegistry()
        registry.registerDetector(StubDetector(ContentType.GRAPH))
        // Link but don't register the renderer
        registry.linkDetectorToRenderer(ContentType.GRAPH, StubHapticsRenderer::class)

        val report = registry.validate()
        assertFalse(report.compatible)
        assertTrue(report.issues.any { it.contains("StubHapticsRenderer") && it.contains("not registered") })
    }

    @Test
    fun validateEmptyRegistryIsCompatible() {
        val registry = PluginRegistry()
        val report = registry.validate()
        assertTrue(report.compatible)
        assertTrue(report.issues.isEmpty())
    }
}
