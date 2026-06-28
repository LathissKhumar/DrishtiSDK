package io.drishti.core

import kotlin.test.*

class ContentItemTest {

    @Test
    fun graphContentHasCorrectContentType() {
        val content = TestFixtures.graphContent()
        assertEquals(ContentType.GRAPH, content.contentType)
        assertEquals(0.85f, content.confidence)
    }

    @Test
    fun graphContentDefaults() {
        val content = GraphContent(graphType = GraphType.LINE_CHART)
        assertEquals(GraphType.LINE_CHART, content.graphType)
        assertEquals("", content.title)
        assertEquals(Axes(), content.axes)
        assertTrue(content.dataPoints.isEmpty())
        assertTrue(content.labels.isEmpty())
    }

    @Test
    fun graphContentWithCustomValues() {
        val points = listOf(DataPoint(1f, 2f), DataPoint(3f, 4f))
        val content = TestFixtures.graphContent(
            graphType = GraphType.BAR_CHART,
            title = "Sales",
            dataPoints = points,
            labels = listOf("Q1", "Q2")
        )
        assertEquals(GraphType.BAR_CHART, content.graphType)
        assertEquals("Sales", content.title)
        assertEquals(2, content.dataPoints.size)
        assertEquals(listOf("Q1", "Q2"), content.labels)
    }

    @Test
    fun formulaContentHasCorrectContentType() {
        val content = TestFixtures.formulaContent()
        assertEquals(ContentType.FORMULA, content.contentType)
        assertEquals(0.88f, content.confidence)
    }

    @Test
    fun formulaContentWithSymbols() {
        val symbols = listOf(
            FormulaSymbol(SymbolType.VARIABLE, Point(10f, 10f), BoundingBox(5f, 5f, 20f, 20f), "x"),
            FormulaSymbol(SymbolType.OPERATOR, Point(40f, 10f), BoundingBox(35f, 5f, 10f, 20f), "+"),
            FormulaSymbol(SymbolType.NUMBER, Point(70f, 10f), BoundingBox(65f, 5f, 20f, 20f), "3")
        )
        val content = TestFixtures.formulaContent(expression = "x + 3", symbols = symbols)
        assertEquals("x + 3", content.expression)
        assertEquals(3, content.symbols.size)
        assertEquals(SymbolType.VARIABLE, content.symbols[0].type)
        assertEquals("x", content.symbols[0].value)
    }

    @Test
    fun moleculeContentHasCorrectContentType() {
        val content = TestFixtures.moleculeContent()
        assertEquals(ContentType.MOLECULE, content.contentType)
        assertEquals(0.92f, content.confidence)
    }

    @Test
    fun moleculeContentWithAtomsAndBonds() {
        val atoms = listOf(
            Atom(0, "C", Point(50f, 50f)),
            Atom(1, "H", Point(30f, 30f))
        )
        val bonds = listOf(Bond(0, 1, BondType.SINGLE))
        val content = TestFixtures.moleculeContent(atoms = atoms, bonds = bonds, name = "CH")
        assertEquals("CH", content.name)
        assertEquals(2, content.atoms.size)
        assertEquals(1, content.bonds.size)
        assertEquals("C", content.atoms[0].element)
        assertEquals(BondType.SINGLE, content.bonds[0].type)
    }

    @Test
    fun shapeContentHasCorrectContentType() {
        val content = ShapeContent(ShapeType.CIRCLE, area = 100f, perimeter = 35.4f)
        assertEquals(ContentType.SHAPE, content.contentType)
        assertEquals(ShapeType.CIRCLE, content.shapeType)
        assertEquals(100f, content.area)
    }

    @Test
    fun tableContentHasCorrectContentType() {
        val cells = listOf(listOf("A", "B"), listOf("1", "2"))
        val content = TableContent(rows = 2, columns = 2, cells = cells)
        assertEquals(ContentType.TABLE, content.contentType)
        assertEquals(2, content.rows)
        assertEquals(2, content.columns)
        assertEquals("A", content.cells[0][0])
    }

    @Test
    fun graphTypeAllValues() {
        val values = GraphType.entries
        assertEquals(6, values.size)
        assertTrue(values.contains(GraphType.LINE_CHART))
        assertTrue(values.contains(GraphType.BAR_CHART))
        assertTrue(values.contains(GraphType.PIE_CHART))
        assertTrue(values.contains(GraphType.SCATTER_PLOT))
        assertTrue(values.contains(GraphType.AREA_CHART))
        assertTrue(values.contains(GraphType.HISTOGRAM))
    }

    @Test
    fun formulaTypeAllValues() {
        val values = FormulaType.entries
        assertEquals(5, values.size)
    }

    @Test
    fun moleculeTypeAllValues() {
        val values = MoleculeType.entries
        assertEquals(4, values.size)
    }

    @Test
    fun symbolTypeAllValues() {
        val values = SymbolType.entries
        assertEquals(13, values.size)
    }

    @Test
    fun explorationDirectionAllValues() {
        val values = ExplorationDirection.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(ExplorationDirection.NEXT))
        assertTrue(values.contains(ExplorationDirection.PREVIOUS))
        assertTrue(values.contains(ExplorationDirection.POSITION))
    }
}
