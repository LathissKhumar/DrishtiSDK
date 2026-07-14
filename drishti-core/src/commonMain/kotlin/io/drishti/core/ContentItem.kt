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

/**
 * A detected content item from the vision pipeline.
 */
public interface ContentItem {
    public val contentType: ContentType
    public val confidence: Float
}

/**
 * Graph content types.
 */
public enum class GraphType {
    LINE_CHART,
    BAR_CHART,
    PIE_CHART,
    SCATTER_PLOT,
    AREA_CHART,
    HISTOGRAM
}

/**
 * Graph content item.
 *
 * @param confidence Detection confidence — callers must always pass the
 *   observed confidence from the detector. The default exists only for
 *   backward-compatible data-class construction; production code must
 *   not rely on it.
 */
public data class GraphContent(
    val graphType: GraphType,
    val title: String = "",
    val axes: Axes = Axes(),
    val dataPoints: List<DataPoint> = emptyList(),
    val labels: List<String> = emptyList(),
    override val confidence: Float
) : ContentItem {
    override val contentType: ContentType = ContentType.Graph
}

/**
 * Formula types.
 */
public enum class FormulaType {
    ALGEBRAIC,
    TRIGONOMETRIC,
    CALCULUS,
    MATHEMATICAL,
    NOTATION
}

/**
 * Symbol types in formulas.
 */
public enum class SymbolType {
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

public interface FormulaContentItem : ContentItem {
    public val formulaType: FormulaType
    public val expression: String
    public val symbols: List<FormulaSymbol>
    public val geometry: Geometry?
}

/**
 * Formula content item.
 *
 * @param confidence Detection confidence — callers must always pass the
 *   observed confidence from the detector.
 */
public data class FormulaContent(
    override val formulaType: FormulaType,
    override val expression: String,
    override val symbols: List<FormulaSymbol> = emptyList(),
    override val geometry: Geometry? = null,
    override val confidence: Float
) : FormulaContentItem {
    override val contentType: ContentType = ContentType.Formula
}

/**
 * A symbol in a formula.
 */
public data class FormulaSymbol(
    val type: SymbolType,
    val position: Point,
    val boundingBox: BoundingBox,
    val value: String
)

/**
 * Molecule types.
 */
public enum class MoleculeType {
    ORGANIC,
    INORGANIC,
    COMPLEX,
    SIMPLE
}

/**
 * Bond types in molecules.
 */
public enum class BondType {
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
public data class Atom(
    val id: Int,
    val element: String,
    val position: Point,
    val charge: Int = 0,
    val label: String = element,
    val z: Float = 0.0f
)

/**
 * A bond between atoms.
 */
public data class Bond(
    val from: Int,
    val to: Int,
    val type: BondType,
    val strength: Float = 1.0f
)

/**
 * Molecule content item.
 *
 * @param confidence Detection confidence — callers must always pass the
 *   observed confidence from the detector.
 */
public data class MoleculeContent(
    val moleculeType: MoleculeType,
    val atoms: List<Atom> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val name: String = "",
    val geometry: Geometry? = null,
    val cid: Int = 0,
    val molecularFormula: String = "",
    val molecularWeight: Double = 0.0,
    val iupacName: String = "",
    val canonicalSmiles: String = "",
    val inchiKey: String = "",
    override val confidence: Float
) : ContentItem {
    override val contentType: ContentType = ContentType.Molecule
}

/**
 * Shape types for geometric content.
 */
public enum class ShapeType {
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
 *
 * @param shapeType Classified shape geometry.
 * @param area Enclosed area in square pixels.
 * @param perimeter Perimeter length in pixels.
 * @param x Horizontal position of the bounding-box origin in pixels (0 = left edge).
 * @param y Vertical position of the bounding-box origin in pixels (0 = top edge).
 * @param width Width of the bounding box in pixels.
 * @param height Height of the bounding box in pixels.
 * @param confidence Detection confidence between 0.0 and 1.0.
 */
public data class ShapeContent(
    val shapeType: ShapeType,
    val area: Float,
    val perimeter: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    override val confidence: Float
) : ContentItem {
    override val contentType: ContentType = ContentType.Shape
}

/**
 * Table content item.
 *
 * @param confidence Detection confidence — callers must always pass the
 *   observed confidence from the detector.
 */
public data class TableContent(
    val rows: Int,
    val columns: Int,
    val cells: List<List<String>>,
    override val confidence: Float
) : ContentItem {
    override val contentType: ContentType = ContentType.Table
}

/**
 * Exploration direction for interactive content.
 */
public enum class ExplorationDirection {
    NEXT,
    PREVIOUS,
    POSITION
}
