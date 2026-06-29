package io.drishti.molecule

/**
 * Identifies the type of molecule input for routing to the correct PubChem endpoint.
 */
enum class MoleculeInputType {
    /** SMILES notation (e.g., "CCO", "c1ccccc1") */
    SMILES,
    /** Molecular formula (e.g., "C2H6O", "C₆H₁₂O₆") */
    MOLECULAR_FORMULA,
    /** IUPAC systematic name (e.g., "ethanol", "2-propanol") */
    IUPAC_NAME,
    /** Common name (e.g., "water", "caffeine") */
    COMMON_NAME,
    /** InChI identifier (e.g., "InChI=1S/C2H6O/c1-2-3/h3H,2H2,1H3") */
    INCHI
}

/**
 * Result of parsing a molecule input string.
 *
 * @property type Detected input type
 * @property value Original input value
 * @property normalizedValue Normalized value suitable for PubChem API
 */
data class ParsedMoleculeInput(
    val type: MoleculeInputType,
    val value: String,
    val normalizedValue: String
)

/**
 * Parser for molecule input strings.
 *
 * Detects and normalizes SMILES notation, molecular formulas (including
 * Unicode subscript variants), IUPAC names, common names, and InChI
 * identifiers. Routes parsed input to the appropriate PubChem endpoint.
 *
 * Unicode subscript handling: "C₆H₁₂O₆" is normalized to "C6H12O6".
 */
class MoleculeParser {

    private companion object {
        val FORMULA_PATTERN = Regex("^[A-Z][a-z]?[0-9]*(?:[A-Z][a-z]?[0-9]*)*$")
        val NAME_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9\\-() ]*$")
        val AROMATIC_SEQUENCE = Regex("[cnos]{2,}")
    }

    /**
     * Parse a molecule input string and determine its type.
     *
     * @param input Raw molecule input (name, formula, SMILES, or InChI)
     * @return [ParsedMoleculeInput] with detected type and normalized value
     */
    fun parse(input: String): ParsedMoleculeInput {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "Molecule input must not be blank" }
        return when {
            isInchi(trimmed) -> ParsedMoleculeInput(
                type = MoleculeInputType.INCHI,
                value = trimmed,
                normalizedValue = trimmed
            )
            isSmiles(trimmed) -> ParsedMoleculeInput(
                type = MoleculeInputType.SMILES,
                value = trimmed,
                normalizedValue = trimmed
            )
            isFormula(trimmed) -> ParsedMoleculeInput(
                type = MoleculeInputType.MOLECULAR_FORMULA,
                value = trimmed,
                normalizedValue = normalizeFormula(trimmed)
            )
            isLikelyChemicalName(trimmed) -> ParsedMoleculeInput(
                type = MoleculeInputType.COMMON_NAME,
                value = trimmed,
                normalizedValue = trimmed
            )
            else -> ParsedMoleculeInput(
                type = MoleculeInputType.COMMON_NAME,
                value = trimmed,
                normalizedValue = trimmed
            )
        }
    }

    /**
     * Normalize a molecular formula by converting Unicode subscripts to ASCII.
     *
     * Example: "C₆H₁₂O₆" → "C6H12O6"
     *
     * @param formula Molecular formula potentially containing Unicode subscripts
     * @return Normalized formula with ASCII digits
     */
    fun normalizeFormula(formula: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < formula.length) {
            val ch = formula[i]
            val digit = subscriptDigit(ch)
            if (digit != null) {
                sb.append(digit)
            } else {
                sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    /**
     * Detect whether the input is a SMILES string.
     *
     * SMILES detection uses heuristics:
     * - Contains SMILES-specific bond/bracket characters (=, #, @, [, ], /, \, %, +)
     * - Mixed aromatic pattern: lowercase aromatic atoms (c, n, o, s) with uppercase atom symbols
     * - Pure aromatic with ring closures: lowercase atoms + digits (e.g., c1ccccc1)
     * - Ring closures: letter followed by digit pattern, distinguishing from molecular formulas
     * - Branches: parentheses around atom sequences (e.g., CC(O)C)
     *
     * Note: Hyphens (-) and dots (.) are NOT primary SMILES indicators because they
     * also appear in IUPAC names (e.g., "2-propanol", "N-methyl").
     *
     * @param input Input string to test
     * @return `true` if input appears to be SMILES notation
     */
    fun isSmiles(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.length < 2) return false
        // Primary SMILES-specific characters (excluding '-' and '.' which appear in IUPAC names)
        val primarySmilesChars = setOf('=', '#', '@', '[', ']', '/', '\\', '%', '+')
        if (trimmed.any { it in primarySmilesChars }) return true
        // Must contain only SMILES-valid characters (letters, digits, parens, hyphens, dots)
        if (trimmed.any { !it.isLetterOrDigit() && it != '(' && it != ')' && it != '-' && it != '.' }) return false

        val hasAromaticSequence = AROMATIC_SEQUENCE.containsMatchIn(trimmed)
        val hasUppercaseAtoms = trimmed.any { it in "CNOSPFBI" }
        val hasDigits = trimmed.any { it.isDigit() }

        // All-uppercase atom chains (CCO, CN, CO, etc.) — can only be SMILES since
        // formulas require digits, names have lowercase letters, InChI starts with "InChI="
        if (trimmed.all { it.isUpperCase() } && !trimmed.contains(' ') && !trimmed.contains('-')) return true

        // Mixed aromatic: consecutive aromatic atoms with uppercase atom symbols (e.g., Cc1ccccc1)
        if (hasAromaticSequence && hasUppercaseAtoms) return true

        // Ring closures: letter followed by digit (not followed by more digits)
        // e.g., C1CCCCC1 (cyclohexane), c1ccccc1 (benzene)
        if (hasDigits && trimmed[0].isLetter() && !trimmed.contains('-')) {
            val hasRingClosure = trimmed.indices.any { i ->
                i < trimmed.length - 1 &&
                    trimmed[i].isLetter() && trimmed[i + 1].isDigit() &&
                    (i + 2 >= trimmed.length || trimmed[i + 2].isLetter())
            }
            if (hasRingClosure) {
                if (isFormula(trimmed)) {
                    // Single uppercase element type → ring closure (C1CCCCC1), not formula (C6H12O6)
                    val uniqueUppercase = trimmed.filter { it.isUpperCase() }.toSet()
                    if (uniqueUppercase.size == 1) return true
                } else {
                    // Not a formula pattern but has ring closure digits → SMILES
                    return true
                }
            }
        }

        // Branches: parentheses between atom sequences, no spaces (e.g., CC(O)C)
        if (trimmed.contains('(') && !trimmed.contains(' ') && (hasUppercaseAtoms || hasAromaticSequence)) return true

        return false
    }

    /**
     * Detect whether the input is a molecular formula.
     *
     * A formula starts with an uppercase letter (element symbol), may contain
     * digits, parentheses for groups, and Unicode subscript characters.
     *
     * @param input Input string to test
     * @return `true` if input matches a molecular formula pattern
     */
    fun isFormula(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        val normalized = normalizeFormula(trimmed)
        // Must start with uppercase letter
        if (!normalized[0].isUpperCase()) return false
        // Must contain at least one digit (otherwise it's just a name like "C")
        val hasDigit = normalized.any { it.isDigit() }
        // Can contain element symbols (uppercase followed by optional lowercase) and digits
        return hasDigit && FORMULA_PATTERN.matches(normalized)
    }

    /**
     * Detect whether the input is an InChI identifier.
     *
     * InChI strings start with "InChI=" followed by a version number and layer data.
     *
     * @param input Input string to test
     * @return `true` if input is an InChI identifier
     */
    fun isInchi(input: String): Boolean = input.startsWith("InChI=", ignoreCase = true)

    /**
     * Detect whether the input looks like a chemical name (IUPAC or common).
     *
     * Names are typically alphabetic words, possibly with hyphens, numbers,
     * and parentheses. They don't match formula or SMILES patterns.
     *
     * @param input Input string to test
     * @return `true` if input appears to be a chemical name
     */
    fun isLikelyChemicalName(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        if (isFormula(trimmed) || isSmiles(trimmed) || isInchi(trimmed)) return false
        // Names are typically alphabetic with optional hyphens and spaces
        return NAME_PATTERN.matches(trimmed) && trimmed.length >= 2
    }

    private fun subscriptDigit(ch: Char): Char? = when (ch) {
        '\u2080' -> '0'
        '\u2081' -> '1'
        '\u2082' -> '2'
        '\u2083' -> '3'
        '\u2084' -> '4'
        '\u2085' -> '5'
        '\u2086' -> '6'
        '\u2087' -> '7'
        '\u2088' -> '8'
        '\u2089' -> '9'
        else -> null
    }
}
