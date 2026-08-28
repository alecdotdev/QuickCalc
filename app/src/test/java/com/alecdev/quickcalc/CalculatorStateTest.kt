package com.alecdev.quickcalc

import com.alecdev.quickcalc.presentation.CalculatorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CalculatorStateTest {

    private val testLocales = listOf(
        Locale("pt", "BR"),
        Locale.GERMANY,
        Locale.FRANCE,
        Locale.ITALY,
        Locale("es", "ES"),
        Locale("ru", "RU"),
        Locale.US,
        Locale.UK,
        Locale.JAPAN,
        Locale.CHINA,
        Locale("ar", "SA")
    )

    private fun runWithLocales(block: (Locale) -> Unit) {
        val originalLocale = Locale.getDefault()
        try {
            for (locale in testLocales) {
                Locale.setDefault(locale)
                block(locale)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun testInitialState() {
        val state = CalculatorState()
        assertEquals("", state.display)
        assertTrue(state.history.isEmpty())
    }

    @Test
    fun testDigitInput() {
        val state = CalculatorState()
        state.onInput("7")
        assertEquals("7", state.display)
        state.onInput("5")
        assertEquals("75", state.display)
    }

    @Test
    fun testDecimalConstraint() {
        runWithLocales {
            val state = CalculatorState()
            // dot on empty expression inserts 0.
            state.onInput(".")
            assertEquals("0.", state.display)
            state.onClear()

            state.onInput("7")
            state.onInput(".")
            state.onInput("5")
            assertEquals("7.5", state.display)

            // second dot in the same number is ignored
            state.onInput(".")
            assertEquals("7.5", state.display)

            state.onOperation("+")
            state.onInput("2")
            state.onInput(".")
            state.onInput("3")
            assertEquals("7.5+2.3", state.display)
        }
    }

    @Test
    fun testExponentialAndDecimalExponentAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("2")
            state.onInput(".")
            state.onInput("1")
            state.onInput("^")
            state.onInput("2")
            // second dot in exponent must be allowed
            state.onInput(".")
            state.onInput("1")
            assertEquals("2.1^2.1", state.display)

            // third dot in exponent number is ignored
            state.onInput(".")
            assertEquals("2.1^2.1", state.display)

            state.onCalculate()
            val result = state.display.toDouble()
            assertTrue(result > 4.74 && result < 4.75)
            assertTrue(state.display.contains("."))
            assertTrue(!state.display.contains(","))
        }
    }

    @Test
    fun testSquareRootAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("√(")
            state.onInput("4")
            state.onInput(")")
            state.onCalculate()
            assertEquals("2", state.display)

            state.onClear()
            state.onInput("√(")
            state.onInput("2")
            // auto-close unclosed parenthesis
            state.onCalculate()
            val result = state.display.toDouble()
            assertTrue(result > 1.414 && result < 1.415)
            assertTrue(!state.display.contains(","))
        }
    }

    @Test
    fun testSquareRootImplicitMultiplication() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("3")
            state.onInput("√(")
            state.onInput("4")
            state.onInput(")")
            state.onCalculate()
            assertEquals("6", state.display)
        }
    }

    @Test
    fun testConstantsAndImplicitMultiplicationAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("2")
            state.onInput("π")
            state.onCalculate()
            val piResult = state.display.toDouble()
            assertTrue(piResult > 6.28 && piResult < 6.29)

            state.onClear()
            state.onInput("2")
            state.onInput("e")
            state.onCalculate()
            val eResult = state.display.toDouble()
            assertTrue(eResult > 5.43 && eResult < 5.44)
        }
    }

    @Test
    fun testParenthesesImplicitMultiplication() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("(")
            state.onInput("2")
            state.onOperation("+")
            state.onInput("3")
            state.onInput(")")
            state.onInput("(")
            state.onInput("4")
            state.onOperation("−")
            state.onInput("1")
            state.onInput(")")
            state.onCalculate()
            assertEquals("15", state.display)
        }
    }

    @Test
    fun testReciprocalAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("5")
            state.onReciprocal()
            assertEquals("1/(5)", state.display)
            state.onCalculate()
            assertEquals("0.2", state.display)

            state.onClear()
            state.onReciprocal()
            assertEquals("1/", state.display)
            state.onInput("4")
            state.onCalculate()
            assertEquals("0.25", state.display)
        }
    }

    @Test
    fun testChainedCalculationsAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("2")
            state.onInput(".")
            state.onInput("5")
            state.onOperation("+")
            state.onInput("2")
            state.onInput(".")
            state.onInput("5")
            state.onCalculate()
            assertEquals("5", state.display)

            // continue calculating on formatted result
            state.onOperation("+")
            state.onInput("1")
            state.onInput(".")
            state.onInput("5")
            state.onCalculate()
            assertEquals("6.5", state.display)

            state.onOperation("×")
            state.onInput("2")
            state.onCalculate()
            assertEquals("13", state.display)
        }
    }

    @Test
    fun testPowerOperationsAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            // x² (inserts ^2)
            state.onInput("5")
            state.onInput("^2")
            state.onCalculate()
            assertEquals("25", state.display)

            // x³ (inserts ^3)
            state.onClear()
            state.onInput("2")
            state.onInput("^3")
            state.onCalculate()
            assertEquals("8", state.display)

            // fractional exponent (4^0.5)
            state.onClear()
            state.onInput("4")
            state.onInput("^")
            state.onInput("0")
            state.onInput(".")
            state.onInput("5")
            state.onCalculate()
            assertEquals("2", state.display)
        }
    }

    @Test
    fun testClear() {
        val state = CalculatorState()
        state.onInput("7")
        state.onClear()
        assertEquals("", state.display)
    }

    @Test
    fun testDelete() {
        val state = CalculatorState()
        state.onInput("7")
        state.onInput("5")
        state.onDelete()
        assertEquals("7", state.display)
        state.onDelete()
        assertEquals("", state.display)

        state.onInput("√(")
        assertEquals("√(", state.display)
        state.onDelete()
        assertEquals("", state.display)
    }

    @Test
    fun testBasicOperationsAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            state.onInput("6")
            state.onOperation("÷")
            state.onInput("2")
            assertEquals("6÷2", state.display)
            state.onCalculate()
            assertEquals("3", state.display)

            state.onClear()
            state.onInput("1")
            state.onInput(".")
            state.onInput("2")
            state.onOperation("+")
            state.onInput("3")
            state.onInput(".")
            state.onInput("4")
            state.onCalculate()
            assertEquals("4.6", state.display)
        }
    }

    @Test
    fun testOperatorReplacement() {
        val state = CalculatorState()
        state.onInput("6")
        state.onOperation("+")
        state.onOperation("×")
        state.onInput("2")
        assertEquals("6×2", state.display)
        state.onCalculate()
        assertEquals("12", state.display)
    }

    @Test
    fun testNegativeInputAcrossLocales() {
        runWithLocales {
            val state = CalculatorState()
            // minus as first char represents negative number sign
            state.onOperation("−")
            assertEquals("-", state.display)
            state.onInput("5")
            assertEquals("-5", state.display)
            state.onCalculate()
            assertEquals("-5", state.display)

            state.onClear()
            state.onInput("1")
            state.onOperation("−")
            state.onInput("1")
            state.onInput(".")
            state.onInput("5")
            state.onCalculate()
            assertEquals("-0.5", state.display)
        }
    }

    @Test
    fun testInvalidExpressionIgnored() {
        val state = CalculatorState()
        state.onInput("5")
        state.onOperation("+")
        assertEquals("5+", state.display)
        // invalid expression onCalculate should do nothing (not change display to Error)
        state.onCalculate()
        assertEquals("5+", state.display)
    }

    @Test
    fun testHistoryTracking() {
        val state = CalculatorState()
        state.onInput("5")
        state.onOperation("+")
        state.onInput("3")
        state.onCalculate()
        assertEquals("8", state.display)
        assertEquals(1, state.history.size)
        assertEquals("5+3|8", state.history[0])
    }
}
