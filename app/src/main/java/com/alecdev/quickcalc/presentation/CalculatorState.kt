import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.DecimalFormat

class CalculatorState {
    private var expression by mutableStateOf("")
    var display by mutableStateOf("")

    private val df = DecimalFormat("#.########")

    fun onInput(input: String) {
        if (input == "." && (expression.lastOrNull()?.isDigit() != true || lastNumberContainsDecimal())) {
            return
        }

        expression += input
        updateDisplay()
    }

    fun onOperation(op: String) {
        val sanitizedOp = if (op == "−") "-" else op

        if (expression.isEmpty() && sanitizedOp == "-") {
            expression += sanitizedOp
            updateDisplay()
            return
        }

        if (sanitizedOp == "-" && isLastCharOperation() && expression.last() != '-') {
            expression += sanitizedOp
            updateDisplay()
            return
        }

        if (expression.isNotEmpty() && !isLastCharOperation()) {
            expression += sanitizedOp
            updateDisplay()
        }
    }

    fun onCalculate() {
        try {
            val result = evaluate(expression)
            display = df.format(result)
            expression = display
        } catch (e: Exception) {
            display = "Error"
            expression = ""
        }
    }

    fun onClear() {
        expression = ""
        updateDisplay()
    }

    fun onDelete() {
        if (expression.isNotEmpty()) {
            expression = expression.dropLast(1)
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        display = expression
    }

    private fun isLastCharOperation(): Boolean {
        return expression.isNotEmpty() && expression.last() in listOf('+', '−', '×', '÷')
    }

    private fun lastNumberContainsDecimal(): Boolean {
        val lastNumber = expression.split(Regex("[-+×÷]")).last()
        return "." in lastNumber
    }

    private fun evaluate(expression: String): Double {
        val sanitizedExpression = expression.replace('÷', '/').replace('×', '*')
        return try {
            val result = net.objecthunter.exp4j.ExpressionBuilder(sanitizedExpression).build().evaluate()
            if (result.isInfinite() || result.isNaN()) {
                throw ArithmeticException("Invalid calculation")
            }
            result
        } catch (e: Exception) {
            throw ArithmeticException("Invalid expression")
        }
    }
}