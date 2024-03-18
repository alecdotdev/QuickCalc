package com.alecdev.quickcalc.presentation

import CalculatorState
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.alecdev.quickcalc.presentation.theme.QuickCalcTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorApp()
        }
    }
}

@Composable
fun CalculatorApp() {
    val calculatorState = remember { CalculatorState() }
    val scrollState = rememberScrollState()

    QuickCalcTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 14.dp, bottom = 0.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(top = 0.dp, bottom = 0.dp)
                ) {
                    Text(
                        text = calculatorState.display,
                        style = MaterialTheme.typography.display3
                    )
                }
                LaunchedEffect(key1 = calculatorState.display) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
                CalculatorButtons(onButtonClick = { input ->
                    when (input) {
                        "C" -> calculatorState.onClear()
                        "⌫" -> calculatorState.onDelete()
                        "＝" -> calculatorState.onCalculate()
                        "+", "−", "×", "÷" -> calculatorState.onOperation(input)
                        else -> calculatorState.onInput(input)
                    }
                })

            }
        }

}

@Composable
fun CalculatorButtons(onButtonClick: (String) -> Unit) {
    val row1 = listOf("7", "8", "9", "÷", "C")
    val row2 = listOf("4", "5", "6", "×", "⌫")
    val row3 = listOf("1", "2", "3", "−", "＝")
    val row4 = listOf("", "0", ".", "+", "")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp, bottom = 0.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ButtonRow(row1, onButtonClick)
        ButtonRow(row2, onButtonClick)
        ButtonRow(row3, onButtonClick)

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            ButtonRow(row4, onButtonClick, isBottomRow = true)
        }
    }
}

@Composable
fun ButtonRow(
    buttons: List<String>,
    onButtonClick: (String) -> Unit,
    isBottomRow: Boolean = false
) {
    val numberColor = MaterialTheme.colors.surface
    val operatorColor = MaterialTheme.colors.primary
    val equalsColor = MaterialTheme.colors.primaryVariant
    val clearColor = MaterialTheme.colors.error
    val textColor = MaterialTheme.colors.onSurface

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
            .height(0.dp)
    ) {
        for ((index, button) in buttons.withIndex()) {
            val backgroundColor = when (button) {
                in listOf("+", "−", "×", "÷") -> operatorColor
                "C" -> clearColor
                "＝" -> equalsColor
                "⌫" -> equalsColor
                else -> numberColor
            }

            val textColor =
                if (button in listOf("+", "−", "×", "÷", "＝", "C", "⌫")) textColor else Color.White

            val alpha = if (isBottomRow && (index == 0 || index == buttons.lastIndex)) 0f else 1f

            Button(
                onClick = { if (alpha > 0) onButtonClick(button) },
                modifier = Modifier
                    .padding(0.5.dp)
                    .aspectRatio(1.2f)
                    .weight(1f)
                    .alpha(alpha),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = backgroundColor.copy(alpha = alpha),
                    contentColor = textColor
                )
            ) {
                if (alpha > 0) {
                    Text(button, color = textColor)
                }
            }
        }
    }
}


@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    CalculatorApp()
}
