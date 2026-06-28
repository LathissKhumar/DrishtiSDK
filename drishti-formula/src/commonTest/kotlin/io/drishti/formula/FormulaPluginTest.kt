package io.drishti.formula

import io.drishti.core.*
import kotlin.test.*

class FormulaPluginTest {

    @Test
    fun nameIsFormula() {
        val plugin = FormulaPlugin()
        assertEquals("formula", plugin.name)
    }

    @Test
    fun contentTypeIsFormula() {
        val plugin = FormulaPlugin()
        assertEquals(ContentType.FORMULA, plugin.contentType)
    }

    @Test
    fun renderHapticWithFormulaContent() {
        val plugin = FormulaPlugin()
        val items = listOf(TestFixtures.formulaContent())
        val output = plugin.renderHaptic(items)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderHapticWithEmptyItems() {
        val plugin = FormulaPlugin()
        val output = plugin.renderHaptic(emptyList())
        assertNotNull(output)
        assertTrue(output.pulses.isEmpty())
    }

    @Test
    fun renderAudioWithFormulaContent() {
        val plugin = FormulaPlugin()
        val items = listOf(TestFixtures.formulaContent())
        val output = plugin.renderAudio(items)
        assertNotNull(output)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderVoiceWithFormulaContent() {
        val plugin = FormulaPlugin()
        val items = listOf(TestFixtures.formulaContent())
        val output = plugin.renderVoice(items)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
    }

    @Test
    fun renderExplorationHaptic() {
        val plugin = FormulaPlugin()
        val item = TestFixtures.formulaContent()
        val output = plugin.renderExplorationHaptic(item, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun renderExplorationAudio() {
        val plugin = FormulaPlugin()
        val item = TestFixtures.formulaContent()
        val output = plugin.renderExplorationAudio(item, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun renderExplorationVoice() {
        val plugin = FormulaPlugin()
        val item = TestFixtures.formulaContent()
        val output = plugin.renderExplorationVoice(item, ExplorationDirection.NEXT)
        assertNotNull(output)
    }

    @Test
    fun detectLatexReturnsParsedFormula() {
        val plugin = FormulaPlugin()
        val formula = plugin.detectLatex("\\frac{1}{2}")
        assertNotNull(formula)
        assertEquals(ContentType.FORMULA, formula.contentType)
        assertNotNull(formula.ast)
        assertNotNull(formula.speechText)
    }

    @Test
    fun renderHapticWithParsedFormula() {
        val plugin = FormulaPlugin()
        val formula = plugin.detectLatex("x + y")
        val output = plugin.renderHaptic(formula)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderAudioWithParsedFormula() {
        val plugin = FormulaPlugin()
        val formula = plugin.detectLatex("x + y")
        val output = plugin.renderAudio(formula)
        assertNotNull(output)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderVoiceWithParsedFormula() {
        val plugin = FormulaPlugin()
        val formula = plugin.detectLatex("\\frac{a}{b}")
        val output = plugin.renderVoice(formula)
        assertNotNull(output)
        assertTrue(output.speech.text.isNotEmpty())
        assertTrue(output.speech.text.contains("over"))
    }

    @Test
    fun detectUnicodeReturnsFormula() {
        val plugin = FormulaPlugin()
        val formula = plugin.detectUnicode("∫ x")
        assertNotNull(formula)
    }
}

class FormulaRendererTest {

    @Test
    fun renderHapticAlgebraic() {
        val renderer = FormulaRenderer()
        val formula = TestFixtures.formulaContent(formulaType = FormulaType.ALGEBRAIC)
        val output = renderer.renderHaptic(formula)
        assertNotNull(output)
        assertEquals(5, output.pulses.size)
    }

    @Test
    fun renderAudioAlgebraic() {
        val renderer = FormulaRenderer()
        val formula = TestFixtures.formulaContent(formulaType = FormulaType.ALGEBRAIC)
        val output = renderer.renderAudio(formula)
        assertNotNull(output)
        assertEquals(5, output.sources.size)
    }

    @Test
    fun renderVoiceAlgebraic() {
        val renderer = FormulaRenderer()
        val formula = TestFixtures.formulaContent(formulaType = FormulaType.ALGEBRAIC)
        val output = renderer.renderVoice(formula)
        assertNotNull(output)
        assertTrue(output.speech.text.contains("Algebraic"))
    }

    @Test
    fun renderHapticFromParsedFormula() {
        val renderer = FormulaRenderer()
        val formula = ParsedFormula.fromLatex("\\frac{1}{2}")
        val output = renderer.renderHaptic(formula)
        assertNotNull(output)
        assertTrue(output.pulses.isNotEmpty())
    }

    @Test
    fun renderAudioFromParsedFormula() {
        val renderer = FormulaRenderer()
        val formula = ParsedFormula.fromLatex("\\frac{1}{2}")
        val output = renderer.renderAudio(formula)
        assertNotNull(output)
        assertTrue(output.sources.isNotEmpty())
    }

    @Test
    fun renderVoiceFromParsedFormula() {
        val renderer = FormulaRenderer()
        val formula = ParsedFormula.fromLatex("\\frac{a}{b}")
        val output = renderer.renderVoice(formula)
        assertNotNull(output)
        assertTrue(output.speech.text.contains("over"))
    }
}

class LatexParserTest {

    @Test
    fun parseSimpleAddition() {
        val ast = LatexParser.parse("x + y")
        assertTrue(ast is FormulaNode.BinaryOp)
        val op = ast as FormulaNode.BinaryOp
        assertEquals("+", op.operator)
        assertTrue(op.left is FormulaNode.Variable)
        assertTrue(op.right is FormulaNode.Variable)
    }

    @Test
    fun parseFraction() {
        val ast = LatexParser.parse("\\frac{a}{b}")
        assertTrue(ast is FormulaNode.Fraction)
        val frac = ast as FormulaNode.Fraction
        assertTrue(frac.numerator is FormulaNode.Variable)
        assertTrue(frac.denominator is FormulaNode.Variable)
    }

    @Test
    fun parseSquareRoot() {
        val ast = LatexParser.parse("\\sqrt{x}")
        assertTrue(ast is FormulaNode.SquareRoot)
        val sqrt = ast as FormulaNode.SquareRoot
        assertTrue(sqrt.content is FormulaNode.Variable)
        assertNull(sqrt.index)
    }

    @Test
    fun parseSquareRootWithIndex() {
        val ast = LatexParser.parse("\\sqrt[3]{x}")
        assertTrue(ast is FormulaNode.SquareRoot)
        val sqrt = ast as FormulaNode.SquareRoot
        assertNotNull(sqrt.index)
        assertTrue(sqrt.index is FormulaNode.Number)
    }

    @Test
    fun parsePower() {
        val ast = LatexParser.parse("x^{2}")
        assertTrue(ast is FormulaNode.Power)
        val pow = ast as FormulaNode.Power
        assertTrue(pow.base is FormulaNode.Variable)
        assertTrue(pow.exponent is FormulaNode.Number)
    }

    @Test
    fun parseIntegral() {
        val ast = LatexParser.parse("\\int_{0}^{1} x")
        assertTrue(ast is FormulaNode.Integral)
        val integral = ast as FormulaNode.Integral
        assertNotNull(integral.lower)
        assertNotNull(integral.upper)
    }

    @Test
    fun parseSummation() {
        val ast = LatexParser.parse("\\sum_{i=1}^{n} i")
        assertTrue(ast is FormulaNode.Summation)
        val sum = ast as FormulaNode.Summation
        assertNotNull(sum.lower)
        assertNotNull(sum.upper)
    }

    @Test
    fun parseLimit() {
        val ast = LatexParser.parse("\\lim_{x \\to 0} x")
        assertTrue(ast is FormulaNode.Limit)
        val lim = ast as FormulaNode.Limit
        assertTrue(lim.variable is FormulaNode.Variable)
    }

    @Test
    fun parseGreekLetter() {
        val ast = LatexParser.parse("\\pi")
        assertTrue(ast is FormulaNode.NamedSymbol)
        val sym = ast as FormulaNode.NamedSymbol
        assertEquals("pi", sym.name)
        assertEquals(SymbolType.GREEK_LETTER, sym.symbolType)
    }

    @Test
    fun parseFunction() {
        val ast = LatexParser.parse("\\sin(x)")
        assertTrue(ast is FormulaNode.FunctionCall)
        val func = ast as FormulaNode.FunctionCall
        assertEquals("sin", func.name)
    }

    @Test
    fun parseNestedFraction() {
        val ast = LatexParser.parse("\\frac{\\frac{a}{b}}{c}")
        assertTrue(ast is FormulaNode.Fraction)
        val outer = ast as FormulaNode.Fraction
        assertTrue(outer.numerator is FormulaNode.Fraction)
    }

    @Test
    fun parseNumber() {
        val ast = LatexParser.parse("42")
        assertTrue(ast is FormulaNode.Number)
        assertEquals("42", (ast as FormulaNode.Number).value)
    }

    @Test
    fun parseDecimalNumber() {
        val ast = LatexParser.parse("3.14")
        assertTrue(ast is FormulaNode.Number)
        assertEquals("3.14", (ast as FormulaNode.Number).value)
    }

    @Test
    fun parseSubscript() {
        val ast = LatexParser.parse("x_{1}")
        assertTrue(ast is FormulaNode.Subscript)
    }

    @Test
    fun parseMultiplication() {
        val ast = LatexParser.parse("x \\cdot y")
        assertTrue(ast is FormulaNode.BinaryOp)
        val op = ast as FormulaNode.BinaryOp
        assertEquals("*", op.operator)
    }

    @Test
    fun parseTimesSymbol() {
        val ast = LatexParser.parse("x \\times y")
        assertTrue(ast is FormulaNode.BinaryOp)
        assertEquals("*", (ast as FormulaNode.BinaryOp).operator)
    }

    @Test
    fun parseDivSymbol() {
        val ast = LatexParser.parse("x \\div y")
        assertTrue(ast is FormulaNode.BinaryOp)
        assertEquals("/", (ast as FormulaNode.BinaryOp).operator)
    }

    @Test
    fun parseUnaryMinus() {
        val ast = LatexParser.parse("-x")
        assertTrue(ast is FormulaNode.UnaryMinus)
    }

    @Test
    fun parseComplexExpression() {
        val ast = LatexParser.parse("\\frac{-b \\pm \\sqrt{b^{2} - 4ac}}{2a}")
        assertTrue(ast is FormulaNode.Fraction)
        val depth = ast.depth()
        assertTrue(depth > 3)
    }

    @Test
    fun parseInequality() {
        val ast = LatexParser.parse("x \\leq y")
        assertTrue(ast is FormulaNode.BinaryOp)
        assertEquals("leq", (ast as FormulaNode.BinaryOp).operator)
    }

    // ── Edge-case & production-hardening tests ────────────────────────

    @Test
    fun parseEmptyInputThrowsFormulaParseException() {
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse("") }
        assertTrue(ex.message!!.contains("empty"))
    }

    @Test
    fun parseBlankInputThrowsFormulaParseException() {
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse("   ") }
        assertTrue(ex.message!!.contains("empty"))
    }

    @Test
    fun parseUnmatchedOpenBraceThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("{a + b") }
    }

    @Test
    fun parseUnmatchedCloseBraceThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("a + b}") }
    }

    @Test
    fun parseDeeplyNestedBracesThrows() {
        val open = "{".repeat(51)
        val close = "}".repeat(51)
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse("${open}x${close}") }
        assertTrue(ex.message!!.contains("brace nesting depth"))
    }

    @Test
    fun parseDeeplyNestedBracesWithinLimit() {
        val open = "{".repeat(50)
        val close = "}".repeat(50)
        val ast = LatexParser.parse("${open}x${close}")
        assertTrue(ast is FormulaNode.Variable)
    }

    @Test
    fun parseExcessiveUnaryMinusThrows() {
        val input = "-".repeat(51) + "x"
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse(input) }
        assertTrue(ex.message!!.contains("unary minus"))
    }

    @Test
    fun parseFiftyUnaryMinusSucceeds() {
        val input = "-".repeat(50) + "x"
        val ast = LatexParser.parse(input)
        assertTrue(ast is FormulaNode.UnaryMinus)
    }

    @Test
    fun parseAbsoluteValueMissingClosingThrows() {
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse("|x + 1") }
        assertTrue(ex.message!!.contains("closing '|'"))
    }

    @Test
    fun parseAbsoluteValueSimple() {
        val ast = LatexParser.parse("|x|")
        assertTrue(ast is FormulaNode.AbsoluteValue)
        val av = ast as FormulaNode.AbsoluteValue
        assertTrue(av.content is FormulaNode.Variable)
        assertEquals("x", (av.content as FormulaNode.Variable).name)
    }

    @Test
    fun parseAbsoluteValueComplex() {
        val ast = LatexParser.parse("|x + y|")
        assertTrue(ast is FormulaNode.AbsoluteValue)
        val av = ast as FormulaNode.AbsoluteValue
        assertTrue(av.content is FormulaNode.BinaryOp)
    }

    @Test
    fun parseLeftRightSimple() {
        val ast = LatexParser.parse("\\left(x\\right)")
        assertTrue(ast is FormulaNode.Group)
        val g = ast as FormulaNode.Group
        assertEquals(1, g.children.size)
        assertTrue(g.children[0] is FormulaNode.Variable)
    }

    @Test
    fun parseLeftRightNested() {
        val ast = LatexParser.parse("\\left(\\left[x\\right]\\right)")
        assertTrue(ast is FormulaNode.Group)
        val g = ast as FormulaNode.Group
        assertEquals(1, g.children.size)
        val inner = g.children[0]
        assertTrue(inner is FormulaNode.Group)
    }

    @Test
    fun parseLeftRightUnmatchedThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("\\left(x") }
    }

    @Test
    fun parseLeftDotRight() {
        val ast = LatexParser.parse("\\left.\\frac{x}{y}\\right)")
        assertTrue(ast is FormulaNode.Group)
        val g = ast as FormulaNode.Group
        assertEquals(1, g.children.size)
        assertTrue(g.children[0] is FormulaNode.Fraction)
    }

    @Test
    fun parseBeginEndMatrix() {
        val ast = LatexParser.parse("\\begin{matrix}a & b \\\\ c & d\\end{matrix}")
        assertTrue(ast is FormulaNode.Matrix)
        val m = ast as FormulaNode.Matrix
        assertEquals(2, m.rows)
        assertEquals(2, m.columns)
    }

    @Test
    fun parseBeginEndCases() {
        val ast = LatexParser.parse("\\begin{cases} x & \\text{if } x > 0 \\\\ -x & \\text{otherwise} \\end{cases}")
        assertTrue(ast is FormulaNode.Cases)
        val c = ast as FormulaNode.Cases
        assertEquals(2, c.branches.size)
    }

    @Test
    fun parseBeginEndMismatchedEnvThrows() {
        assertFailsWith<FormulaParseException> {
            LatexParser.parse("\\begin{matrix}a\\end{cases}")
        }
    }

    @Test
    fun parseBeginEndMissingEndThrows() {
        assertFailsWith<FormulaParseException> {
            LatexParser.parse("\\begin{matrix}a & b")
        }
    }

    @Test
    fun parseUnsupportedEnvironmentThrows() {
        assertFailsWith<FormulaParseException> {
            LatexParser.parse("\\begin{tabular}{c}a\\end{tabular}")
        }
    }

    @Test
    fun parseMissingClosingParenThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("(x + 1") }
    }

    @Test
    fun parseMissingClosingBracketThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("\\sqrt[3{x}") }
    }

    @Test
    fun parseEndWithoutBeginThrows() {
        assertFailsWith<FormulaParseException> { LatexParser.parse("\\end{matrix}") }
    }

    @Test
    fun parseMatrixSingleRow() {
        val ast = LatexParser.parse("\\begin{matrix}1 & 2 & 3\\end{matrix}")
        assertTrue(ast is FormulaNode.Matrix)
        val m = ast as FormulaNode.Matrix
        assertEquals(1, m.rows)
        assertEquals(3, m.columns)
    }

    @Test
    fun parseCasesSingleBranch() {
        val ast = LatexParser.parse("\\begin{cases} x \\end{cases}")
        assertTrue(ast is FormulaNode.Cases)
        val c = ast as FormulaNode.Cases
        assertEquals(1, c.branches.size)
    }

    @Test
    fun parseExpressionThrowsFormulaParseException() {
        val ex = assertFailsWith<FormulaParseException> { LatexParser.parse("x +") }
        assertNotNull(ex.message)
    }
}

class FormulaEvaluatorTest {

    @Test
    fun evaluateSimpleAddition() {
        val ast = LatexParser.parse("2 + 3")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun evaluateMultiplication() {
        val ast = LatexParser.parse("4 * 5")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(20.0, result, 0.001)
    }

    @Test
    fun evaluateFraction() {
        val ast = LatexParser.parse("\\frac{10}{2}")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun evaluatePower() {
        val ast = LatexParser.parse("2^{3}")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(8.0, result, 0.001)
    }

    @Test
    fun evaluateSqrt() {
        val ast = LatexParser.parse("\\sqrt{9}")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(3.0, result, 0.001)
    }

    @Test
    fun evaluatePi() {
        val ast = LatexParser.parse("\\pi")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(Math.PI, result, 0.001)
    }

    @Test
    fun evaluateWithVariables() {
        val ast = LatexParser.parse("x + 1")
        val result = FormulaEvaluator.evaluate(ast, mapOf("x" to 4.0))
        assertNotNull(result)
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun evaluateUnboundVariableReturnsNull() {
        val ast = LatexParser.parse("x + y")
        val result = FormulaEvaluator.evaluate(ast)
        assertNull(result)
    }

    @Test
    fun evaluateSin() {
        val ast = LatexParser.parse("\\sin(0)")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(0.0, result, 0.001)
    }

    @Test
    fun evaluateCos() {
        val ast = LatexParser.parse("\\cos(0)")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(1.0, result, 0.001)
    }

    @Test
    fun evaluateNestedExpression() {
        val ast = LatexParser.parse("\\frac{2 + 3}{1}")
        val result = FormulaEvaluator.evaluate(ast)
        assertNotNull(result)
        assertEquals(5.0, result, 0.001)
    }

    @Test
    fun collectVariablesFindsAll() {
        val ast = LatexParser.parse("x + y * z")
        val vars = FormulaEvaluator.collectVariables(ast)
        assertEquals(setOf("x", "y", "z"), vars)
    }

    @Test
    fun collectVariablesEmptyForNumbers() {
        val ast = LatexParser.parse("42 + 3.14")
        val vars = FormulaEvaluator.collectVariables(ast)
        assertTrue(vars.isEmpty())
    }
}

class SpeechRuleEngineTest {

    @Test
    fun speechFraction() {
        val speech = SpeechRuleEngine.speechFromLatex("\\frac{a}{b}")
        assertEquals("a over b", speech)
    }

    @Test
    fun speechSquareRoot() {
        val speech = SpeechRuleEngine.speechFromLatex("\\sqrt{x}")
        assertEquals("square root of x", speech)
    }

    @Test
    fun speechSquareRootWithIndex() {
        val speech = SpeechRuleEngine.speechFromLatex("\\sqrt[3]{x}")
        assertEquals("3 root of x", speech)
    }

    @Test
    fun speechIntegral() {
        val speech = SpeechRuleEngine.speechFromLatex("\\int_{a}^{b} x")
        assertEquals("integral from a to b of x", speech)
    }

    @Test
    fun speechSummation() {
        val speech = SpeechRuleEngine.speechFromLatex("\\sum_{i=1}^{n} i")
        assertEquals("sum from i equals 1 to n of i", speech)
    }

    @Test
    fun speechLimit() {
        val speech = SpeechRuleEngine.speechFromLatex("\\lim_{x \\to 0} x")
        assertEquals("limit as x approaches 0 of x", speech)
    }

    @Test
    fun speechPower() {
        val speech = SpeechRuleEngine.speechFromLatex("x^{2}")
        assertEquals("x squared", speech)
    }

    @Test
    fun speechPowerCubed() {
        val speech = SpeechRuleEngine.speechFromLatex("x^{3}")
        assertEquals("x cubed", speech)
    }

    @Test
    fun speechFunction() {
        val speech = SpeechRuleEngine.speechFromLatex("\\sin(x)")
        assertEquals("sine of x", speech)
    }

    @Test
    fun speechCosFunction() {
        val speech = SpeechRuleEngine.speechFromLatex("\\cos(\\pi)")
        assertEquals("cosine of pi", speech)
    }

    @Test
    fun speechUnaryMinus() {
        val speech = SpeechRuleEngine.speechFromLatex("-x")
        assertEquals("negative x", speech)
    }

    @Test
    fun speechBinaryOp() {
        val speech = SpeechRuleEngine.speechFromLatex("a + b")
        assertEquals("a plus b", speech)
    }

    @Test
    fun speechGreekLetter() {
        val speech = SpeechRuleEngine.speechFromLatex("\\alpha")
        assertEquals("alpha", speech)
    }

    @Test
    fun speechComplexFormula() {
        val speech = SpeechRuleEngine.speechFromLatex("\\frac{-b \\pm \\sqrt{b^{2} - 4ac}}{2a}")
        assertTrue(speech.contains("over"))
        assertTrue(speech.contains("square root"))
    }

    @Test
    fun speechAbsoluteValue() {
        val speech = SpeechRuleEngine.speechFromLatex("|x|")
        assertEquals("absolute value of x", speech)
    }

    @Test
    fun speechEmptyset() {
        assertEquals("empty set", SpeechRuleEngine.speechFromLatex("\\emptyset"))
    }

    @Test
    fun speechCup() {
        assertEquals("union", SpeechRuleEngine.speechFromLatex("\\cup"))
    }

    @Test
    fun speechCap() {
        assertEquals("intersection", SpeechRuleEngine.speechFromLatex("\\cap"))
    }

    @Test
    fun speechInStandalone() {
        assertEquals("in", SpeechRuleEngine.speechFromLatex("\\in"))
    }

    @Test
    fun speechNotinStandalone() {
        assertEquals("not in", SpeechRuleEngine.speechFromLatex("\\notin"))
    }

    @Test
    fun speechSubsetStandalone() {
        assertEquals("is a subset of", SpeechRuleEngine.speechFromLatex("\\subset"))
    }

    @Test
    fun speechSupsetStandalone() {
        assertEquals("is a superset of", SpeechRuleEngine.speechFromLatex("\\supset"))
    }

    @Test
    fun speechForall() {
        assertEquals("for all", SpeechRuleEngine.speechFromLatex("\\forall"))
    }

    @Test
    fun speechExists() {
        assertEquals("there exists", SpeechRuleEngine.speechFromLatex("\\exists"))
    }

    @Test
    fun speechNeg() {
        assertEquals("not", SpeechRuleEngine.speechFromLatex("\\neg"))
    }

    @Test
    fun speechLand() {
        assertEquals("and", SpeechRuleEngine.speechFromLatex("\\land"))
    }

    @Test
    fun speechLor() {
        assertEquals("or", SpeechRuleEngine.speechFromLatex("\\lor"))
    }

    @Test
    fun speechImplies() {
        assertEquals("implies", SpeechRuleEngine.speechFromLatex("\\implies"))
    }

    @Test
    fun speechIff() {
        assertEquals("if and only if", SpeechRuleEngine.speechFromLatex("\\iff"))
    }

    @Test
    fun speechRightarrow() {
        assertEquals("goes to", SpeechRuleEngine.speechFromLatex("\\rightarrow"))
    }

    @Test
    fun speechLeftarrow() {
        assertEquals("comes from", SpeechRuleEngine.speechFromLatex("\\leftarrow"))
    }

    @Test
    fun speechDoubleRightarrow() {
        assertEquals("implies", SpeechRuleEngine.speechFromLatex("\\Rightarrow"))
    }

    @Test
    fun speechDoubleLeftarrow() {
        assertEquals("is implied by", SpeechRuleEngine.speechFromLatex("\\Leftarrow"))
    }

    @Test
    fun speechLeftrightarrow() {
        assertEquals("if and only if", SpeechRuleEngine.speechFromLatex("\\Leftrightarrow"))
    }

    @Test
    fun speechMapsto() {
        assertEquals("maps to", SpeechRuleEngine.speechFromLatex("\\mapsto"))
    }

    @Test
    fun speechLfloor() {
        assertEquals("floor of", SpeechRuleEngine.speechFromLatex("\\lfloor"))
    }

    @Test
    fun speechRfloor() {
        assertEquals("end floor", SpeechRuleEngine.speechFromLatex("\\rfloor"))
    }

    @Test
    fun speechLceil() {
        assertEquals("ceiling of", SpeechRuleEngine.speechFromLatex("\\lceil"))
    }

    @Test
    fun speechRceil() {
        assertEquals("end ceiling", SpeechRuleEngine.speechFromLatex("\\rceil"))
    }

    @Test
    fun speechInfinity() {
        assertEquals("infinity", SpeechRuleEngine.speechFromLatex("\\infty"))
    }

    @Test
    fun speechPartial() {
        assertEquals("partial", SpeechRuleEngine.speechFromLatex("\\partial"))
    }

    @Test
    fun speechNabla() {
        assertEquals("nabla", SpeechRuleEngine.speechFromLatex("\\nabla"))
    }

    @Test
    fun speechGreekBeta() {
        assertEquals("beta", SpeechRuleEngine.speechFromLatex("\\beta"))
    }

    @Test
    fun speechGreekCapitalGamma() {
        assertEquals("capital gamma", SpeechRuleEngine.speechFromLatex("\\Gamma"))
    }

    @Test
    fun speechGreekOmega() {
        assertEquals("omega", SpeechRuleEngine.speechFromLatex("\\omega"))
    }

    @Test
    fun speechBinaryIn() {
        assertEquals("x in S", SpeechRuleEngine.speechFromLatex("x \\in S"))
    }

    @Test
    fun speechBinaryNotin() {
        assertEquals("x not in S", SpeechRuleEngine.speechFromLatex("x \\notin S"))
    }

    @Test
    fun speechBinarySubset() {
        assertEquals("A is a subset of B", SpeechRuleEngine.speechFromLatex("A \\subset B"))
    }

    @Test
    fun speechBinarySupset() {
        assertEquals("A is a superset of B", SpeechRuleEngine.speechFromLatex("A \\supset B"))
    }

    @Test
    fun speechBinaryLeq() {
        assertEquals("x less than or equal to y", SpeechRuleEngine.speechFromLatex("x \\leq y"))
    }

    @Test
    fun speechBinaryGeq() {
        assertEquals("x greater than or equal to y", SpeechRuleEngine.speechFromLatex("x \\geq y"))
    }

    @Test
    fun speechBinaryNeq() {
        assertEquals("x not equal to y", SpeechRuleEngine.speechFromLatex("x \\neq y"))
    }

    @Test
    fun speechBinaryRightarrow() {
        assertEquals("x goes to y", SpeechRuleEngine.speechFromLatex("x \\rightarrow y"))
    }

    @Test
    fun speechBinaryLeftarrow() {
        assertEquals("x comes from y", SpeechRuleEngine.speechFromLatex("x \\leftarrow y"))
    }

    @Test
    fun speechBinaryDoubleRightarrow() {
        assertEquals("P implies Q", SpeechRuleEngine.speechFromLatex("P \\Rightarrow Q"))
    }

    @Test
    fun speechBinaryDoubleLeftarrow() {
        assertEquals("Q is implied by P", SpeechRuleEngine.speechFromLatex("Q \\Leftarrow P"))
    }
}

class FormulaDetectorTest {

    @Test
    fun detectFromLatexReturnsParsedFormula() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("\\frac{1}{2}")
        assertNotNull(formula)
        assertTrue(formula.latex.contains("\\frac"))
        assertNotNull(formula.ast)
        assertNotNull(formula.speechText)
    }

    @Test
    fun detectFromLatexWithCalculusType() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("\\int_{0}^{1} x^{2}", FormulaType.CALCULUS)
        assertEquals(FormulaType.CALCULUS, formula.formulaType)
    }

    @Test
    fun detectFromLatexClassifiesTrigonometric() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("\\sin(x) + \\cos(y)")
        assertEquals(FormulaType.TRIGONOMETRIC, formula.formulaType)
    }

    @Test
    fun detectFromLatexClassifiesCalculus() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("\\int_{0}^{1} x^{2}")
        assertEquals(FormulaType.CALCULUS, formula.formulaType)
    }

    @Test
    fun detectFromUnicodeConvertsPi() {
        val detector = FormulaDetector()
        val formula = detector.detectFromUnicode("π")
        assertNotNull(formula)
    }

    @Test
    fun detectFromUnicodeReturnsNullForNoFormula() {
        val detector = FormulaDetector()
        val result = detector.detectFromUnicode("hello world")
        assertNull(result)
    }

    @Test
    fun detectFromOcrTextHandlesUnicode() {
        val detector = FormulaDetector()
        val formula = detector.detectFromOcrText("∫ x")
        assertNotNull(formula)
    }

    @Test
    fun unicodeToLatexConvertsSymbols() {
        val detector = FormulaDetector()
        assertEquals("\\times", detector.unicodeToLatex("×"))
        assertEquals("\\pi", detector.unicodeToLatex("π"))
        assertEquals("\\int", detector.unicodeToLatex("∫"))
        assertEquals("\\alpha", detector.unicodeToLatex("α"))
    }

    @Test
    fun detectFormulaTypeAlgebraic() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("x + y = z")
        assertEquals(FormulaType.ALGEBRAIC, formula.formulaType)
    }

    @Test
    fun detectFormulaTypeNotation() {
        val detector = FormulaDetector()
        val formula = detector.detectFromLatex("\\alpha")
        assertEquals(FormulaType.NOTATION, formula.formulaType)
    }
}

class ParsedFormulaTest {

    @Test
    fun fromLatexCreatesCompleteFormula() {
        val formula = ParsedFormula.fromLatex("\\frac{1}{2}")
        assertNotNull(formula.latex)
        assertNotNull(formula.ast)
        assertNotNull(formula.speechText)
        assertEquals(ContentType.FORMULA, formula.contentType)
    }

    @Test
    fun fromLatexEvaluatesExpression() {
        val formula = ParsedFormula.fromLatex("\\frac{6}{3}")
        assertNotNull(formula.evaluationResult)
        assertEquals(2.0, formula.evaluationResult!!, 0.001)
    }

    @Test
    fun fromLatexWithVariablesIsNotEvaluable() {
        val formula = ParsedFormula.fromLatex("\\frac{x}{y}")
        assertNull(formula.evaluationResult)
    }

    @Test
    fun fromFormulaContentCreatesParsedFormula() {
        val content = TestFixtures.formulaContent()
        val formula = ParsedFormula.fromFormulaContent(content)
        assertNotNull(formula)
        assertEquals(content.expression, formula.latex)
    }

    @Test
    fun isEvaluableTrueWhenEvaluated() {
        val formula = ParsedFormula.fromLatex("2 + 3")
        assertTrue(formula.isEvaluable)
    }

    @Test
    fun isEvaluableFalseWhenUnbound() {
        val formula = ParsedFormula.fromLatex("x + y")
        assertFalse(formula.isEvaluable)
    }

    @Test
    fun symbolsAreExtracted() {
        val formula = ParsedFormula.fromLatex("x + y")
        assertTrue(formula.symbols.isNotEmpty())
    }
}

class FormulaASTTest {

    @Test
    fun depthOfNumberIs1() {
        assertEquals(1, FormulaNode.Number("42").depth())
    }

    @Test
    fun depthOfBinaryOpIs2() {
        val ast = FormulaNode.BinaryOp("+", FormulaNode.Number("1"), FormulaNode.Number("2"))
        assertEquals(2, ast.depth())
    }

    @Test
    fun depthOfFraction() {
        val ast = FormulaNode.Fraction(
            FormulaNode.Number("1"),
            FormulaNode.Number("2")
        )
        assertEquals(2, ast.depth())
    }

    @Test
    fun leafCountOfNumber() {
        assertEquals(1, FormulaNode.Number("42").leafCount())
    }

    @Test
    fun leafCountOfBinaryOp() {
        val ast = FormulaNode.BinaryOp("+", FormulaNode.Number("1"), FormulaNode.Number("2"))
        assertEquals(2, ast.leafCount())
    }

    @Test
    fun leafCountOfNested() {
        val ast = FormulaNode.BinaryOp(
            "+",
            FormulaNode.BinaryOp("*", FormulaNode.Number("1"), FormulaNode.Number("2")),
            FormulaNode.Number("3")
        )
        assertEquals(3, ast.leafCount())
    }
}
