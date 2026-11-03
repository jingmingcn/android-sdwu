package edu.sdwu.teaching.modules.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionTest {

    private fun value(input: String) = (evaluate(input) as CalcResult.Value).text
    private fun error(input: String) = (evaluate(input) as CalcResult.Error).message

    @Test
    fun `operator precedence and brackets`() {
        assertEquals("14", value("2+3*4"))
        assertEquals("20", value("(2+3)*4"))
        assertEquals("6.5", value("2 + 3 × (4 − 1) ÷ 2"))
    }

    @Test
    fun `decimals, unary minus and remainder`() {
        assertEquals("0.3", value("0.1+0.2")) // BigDecimal: no 0.30000000000000004
        assertEquals("-5", value("-(2+3)"))
        assertEquals("1", value("10%3"))
        assertEquals("0.3333333333333333", value("1/3"))
    }

    @Test
    fun `errors are reported, not thrown`() {
        assertEquals("Cannot divide by zero", error("1/0"))
        assertEquals("Missing ')'", error("(1+2"))
        assertEquals("Incomplete expression", error("2+"))
        assertEquals("Unexpected ')'", error("2)"))
        assertEquals("Bad number '1.2.3'", error("1.2.3"))
    }
}
