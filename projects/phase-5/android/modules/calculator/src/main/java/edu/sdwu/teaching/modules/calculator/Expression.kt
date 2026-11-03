package edu.sdwu.teaching.modules.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Evaluates expressions such as "2 + 3 × (4 − 1) / 2". Pure Kotlin, no Android: fully unit tested.
 *
 * Grammar (a classic recursive-descent parser, one function per rule):
 *   expression = term   { ("+" | "-") term }
 *   term       = factor { ("*" | "/" | "%") factor }
 *   factor     = ["-"] ( number | "(" expression ")" )
 *
 * The keypad shows × ÷ − but the parser also accepts * / -.
 */
sealed interface CalcResult {
    data class Value(val number: BigDecimal) : CalcResult {
        /** "7", "0.5", "-12.25" (no trailing zeros, no scientific notation). */
        val text: String get() = number.stripTrailingZeros().toPlainString()
    }

    data class Error(val message: String) : CalcResult
}

fun evaluate(input: String): CalcResult = try {
    val parser = Parser(input.replace('×', '*').replace('÷', '/').replace('−', '-').filterNot { it.isWhitespace() })
    val value = parser.expression()
    if (!parser.atEnd()) throw CalcException("Unexpected '${parser.peek()}'")
    CalcResult.Value(value.round(MathContext(16, RoundingMode.HALF_UP)))
} catch (e: CalcException) {
    CalcResult.Error(e.message ?: "Invalid expression")
}

private class CalcException(message: String) : Exception(message)

private class Parser(private val text: String) {
    private var pos = 0

    fun atEnd() = pos >= text.length
    fun peek(): Char = text[pos]

    fun expression(): BigDecimal {
        var value = term()
        while (!atEnd() && peek() in "+-") {
            val op = text[pos++]
            val right = term()
            value = if (op == '+') value + right else value - right
        }
        return value
    }

    private fun term(): BigDecimal {
        var value = factor()
        while (!atEnd() && peek() in "*/%") {
            val op = text[pos++]
            val right = factor()
            value = when (op) {
                '*' -> value * right
                else -> {
                    if (right.signum() == 0) throw CalcException("Cannot divide by zero")
                    if (op == '/') value.divide(right, MathContext.DECIMAL64) else value.rem(right)
                }
            }
        }
        return value
    }

    private fun factor(): BigDecimal {
        if (atEnd()) throw CalcException("Incomplete expression")
        if (peek() == '-') {
            pos++
            return factor().negate()
        }
        if (peek() == '(') {
            pos++
            val value = expression()
            if (atEnd() || peek() != ')') throw CalcException("Missing ')'")
            pos++
            return value
        }
        val start = pos
        while (!atEnd() && (peek().isDigit() || peek() == '.')) pos++
        val number = text.substring(start, pos)
        if (number.isEmpty()) throw CalcException("Unexpected '${peek()}'")
        return number.toBigDecimalOrNull() ?: throw CalcException("Bad number '$number'")
    }
}
