package io.drishti.formula

/**
 * Thrown when the LaTeX parser encounters input it cannot process.
 *
 * @property message Human-readable description of the parse failure
 * @property position Character offset in the input string where the error was detected, or -1 if unknown
 */
class FormulaParseException(
    message: String,
    val position: Int = -1,
    cause: Throwable? = null
) : Exception(message, cause)
