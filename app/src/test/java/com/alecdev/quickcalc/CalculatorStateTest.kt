package com.alecdev.quickcalc

import CalculatorState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorStateTest {

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
        val state = CalculatorState()
        // dot on empty expression is ignored
        state.onInput(".")
        assertEquals("", state.display)

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
    }

    @Test
    fun testBasicOperations() {
        val state = CalculatorState()
        state.onInput("6")
        state.onOperation("÷")
        state.onInput("2")
        assertEquals("6÷2", state.display)
        state.onCalculate()
        assertEquals("3", state.display)
    }

    @Test
    fun testNegativeInput() {
        val state = CalculatorState()
        // minus as first char represents negative number sign
        state.onOperation("−")
        assertEquals("-", state.display)
        state.onInput("5")
        assertEquals("-5", state.display)
        state.onCalculate()
        assertEquals("-5", state.display)
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
