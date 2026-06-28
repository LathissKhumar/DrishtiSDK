package io.drishti.core

/**
 * A detected content item from the vision pipeline.
 */
interface ContentItem {
    val contentType: ContentType
    val confidence: Float
}

/**
 * Graph content types.
 */
enum class GraphType {
    LINE_CHART,
    BAR_CHART,
    PIE_CHART,
    SCATTER_PLOT,
    AREA_CHART,
    HISTOGRAM
}

/**
 * Graph content item.
 */
data class GraphContent(
    val graphType: GraphType,
    val title: String = "",
    val axes: Axes = Axes(),
    val dataPoints: List<DataPoint> = emptyList(),
    val labels: List<String> = emptyList()
) : ContentItem {
    override val contentType = ContentType.GRAPH
    override val confidence = 0.85f
}

/**
 * Formula types.
 */
enum class FormulaType {
    ALGEBRAIC,
    TRIGONOMETRIC,
    CALCULUS,
    MATHEMATICAL,
    NOTATION
}

/**
 * Symbol types in formulas.
 */
enum class SymbolType {
    NUMBER,
    VARIABLE,
    OPERATOR,
    FUNCTION,
    BRACKET,
    SUBSCRIPT,
    SUPERSCRIPT,
    FRACTION,
    SUMMATION,
    INTEGRAL,
    GREEK_LETTER,
    EQUALS,
    RELATION
}

/**
 * Formula content item.
 */
data class FormulaContent(
    val formulaType: FormulaType,
    val expression: String,
    val symbols: List<FormulaSymbol> = emptyList(),
    val geometry: Geometry? = null
) : ContentItem {
    override val contentType = ContentType.FORMULA
    override val confidence = 0.88f
}

/**
 * A symbol in a formula.
 */
data class FormulaSymbol(
    val type: SymbolType,
    val position: Point,
    val boundingBox: BoundingBox,
    val value: String
)

/**
 * Molecule types.
 */
enum class MoleculeType {
    ORGANIC,
    INORGANIC,
    COMPLEX,
    SIMPLE
}

/**
 * Bond types in molecules.
 */
enum class BondType {
    SINGLE,
    DOUBLE,
    TRIPLE,
    AROMATIC,
    IONIC,
    HYDROGEN
}

/**
 * An atom in a molecule.
 */
data class Atom(
    val id: Int,
    val element: String,
    val position: Point,
    val charge: Int = 0,
    val label: String = element
)

/**
 * A bond between atoms.
 */
data class Bond(
    val from: Int,
    val to: Int,
    val type: BondType,
    val strength: Float = 1.0f
)

/**
 * Molecule content item.
 */
data class MoleculeContent(
    val moleculeType: MoleculeType,
    val atoms: List<Atom> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val name: String = "",
    val geometry: Geometry? = null
) : ContentItem {
    override val contentType = ContentType.MOLECULE
    override val confidence = 0.92f
}

/**
 * Shape types for geometric content.
 */
enum class ShapeType {
    CIRCLE,
    RECTANGLE,
    TRIANGLE,
    POLYGON,
    ELLIPSE,
    LINE,
    UNKNOWN
}

/**
 * Shape content item.
 */
data class ShapeContent(
    val shapeType: ShapeType,
    val area: Float,
    val perimeter: Float
) : ContentItem {
    override val contentType = ContentType.SHAPE
    override val confidence = 0.85f
}

/**
 * Table content item.
 */
data class TableContent(
    val rows: Int,
    val columns: Int,
    val cells: List<List<String>>
) : ContentItem {
    override val contentType = ContentType.TABLE
    override val confidence = 0.9f
}

/**
 * Exploration direction for interactive content.
 */
enum class ExplorationDirection {
    NEXT,
    PREVIOUS,
    POSITION
}
