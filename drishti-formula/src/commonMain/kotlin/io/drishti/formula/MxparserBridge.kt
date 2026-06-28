package io.drishti.formula

/**
 * Bridge to the mXparser math evaluation engine.
 *
 * Uses expect/actual to bridge between common Kotlin code and the
 * Java-based mXparser library.
 */
expect object MxparserBridge {
    /**
     * Evaluate a math expression string with optional variable bindings.
     *
     * @param expression mXparser-compatible expression (e.g. "(2)+(3)")
     * @param variables Variable name → value bindings
     * @return Numeric result, or NaN if evaluation fails
     */
    fun evaluate(expression: String, variables: Map<String, Double>): Double
}
