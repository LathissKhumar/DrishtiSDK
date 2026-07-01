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

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression

/**
 * Android/Java implementation of the mXparser bridge.
 *
 * Wraps the mXparser library for mathematical expression evaluation.
 */
public actual object MxparserBridge {
    private val varNameRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")

    public actual fun evaluate(expression: String, variables: Map<String, Double>): Double {
        val mExpr = Expression(expression)
        for ((name, value) in variables) {
            require(varNameRegex.matches(name)) { "Invalid variable name: $name" }
            mExpr.addArguments(Argument("$name = $value"))
        }
        return mExpr.calculate()
    }
}
