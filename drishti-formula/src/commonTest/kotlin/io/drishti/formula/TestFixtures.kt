package io.drishti.formula

import io.drishti.core.FormulaContent
import io.drishti.core.FormulaType

object TestFixtures {

    fun formulaContent(formulaType: FormulaType = FormulaType.ALGEBRAIC): FormulaContent {
        val expression = when (formulaType) {
            FormulaType.ALGEBRAIC -> "x + y = z"
            FormulaType.TRIGONOMETRIC -> "\\sin(x) + \\cos(y)"
            FormulaType.CALCULUS -> "\\int_{0}^{1} x^{2} dx"
            FormulaType.MATHEMATICAL -> "\\frac{a}{b}"
            FormulaType.NOTATION -> "\\alpha + \\beta"
        }
        return FormulaContent(
            formulaType = formulaType,
            expression = expression
        )
    }
}
