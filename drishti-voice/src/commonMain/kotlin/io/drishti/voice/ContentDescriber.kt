package io.drishti.voice

import io.drishti.core.*

/**
 * Generates descriptions for content items.
 */
class ContentDescriber {
    /**
     * Describe a graph content item.
     */
    fun describeGraph(graph: GraphContent): String {
        return buildString {
            append("${graph.graphType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} ")
            if (graph.title.isNotEmpty()) {
                append("titled '${graph.title}' ")
            }
            append("with ${graph.dataPoints.size} data points. ")
            append("X-axis: ${graph.axes.x.label}, Y-axis: ${graph.axes.y.label}. ")
        }
    }

    /**
     * Describe a formula content item.
     */
    fun describeFormula(formula: FormulaContent): String {
        return buildString {
            append("${formula.formulaType.name.lowercase().replaceFirstChar { it.uppercase() }} formula ")
            append("containing ${formula.symbols.size} symbols. ")
            append("Expression: ${formula.expression}. ")
        }
    }

    /**
     * Describe a molecule content item.
     */
    fun describeMolecule(molecule: MoleculeContent): String {
        return buildString {
            append("Molecule ")
            if (molecule.name.isNotEmpty()) {
                append("named '${molecule.name}' ")
            }
            append("with ${molecule.atoms.size} atoms and ${molecule.bonds.size} bonds. ")
        }
    }

    /**
     * Describe a shape content item.
     */
    fun describeShape(shape: ShapeContent): String {
        return buildString {
            append("${shape.shapeType.name.lowercase().replaceFirstChar { it.uppercase() }} shape ")
            append("with area ${"%.1f".format(shape.area)}. ")
        }
    }

    /**
     * Describe a table content item.
     */
    fun describeTable(table: TableContent): String {
        return buildString {
            append("Table with ${table.rows} rows and ${table.columns} columns. ")
        }
    }
}
