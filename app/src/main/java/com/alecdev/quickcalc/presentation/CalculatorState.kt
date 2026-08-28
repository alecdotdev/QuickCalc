package com.alecdev.quickcalc.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorState {
    var expression by mutableStateOf("")
        private set
    var display by mutableStateOf("")
    val history = mutableStateListOf<String>()

    companion object {
        private val decimalFormat = DecimalFormat("0.########", DecimalFormatSymbols.getInstance(Locale.US)).apply {
            isGroupingUsed = false
        }

        fun formatResult(value: Double): String {
            return decimalFormat.format(value)
        }

        fun sanitizeExpression(expression: String): String {
            var sanitized = expression
                .replace('÷', '/')
                .replace('×', '*')
                .replace('−', '-')
                .replace(',', '.')
                .replace("√", "sqrt")
                .replace("π", "pi")

            // handle implicit multiplication
            sanitized = sanitized.replace(Regex("(\\d|\\)|pi|e)(?=\\s*\\()"), "$1*")
            sanitized = sanitized.replace(Regex("(\\d|\\))(?=\\s*(sqrt|pi|e))"), "$1*")
            sanitized = sanitized.replace(Regex("(\\))(?=\\s*\\d)"), "$1*")
            sanitized = sanitized.replace(Regex("(pi|e)(?=\\s*\\d)"), "$1*")

            // auto-close unclosed parentheses
            val openCount = sanitized.count { it == '(' }
            val closeCount = sanitized.count { it == ')' }
            if (openCount > closeCount) {
                sanitized += ")".repeat(openCount - closeCount)
            }

            return sanitized
        }

        fun evaluate(expression: String): Double {
            if (expression.isEmpty()) {
                throw ArithmeticException("Empty expression")
            }
            val sanitized = sanitizeExpression(expression)
            return try {
                val result = net.objecthunter.exp4j.ExpressionBuilder(sanitized).build().evaluate()
                if (result.isInfinite() || result.isNaN()) {
                    throw ArithmeticException("Invalid calculation")
                }
                result
            } catch (e: Exception) {
                throw ArithmeticException("Invalid expression")
            }
        }
    }

    fun updateExpression(expr: String) {
        expression = expr
        updateDisplay()
    }

    fun onInput(input: String) {
        if (input == ".") {
            if (lastNumberContainsDecimal()) {
                return
            }
            if (expression.isEmpty() || !expression.last().isDigit()) {
                expression += "0."
                updateDisplay()
                return
            }
        }

        expression += input
        updateDisplay()
    }

    fun onReciprocal() {
        expression = if (expression.isEmpty()) {
            "1/"
        } else {
            "1/($expression)"
        }
        updateDisplay()
    }

    fun onOperation(op: String) {
        val sanitizedOp = if (op == "−") "-" else op

        if (expression.isEmpty()) {
            if (sanitizedOp == "-") {
                expression += sanitizedOp
                updateDisplay()
            }
            return
        }

        if (isLastCharOperation()) {
            if (sanitizedOp == "-" && expression.last() != '-') {
                expression += sanitizedOp
                updateDisplay()
                return
            }
            // replace last operator
            expression = expression.dropLast(1) + sanitizedOp
            updateDisplay()
            return
        }

        expression += sanitizedOp
        updateDisplay()
    }

    fun onCalculate() {
        try {
            val result = evaluate(expression)
            val output = formatResult(result)
            if (expression.isNotEmpty() && expression != output) {
                history.add("$expression|$output")
            }
            display = output
            expression = display
        } catch (e: Exception) {
            // do nothing on error
        }
    }

    fun onClear() {
        expression = ""
        updateDisplay()
    }

    fun onDelete() {
        if (expression.isNotEmpty()) {
            expression = if (expression.endsWith("√(")) {
                expression.dropLast(2)
            } else {
                expression.dropLast(1)
            }
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        display = expression
    }

    private fun isLastCharOperation(): Boolean {
        return expression.isNotEmpty() && expression.last() in listOf('+', '-', '−', '×', '÷', '^')
    }

    private fun lastNumberContainsDecimal(): Boolean {
        var i = expression.length - 1
        while (i >= 0 && (expression[i].isDigit() || expression[i] == '.')) {
            if (expression[i] == '.') {
                return true
            }
            i--
        }
        return false
    }
}