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
            expression = expression,
            confidence = 0.85f
        )
    }
}
