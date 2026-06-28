package io.drishti.formula

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression

/**
 * Android/Java implementation of the mXparser bridge.
 *
 * Wraps the mXparser library for mathematical expression evaluation.
 */
actual object MxparserBridge {
    private val varNameRegex = Regex("[a-zA-Z_][a-zA-Z0-9_]*")

    actual fun evaluate(expression: String, variables: Map<String, Double>): Double {
        val mExpr = Expression(expression)
        for ((name, value) in variables) {
            require(varNameRegex.matches(name)) { "Invalid variable name: $name" }
            mExpr.addArguments(Argument("$name = $value"))
        }
        return mExpr.calculate()
    }
}
