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

package io.drishti.graph

import io.drishti.core.TestFixtures
import io.drishti.core.*
import kotlin.test.*

class GraphPluginTest {

    @Test
    fun nameIsGraph() {
        val plugin = GraphPlugin()
        assertEquals("graph", plugin.name)
    }

    @Test
    fun contentTypeIsGraph() {
        val plugin = GraphPlugin()
        assertEquals(ContentType.GRAPH, plugin.contentType)
    }

    @Test
    fun detectFromJsonReturnsGraph() {
        val plugin = GraphPlugin()
        val json = """
        {
            "type": "line_chart",
            "title": "Sales",
            "x_label": "Month",
            "y_label": "Revenue",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent()

        val graph = plugin.fromJson(json)
        assertNotNull(graph)
        assertEquals(GraphType.LINE_CHART, graph.graphType)
        assertEquals("Sales", graph.title)
        assertEquals("Month", graph.axes.x.label)
        assertEquals("Revenue", graph.axes.y.label)
        assertEquals(2, graph.dataPoints.size)
    }

    @Test
    fun detectFromJsonReturnsNullOnInvalidJson() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("not json")
        assertNull(graph)
    }

    @Test
    fun detectFromCsvReturnsGraph() {
        val plugin = GraphPlugin()
        val csv = """
        Month,Revenue
        Jan,100
        Feb,200
        Mar,150
        """.trimIndent()

        val graph = plugin.fromCsv(csv, title = "Monthly Sales")
        assertNotNull(graph)
        assertEquals("Monthly Sales", graph.title)
        assertEquals(3, graph.dataPoints.size)
    }

    @Test
    fun fromDataPointsReturnsGraph() {
        val plugin = GraphPlugin()
        val graph = plugin.fromDataPoints(
            type = "bar_chart",
            title = "Quarterly",
            points = listOf(1 to 100, 2 to 200, 3 to 150)
        )
        assertNotNull(graph)
        assertEquals(GraphType.BAR_CHART, graph.graphType)
        assertEquals("Quarterly", graph.title)
        assertEquals(3, graph.dataPoints.size)
    }

    @Test
    fun renderHapticWithGraphContent() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"},
                {"x": "3", "y": "150"}
            ]
        }
        """.trimIndent())!!

        val output = plugin.renderHaptic(listOf(graph))
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
        assertEquals("graph_haptic", output.pattern)
    }

    @Test
    fun renderHapticWithEmptyItems() {
        val plugin = GraphPlugin()
        val output = plugin.renderHaptic(emptyList())
        assertNotNull(output)
        assertTrue(output.pulses.isEmpty())
        assertEquals("empty", output.pattern)
    }

    @Test
    fun renderAudioWithGraphContent() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent())!!

        val output = plugin.renderAudio(listOf(graph))
        assertNotNull(output)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderAudioWithEmptyItems() {
        val plugin = GraphPlugin()
        val output = plugin.renderAudio(emptyList())
        assertNotNull(output)
        assertTrue(output.sources.isEmpty())
    }

    @Test
    fun renderVoiceWithGraphContent() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "title": "Test",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent())!!

        val output = plugin.renderVoice(listOf(graph))
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("Test"))
    }

    @Test
    fun renderVoiceWithEmptyItems() {
        val plugin = GraphPlugin()
        val output = plugin.renderVoice(emptyList())
        assertNotNull(output)
        assertEquals("No graph content", output.speech.text)
    }

    @Test
    fun renderVegaLiteSpecReturnsValidJson() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "title": "Test Chart",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent())!!

        val spec = plugin.renderVegaLiteSpec(graph)
        assertTrue(spec.contains("vega-lite"))
        assertTrue(spec.contains("line"))
        assertTrue(spec.contains("Test Chart"))
    }

    @Test
    fun getAccessibilityDescriptionReturnsNonEmpty() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "bar_chart",
            "title": "Revenue",
            "x_label": "Quarter",
            "y_label": "Amount",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent())!!

        val description = plugin.getAccessibilityDescription(graph)
        assertTrue(description.isNotEmpty())
        assertTrue(description.contains("Bar chart"))
        assertTrue(description.contains("Revenue"))
    }

    @Test
    fun getDataSummaryReturnsStats() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"},
                {"x": "3", "y": "150"}
            ]
        }
        """.trimIndent())!!

        val summary = plugin.getDataSummary(graph)
        assertEquals(3, summary.count)
        assertEquals(100f, summary.min)
        assertEquals(200f, summary.max)
    }

    @Test
    fun renderExplorationHaptic() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [{"x": "1", "y": "100"}]
        }
        """.trimIndent())!!

        val output = plugin.renderExplorationHaptic(graph, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun renderExplorationAudio() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [{"x": "1", "y": "100"}]
        }
        """.trimIndent())!!

        val output = plugin.renderExplorationAudio(graph, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun renderExplorationVoice() {
        val plugin = GraphPlugin()
        val graph = plugin.fromJson("""
        {
            "type": "line_chart",
            "data": [{"x": "1", "y": "100"}]
        }
        """.trimIndent())!!

        val output = plugin.renderExplorationVoice(graph, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun detectFromJsonHandlesAllChartTypes() {
        val plugin = GraphPlugin()
        val types = listOf(
            "line_chart" to GraphType.LINE_CHART,
            "bar_chart" to GraphType.BAR_CHART,
            "pie_chart" to GraphType.PIE_CHART,
            "scatter_plot" to GraphType.SCATTER_PLOT,
            "area_chart" to GraphType.AREA_CHART
        )

        for ((typeStr, expectedType) in types) {
            val json = """
            {
                "type": "$typeStr",
                "data": [{"x": "1", "y": "100"}]
            }
            """.trimIndent()

            val graph = plugin.fromJson(json)
            assertNotNull(graph, "Failed for type: $typeStr")
            assertEquals(expectedType, graph.graphType, "Wrong type for: $typeStr")
        }
    }
}

class GraphRendererTest {

    @Test
    fun renderHapticLineChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART)
        val output = renderer.renderHaptic(graph)
        assertNotNull(output)
        assertEquals(3, output.pulses.size)
    }

    @Test
    fun renderHapticBarChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.BAR_CHART)
        val output = renderer.renderHaptic(graph)
        assertNotNull(output)
        assertEquals(3, output.pulses.size)
        assertTrue(output.pulses.all { it.duration == 100L })
    }

    @Test
    fun renderHapticPieChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.PIE_CHART, labels = listOf("A", "B"))
        val output = renderer.renderHaptic(graph)
        assertNotNull(output)
        assertEquals(2, output.pulses.size)
    }

    @Test
    fun renderHapticScatterPlot() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.SCATTER_PLOT)
        val output = renderer.renderHaptic(graph)
        assertNotNull(output)
        assertEquals(3, output.pulses.size)
        assertTrue(output.pulses.all { it.intensity == 0.7f })
    }

    @Test
    fun renderAudioLineChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART)
        val output = renderer.renderAudio(graph)
        assertNotNull(output)
        assertEquals(3, output.sources.size)
        assertTrue(output.spatial)
    }

    @Test
    fun renderVoiceLineChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART)
        val output = renderer.renderVoice(graph)
        assertNotNull(output)
        assertTrue(output.speech.text.contains("Line chart"))
        assertTrue(output.speech.text.contains("3 data points"))
    }

    @Test
    fun renderVegaLiteSpecLineChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART, title = "Test")
        val spec = renderer.renderVegaLiteSpec(graph)
        assertTrue(spec.contains("vega-lite"))
        assertTrue(spec.contains("line"))
        assertTrue(spec.contains("Test"))
    }

    @Test
    fun renderVegaLiteSpecBarChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.BAR_CHART, title = "Bars")
        val spec = renderer.renderVegaLiteSpec(graph)
        assertTrue(spec.contains("bar"))
        assertTrue(spec.contains("Bars"))
    }

    @Test
    fun renderVegaLiteSpecPieChart() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(
            graphType = GraphType.PIE_CHART,
            labels = listOf("A", "B", "C"),
            dataPoints = listOf(DataPoint(1f, 30f), DataPoint(2f, 50f), DataPoint(3f, 20f))
        )
        val spec = renderer.renderVegaLiteSpec(graph)
        assertTrue(spec.contains("arc"))
    }

    @Test
    fun renderVegaLiteSpecScatterPlot() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(graphType = GraphType.SCATTER_PLOT)
        val spec = renderer.renderVegaLiteSpec(graph)
        assertTrue(spec.contains("point"))
    }

    @Test
    fun getAccessibilityDescriptionIncludesDetails() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(
            graphType = GraphType.BAR_CHART,
            title = "Revenue",
            axes = Axes(x = Axis(label = "Quarter", range = 0f..100f), y = Axis(label = "Amount", range = 0f..100f))
        )
        val desc = renderer.getAccessibilityDescription(graph)
        assertTrue(desc.contains("Bar chart"))
        assertTrue(desc.contains("Revenue"))
        assertTrue(desc.contains("Quarter"))
        assertTrue(desc.contains("Amount"))
    }

    @Test
    fun getDataSummaryComputesCorrectly() {
        val renderer = GraphRenderer()
        val graph = TestFixtures.graphContent(
            dataPoints = listOf(DataPoint(1f, 10f), DataPoint(2f, 20f), DataPoint(3f, 30f))
        )
        val summary = renderer.getDataSummary(graph)
        assertEquals(3, summary.count)
        assertEquals(10f, summary.min)
        assertEquals(30f, summary.max)
        assertEquals(20f, summary.mean)
    }
}

class GraphDataParserTest {

    private val parser = GraphDataParser()

    @Test
    fun parseJsonLineChart() {
        val json = """
        {
            "type": "line_chart",
            "title": "Sales",
            "x_label": "Month",
            "y_label": "Revenue",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"},
                {"x": "3", "y": "150"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(GraphType.LINE_CHART, result.graph.graphType)
        assertEquals("Sales", result.graph.title)
        assertEquals("Month", result.graph.axes.x.label)
        assertEquals("Revenue", result.graph.axes.y.label)
        assertEquals(3, result.graph.dataPoints.size)
        assertEquals(100f, result.graph.dataPoints[0].y)
        assertEquals(200f, result.graph.dataPoints[1].y)
    }

    @Test
    fun parseJsonBarChart() {
        val json = """
        {
            "type": "bar_chart",
            "title": "Quarterly",
            "data": [
                {"x": "Q1", "y": "100"},
                {"x": "Q2", "y": "200"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(GraphType.BAR_CHART, result.graph.graphType)
        assertEquals(2, result.graph.dataPoints.size)
    }

    @Test
    fun parseJsonPieChart() {
        val json = """
        {
            "type": "pie_chart",
            "title": "Distribution",
            "data": [
                {"x": "Product A", "y": "40"},
                {"x": "Product B", "y": "35"},
                {"x": "Product C", "y": "25"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(GraphType.PIE_CHART, result.graph.graphType)
        assertEquals(3, result.graph.dataPoints.size)
    }

    @Test
    fun parseJsonScatterPlot() {
        val json = """
        {
            "type": "scatter_plot",
            "data": [
                {"x": "1.5", "y": "2.3"},
                {"x": "3.7", "y": "4.1"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(GraphType.SCATTER_PLOT, result.graph.graphType)
    }

    @Test
    fun parseJsonAreaChart() {
        val json = """
        {
            "type": "area_chart",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "150"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(GraphType.AREA_CHART, result.graph.graphType)
    }

    @Test
    fun parseJsonWithLabels() {
        val json = """
        {
            "type": "pie_chart",
            "labels": ["A", "B", "C"],
            "data": [
                {"x": "1", "y": "30"},
                {"x": "2", "y": "50"},
                {"x": "3", "y": "20"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertEquals(3, result.graph.labels.size)
        assertEquals("A", result.graph.labels[0])
    }

    @Test
    fun parseJsonInfersTypeWhenMissing() {
        val json = """
        {
            "title": "Auto Type",
            "data": [
                {"x": "1", "y": "100"},
                {"x": "2", "y": "200"}
            ]
        }
        """.trimIndent()

        val result = parser.parseJson(json)
        assertTrue(result.inferredType)
        assertNotNull(result.graph.graphType)
    }

    @Test
    fun parseJsonEmptyThrows() {
        assertFailsWith<GraphDataException> {
            parser.parseJson("")
        }
    }

    @Test
    fun parseJsonInvalidSyntaxThrows() {
        assertFailsWith<GraphDataException> {
            parser.parseJson("{invalid json}")
        }
    }

    @Test
    fun parseJsonNonObjectThrows() {
        assertFailsWith<GraphDataException> {
            parser.parseJson("[1, 2, 3]")
        }
    }

    @Test
    fun parseCsvBasic() {
        val csv = """
        Month,Revenue
        Jan,100
        Feb,200
        Mar,150
        """.trimIndent()

        val result = parser.parseCsv(csv, title = "Sales")
        assertEquals("Sales", result.graph.title)
        assertEquals(3, result.graph.dataPoints.size)
    }

    @Test
    fun parseCsvWithChartType() {
        val csv = """
        Category,Value
        A,100
        B,200
        """.trimIndent()

        val result = parser.parseCsv(csv, chartType = "bar_chart")
        assertEquals(GraphType.BAR_CHART, result.graph.graphType)
    }

    @Test
    fun parseCsvEmptyThrows() {
        assertFailsWith<GraphDataException> {
            parser.parseCsv("")
        }
    }

    @Test
    fun parseCsvHeaderOnlyThrows() {
        assertFailsWith<GraphDataException> {
            parser.parseCsv("X,Y")
        }
    }

    @Test
    fun parseDataPoints() {
        val graph = parser.parseDataPoints(
            graphType = "line_chart",
            title = "Test",
            xLabel = "X",
            yLabel = "Y",
            dataPoints = listOf(1 to 100, 2 to 200, 3 to 150)
        )
        assertEquals(GraphType.LINE_CHART, graph.graphType)
        assertEquals("Test", graph.title)
        assertEquals(3, graph.dataPoints.size)
    }

    @Test
    fun resolveGraphTypeHandlesAllTypes() {
        assertEquals(GraphType.LINE_CHART, parser.resolveGraphType("line_chart"))
        assertEquals(GraphType.BAR_CHART, parser.resolveGraphType("bar_chart"))
        assertEquals(GraphType.PIE_CHART, parser.resolveGraphType("pie_chart"))
        assertEquals(GraphType.SCATTER_PLOT, parser.resolveGraphType("scatter_plot"))
        assertEquals(GraphType.AREA_CHART, parser.resolveGraphType("area_chart"))
        assertEquals(GraphType.HISTOGRAM, parser.resolveGraphType("histogram"))
        assertFailsWith<GraphDataException> {
            parser.resolveGraphType("unknown")
        }
    }

    @Test
    fun inferChartTypeDefaults() {
        assertEquals("line_chart", parser.inferChartType(emptyList()))
        assertEquals("line_chart", parser.inferChartType(listOf(DataPointInput("1", "100"))))
    }
}

class DataExtractorTest {

    private val extractor = DataExtractor()

    @Test
    fun fromJsonReturnsResult() {
        val json = """
        {
            "type": "line_chart",
            "data": [{"x": "1", "y": "100"}]
        }
        """.trimIndent()

        val result = extractor.fromJson(json)
        assertEquals(DataSource.JSON, result.source)
        assertNotNull(result.graph)
    }

    @Test
    fun fromCsvReturnsResult() {
        val csv = """
        X,Y
        1,100
        2,200
        """.trimIndent()

        val result = extractor.fromCsv(csv)
        assertEquals(DataSource.CSV, result.source)
        assertEquals(2, result.graph.dataPoints.size)
    }

    @Test
    fun fromDataPointsReturnsResult() {
        val result = extractor.fromDataPoints(
            type = "bar_chart",
            title = "Test",
            points = listOf(1 to 100, 2 to 200)
        )
        assertEquals(DataSource.PROGRAMMATIC, result.source)
        assertEquals(GraphType.BAR_CHART, result.graph.graphType)
    }

    @Test
    fun fromPropertiesReturnsResult() {
        val props = mapOf(
            "type" to "line_chart",
            "title" to "From Props",
            "x_label" to "X",
            "y_label" to "Y",
            "data" to "[{\"x\":\"1\",\"y\":\"100\"},{\"x\":\"2\",\"y\":\"200\"}]"
        )
        val result = extractor.fromProperties(props)
        assertEquals(DataSource.PROPERTIES, result.source)
        assertEquals("From Props", result.graph.title)
    }

    @Test
    fun autoDetectJson() {
        val input = """
        {
            "type": "line_chart",
            "data": [{"x": "1", "y": "100"}]
        }
        """.trimIndent()

        val result = extractor.autoDetect(input)
        assertNotNull(result.graph)
    }

    @Test
    fun autoDetectCsv() {
        val input = """
        X,Y
        1,100
        2,200
        """.trimIndent()

        val result = extractor.autoDetect(input)
        assertNotNull(result.graph)
        assertEquals(2, result.graph.dataPoints.size)
    }

    @Test
    fun autoDetectUnknownThrows() {
        assertFailsWith<GraphDataException> {
            extractor.autoDetect("just some text without commas or newlines")
        }
    }
}

class VegaLiteSpecTest {

    private val specGenerator = VegaLiteSpec()

    @Test
    fun generateLineChartSpec() {
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART, title = "Test")
        val spec = specGenerator.generate(graph)
        assertTrue(spec.containsKey("\$schema"))
        assertTrue(spec.containsKey("mark"))
        assertTrue(spec.containsKey("encoding"))
        assertTrue(spec.containsKey("data"))
    }

    @Test
    fun generateBarChartSpec() {
        val graph = TestFixtures.graphContent(graphType = GraphType.BAR_CHART)
        val spec = specGenerator.generate(graph)
        val mark = spec["mark"]
        assertNotNull(mark)
        assertTrue(mark.toString().contains("bar"))
    }

    @Test
    fun generatePieChartSpec() {
        val graph = TestFixtures.graphContent(
            graphType = GraphType.PIE_CHART,
            labels = listOf("A", "B"),
            dataPoints = listOf(DataPoint(1f, 30f), DataPoint(2f, 70f))
        )
        val spec = specGenerator.generate(graph)
        val mark = spec["mark"]
        assertNotNull(mark)
        assertTrue(mark.toString().contains("arc"))
    }

    @Test
    fun generateScatterPlotSpec() {
        val graph = TestFixtures.graphContent(graphType = GraphType.SCATTER_PLOT)
        val spec = specGenerator.generate(graph)
        val mark = spec["mark"]
        assertNotNull(mark)
        assertTrue(mark.toString().contains("point"))
    }

    @Test
    fun generateAreaChartSpec() {
        val graph = TestFixtures.graphContent(graphType = GraphType.AREA_CHART)
        val spec = specGenerator.generate(graph)
        val mark = spec["mark"]
        assertNotNull(mark)
        assertTrue(mark.toString().contains("area"))
    }

    @Test
    fun generateStringReturnsValidJson() {
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART)
        val specStr = specGenerator.generateString(graph)
        assertTrue(specStr.startsWith("{"))
        assertTrue(specStr.endsWith("}"))
        assertTrue(specStr.contains("vega-lite"))
    }

    @Test
    fun markTypeForGraphReturnsCorrectTypes() {
        assertEquals("line", specGenerator.markTypeForGraph(GraphType.LINE_CHART))
        assertEquals("bar", specGenerator.markTypeForGraph(GraphType.BAR_CHART))
        assertEquals("arc", specGenerator.markTypeForGraph(GraphType.PIE_CHART))
        assertEquals("point", specGenerator.markTypeForGraph(GraphType.SCATTER_PLOT))
        assertEquals("area", specGenerator.markTypeForGraph(GraphType.AREA_CHART))
        assertEquals("bar", specGenerator.markTypeForGraph(GraphType.HISTOGRAM))
    }

    @Test
    fun specIncludesTitleWhenPresent() {
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART, title = "My Chart")
        val spec = specGenerator.generate(graph)
        val title = spec["title"]
        assertNotNull(title)
        assertTrue(title.toString().contains("My Chart"))
    }

    @Test
    fun specExcludesTitleWhenEmpty() {
        val graph = TestFixtures.graphContent(graphType = GraphType.LINE_CHART, title = "")
        val spec = specGenerator.generate(graph)
        assertFalse(spec.containsKey("title"))
    }
}

class DataSummaryTest {

    @Test
    fun computeDataSummaryBasic() {
        val points = listOf(
            DataPoint(1f, 10f),
            DataPoint(2f, 20f),
            DataPoint(3f, 30f)
        )
        val summary = computeDataSummary(points)
        assertEquals(3, summary.count)
        assertEquals(10f, summary.min)
        assertEquals(30f, summary.max)
        assertEquals(20f, summary.mean)
        assertEquals(20f, summary.median)
    }

    @Test
    fun computeDataSummaryEmpty() {
        val summary = computeDataSummary(emptyList())
        assertEquals(0, summary.count)
        assertEquals(0f, summary.min)
        assertEquals(0f, summary.max)
    }

    @Test
    fun computeDataSummarySinglePoint() {
        val summary = computeDataSummary(listOf(DataPoint(1f, 42f)))
        assertEquals(1, summary.count)
        assertEquals(42f, summary.min)
        assertEquals(42f, summary.max)
        assertEquals(42f, summary.mean)
    }

    @Test
    fun computeDataSummaryEvenCount() {
        val points = listOf(
            DataPoint(1f, 10f),
            DataPoint(2f, 20f),
            DataPoint(3f, 30f),
            DataPoint(4f, 40f)
        )
        val summary = computeDataSummary(points)
        assertEquals(25f, summary.median) // (20 + 30) / 2
    }

    @Test
    fun trendIncreasing() {
        val points = listOf(
            DataPoint(1f, 10f),
            DataPoint(2f, 20f),
            DataPoint(3f, 50f)
        )
        val summary = computeDataSummary(points)
        assertEquals(TrendDirection.INCREASING, summary.trend)
    }

    @Test
    fun trendDecreasing() {
        val points = listOf(
            DataPoint(1f, 50f),
            DataPoint(2f, 30f),
            DataPoint(3f, 10f)
        )
        val summary = computeDataSummary(points)
        assertEquals(TrendDirection.DECREASING, summary.trend)
    }

    @Test
    fun trendStable() {
        // Need large spread (range=200) relative to endpoint change (first=100, last=105, delta=5)
        // changeRatio = 5/200 = 0.025, which is between -0.2 and 0.2 → STABLE
        val points = listOf(
            DataPoint(1f, 100f),
            DataPoint(2f, 200f),
            DataPoint(3f, 105f)
        )
        val summary = computeDataSummary(points)
        assertEquals(TrendDirection.STABLE, summary.trend)
    }
}

class AccessibilityDescriptionTest {

    @Test
    fun lineChartDescription() {
        val graph = TestFixtures.graphContent(
            graphType = GraphType.LINE_CHART,
            title = "Sales Trend",
            axes = Axes(
                x = Axis(label = "Month", range = 0f..100f),
                y = Axis(label = "Revenue", range = 0f..100f)
            )
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("Line chart"))
        assertTrue(desc.contains("Sales Trend"))
        assertTrue(desc.contains("Month"))
        assertTrue(desc.contains("Revenue"))
        assertTrue(desc.contains("3 data points"))
    }

    @Test
    fun barChartDescription() {
        val graph = TestFixtures.graphContent(
            graphType = GraphType.BAR_CHART,
            title = "Quarterly"
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("Bar chart"))
        assertTrue(desc.contains("Quarterly"))
    }

    @Test
    fun pieChartDescription() {
        val graph = TestFixtures.graphContent(
            graphType = GraphType.PIE_CHART,
            labels = listOf("A", "B")
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("Pie chart"))
    }

    @Test
    fun scatterPlotDescription() {
        val graph = TestFixtures.graphContent(
            graphType = GraphType.SCATTER_PLOT,
            title = "Correlation"
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("Scatter plot"))
        assertTrue(desc.contains("Correlation"))
    }

    @Test
    fun descriptionIncludesTrend() {
        val graph = TestFixtures.graphContent(
            dataPoints = listOf(
                DataPoint(1f, 10f),
                DataPoint(2f, 20f),
                DataPoint(3f, 50f)
            )
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("increasing"))
    }

    @Test
    fun descriptionIncludesDataRange() {
        val graph = TestFixtures.graphContent(
            dataPoints = listOf(
                DataPoint(1f, 10f),
                DataPoint(2f, 100f)
            )
        )
        val desc = generateAccessibilityDescription(graph)
        assertTrue(desc.contains("10"))
        assertTrue(desc.contains("100"))
    }
}
