package com.alecdev.quickcalc.presentation

import CalculatorState
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.material.curvedText
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.HorizontalPageIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PageIndicatorState
import androidx.wear.compose.material.Text
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.focusProperties
import com.alecdev.quickcalc.presentation.theme.QuickCalcTheme
import com.alecdev.quickcalc.presentation.theme.RoundedFontFamily

class MainActivity : ComponentActivity() {
    private val calculatorState = CalculatorState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            val currentDisplay = calculatorState.display
            val context = LocalContext.current
            LaunchedEffect(currentDisplay) {
                val tilePrefs = context.getSharedPreferences("tile_prefs", android.content.Context.MODE_PRIVATE)
                val savedExpr = tilePrefs.getString("expression", "") ?: ""
                if (currentDisplay != savedExpr) {
                    tilePrefs.edit().putString("expression", currentDisplay).apply()
                    try {
                        androidx.wear.tiles.TileService.getUpdater(context)
                            .requestUpdate(com.alecdev.quickcalc.tile.MainTileService::class.java)
                    } catch (e: Exception) {
                        // ignore update failures
                    }
                }
            }
            CalculatorApp(calculatorState)
        }
    }

    override fun onResume() {
        super.onResume()
        val tilePrefs = getSharedPreferences("tile_prefs", android.content.Context.MODE_PRIVATE)
        val initialExpr = tilePrefs.getString("expression", "") ?: ""
        calculatorState.updateExpression(initialExpr)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        val initialInput = intent?.getStringExtra("input")
        if (!initialInput.isNullOrEmpty()) {
            when (initialInput) {
                "C" -> calculatorState.onClear()
                "⌫" -> calculatorState.onDelete()
                "＝" -> calculatorState.onCalculate()
                "+", "−", "×", "÷" -> calculatorState.onOperation(initialInput)
                else -> calculatorState.onInput(initialInput)
            }
        }
    }
}

@Composable
fun CalculatorApp(calculatorState: CalculatorState = remember { CalculatorState() }) {
    val scrollState = rememberScrollState()
    var keysVisible by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberScalingLazyListState()

    val transitionProgress by animateFloatAsState(
        targetValue = if (keysVisible) 0f else 1f,
        animationSpec = tween(durationMillis = 300)
    )

    val isScrollInProgress = lazyListState.isScrollInProgress
    LaunchedEffect(keysVisible, isScrollInProgress) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(key1 = calculatorState.display) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    QuickCalcTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    if (keysVisible) {
                        if (event.verticalScrollPixels > 0f) {
                            keysVisible = false
                            true
                        } else {
                            false
                        }
                    } else {
                        val isAtTop = calculatorState.history.isEmpty() ||
                                lazyListState.layoutInfo.visibleItemsInfo.firstOrNull()?.index == 0
                        if (event.verticalScrollPixels < 0f && isAtTop) {
                            keysVisible = true
                            true
                        } else {
                            coroutineScope.launch {
                                lazyListState.scrollBy(event.verticalScrollPixels)
                            }
                            true
                        }
                    }
                }
                .focusRequester(focusRequester)
                .focusable()
        ) {
            if (transitionProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = transitionProgress
                            translationY = (transitionProgress - 1f) * 150.dp.toPx()
                        }
                ) {
                    CurvedLayout(
                        modifier = Modifier.fillMaxSize(),
                        anchor = 270f,
                        anchorType = AnchorType.Center
                    ) {
                        curvedText(
                            text = "History",
                            style = CurvedTextStyle(
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontFamily = RoundedFontFamily
                            )
                        )
                    }

                    // touch fallback
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(50.dp)
                            .clickable { keysVisible = true }
                    )

                    if (calculatorState.history.isEmpty()) {
                        Text(
                            text = "No history yet",
                            style = TextStyle(
                                fontFamily = RoundedFontFamily,
                                fontSize = 14.sp,
                                color = Color.Gray
                            ),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        ScalingLazyColumn(
                            state = lazyListState,
                            contentPadding = PaddingValues(top = 48.dp, bottom = 80.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            autoCentering = null
                        ) {
                            items(calculatorState.history.reversed()) { item ->
                                val parts = item.split("|")
                                val expr = parts.getOrNull(0) ?: ""
                                val res = parts.getOrNull(1) ?: ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = expr,
                                        style = TextStyle(
                                            fontFamily = RoundedFontFamily,
                                            fontSize = 14.sp,
                                            color = Color(0xFF9E9E9E)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res,
                                        style = TextStyle(
                                            fontFamily = RoundedFontFamily,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                            if (calculatorState.history.isNotEmpty()) {
                                item {
                                    Button(
                                        onClick = {
                                            calculatorState.history.clear()
                                        },
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .fillMaxWidth(0.8f)
                                            .focusProperties { canFocus = false },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = Color(0xFF2C2C2F),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Text(
                                            text = "Clear History",
                                            style = TextStyle(
                                                fontFamily = RoundedFontFamily,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (transitionProgress < 1f) {
                val onButtonClick: (String) -> Unit = { input ->
                    when (input) {
                        "C" -> calculatorState.onClear()
                        "⌫" -> calculatorState.onDelete()
                        "＝" -> calculatorState.onCalculate()
                        "+", "−", "×", "÷" -> calculatorState.onOperation(input)
                        "1/x" -> calculatorState.onInput("1/")
                        "√" -> calculatorState.onInput("√(")
                        "^" -> calculatorState.onInput("^")
                        "x²" -> calculatorState.onInput("^2")
                        "x³" -> calculatorState.onInput("^3")
                        "π" -> calculatorState.onInput("π")
                        "e" -> calculatorState.onInput("e")
                        "(" -> calculatorState.onInput("(")
                        ")" -> calculatorState.onInput(")")
                        "←" -> {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    }
                    else -> calculatorState.onInput(input)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp, bottom = 0.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                            .graphicsLayer {
                                alpha = 1f - transitionProgress
                                translationY = -transitionProgress * 100.dp.toPx()
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                val fadeWidth = 16.dp.toPx()
                                val leftFadeWidth = if (scrollState.maxValue > 0) {
                                    minOf(scrollState.value.toFloat(), fadeWidth)
                                } else {
                                    0f
                                }
                                val rightFadeWidth = if (scrollState.maxValue > 0) {
                                    minOf((scrollState.maxValue - scrollState.value).toFloat(), fadeWidth)
                                } else {
                                    0f
                                }
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0f to Color.Transparent,
                                        (leftFadeWidth / size.width) to Color.Black,
                                        ((size.width - rightFadeWidth) / size.width) to Color.Black,
                                        1f to Color.Transparent
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .horizontalScroll(scrollState)
                        ) {
                            val displayText = calculatorState.display.ifEmpty { "0" }
                            val textColor = if (calculatorState.display.isEmpty()) Color.Gray else Color.White
                            Text(
                                text = displayText,
                                color = textColor,
                                style = MaterialTheme.typography.display3.copy(
                                    fontFamily = RoundedFontFamily
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = 1f - transitionProgress
                                translationY = transitionProgress * 200.dp.toPx()
                            }
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            CalculatorButtons(
                                page = page,
                                onButtonClick = onButtonClick
                            )
                        }

                        val pageIndicatorState = remember(pagerState.currentPage, pagerState.pageCount) {
                            object : PageIndicatorState {
                                override val pageCount: Int get() = pagerState.pageCount
                                override val pageOffset: Float get() = 0f
                                override val selectedPage: Int get() = pagerState.currentPage
                            }
                        }

                        HorizontalPageIndicator(
                            pageIndicatorState = pageIndicatorState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButtons(
    page: Int,
    onButtonClick: (String) -> Unit
) {
    val row1 = if (page == 0) listOf("7", "8", "9", "÷", "C") else listOf("1/x", "√", "^", "÷", "C")
    val row2 = if (page == 0) listOf("4", "5", "6", "×", "⌫") else listOf("x²", "x³", "π", "×", "⌫")
    val row3 = if (page == 0) listOf("1", "2", "3", "−", "＝") else listOf("(", ")", "e", "−", "＝")
    val row4 = if (page == 0) listOf("", "0", ".", "+", "") else listOf("", "←", ".", "+", "")

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        ButtonRow(row1, onButtonClick)
        ButtonRow(row2, onButtonClick)
        ButtonRow(row3, onButtonClick)
        ButtonRow(row4, onButtonClick)
    }
}

data class ButtonColorScheme(
    val surfaceVariant: Color,
    val onSurface: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val darkAccent: Color,
    val onDarkAccent: Color
)

@Composable
fun getSystemColorScheme(): ButtonColorScheme {
    val context = LocalContext.current
    val isApi31 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return remember(isApi31) {
        if (isApi31) {
            ButtonColorScheme(
                surfaceVariant = Color(context.getColor(android.R.color.system_neutral1_800)),
                onSurface = Color(context.getColor(android.R.color.system_neutral1_100)),
                secondaryContainer = Color(context.getColor(android.R.color.system_neutral2_700)),
                onSecondaryContainer = Color(context.getColor(android.R.color.system_neutral2_100)),
                tertiaryContainer = Color(context.getColor(android.R.color.system_accent3_700)),
                onTertiaryContainer = Color(context.getColor(android.R.color.system_accent3_100)),
                primaryContainer = Color(context.getColor(android.R.color.system_accent1_700)),
                onPrimaryContainer = Color(context.getColor(android.R.color.system_accent1_100)),
                darkAccent = Color(context.getColor(android.R.color.system_accent1_900)),
                onDarkAccent = Color(context.getColor(android.R.color.system_accent1_200))
            )
        } else {
            ButtonColorScheme(
                surfaceVariant = Color(0xFF2C2C2F),
                onSurface = Color(0xFFE6E1E5),
                secondaryContainer = Color(0xFF4A4458),
                onSecondaryContainer = Color(0xFFE8DEF8),
                tertiaryContainer = Color(0xFF633B48),
                onTertiaryContainer = Color(0xFFFFD8E4),
                primaryContainer = Color(0xFF4F378B),
                onPrimaryContainer = Color(0xFFEADDFF),
                darkAccent = Color(0xFF1E1233),
                onDarkAccent = Color(0xFFD0BCFF)
            )
        }
    }
}

@Composable
fun ButtonRow(
    buttons: List<String>,
    onButtonClick: (String) -> Unit
) {
    val colorScheme = getSystemColorScheme()

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp)
            .height(0.dp)
    ) {
        for (button in buttons) {
            val backgroundColor = when (button) {
                in listOf("+", "−", "×", "÷") -> colorScheme.secondaryContainer
                "C" -> colorScheme.tertiaryContainer
                "＝", "⌫" -> colorScheme.primaryContainer
                "←" -> colorScheme.darkAccent
                else -> colorScheme.surfaceVariant
            }

            val btnTextColor = when (button) {
                in listOf("+", "−", "×", "÷") -> colorScheme.onSecondaryContainer
                "C" -> colorScheme.onTertiaryContainer
                "＝", "⌫" -> colorScheme.onPrimaryContainer
                "←" -> colorScheme.onDarkAccent
                else -> colorScheme.onSurface
            }

            val alpha = if (button.isEmpty()) 0f else 1f

            Button(
                onClick = { if (alpha > 0) onButtonClick(button) },
                modifier = Modifier
                    .padding(0.5.dp)
                    .aspectRatio(1.2f)
                    .weight(1f)
                    .alpha(alpha),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = backgroundColor.copy(alpha = alpha),
                    contentColor = btnTextColor
                )
            ) {
                if (alpha > 0) {
                    if (button == "←") {
                        androidx.wear.compose.material.Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.alecdev.quickcalc.R.drawable.ic_arrow_back),
                            contentDescription = "Back",
                            tint = btnTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        val fontSize = when (button) {
                            "1/x" -> 13.sp
                            "x²", "x³" -> 16.sp
                            in listOf("+", "−", "×", "÷", "＝", "C", "⌫", "√", "^", "(", ")") -> 20.sp
                            else -> 18.sp
                        }
                        Text(
                            text = button,
                            style = MaterialTheme.typography.button.copy(
                                fontFamily = RoundedFontFamily,
                                fontSize = fontSize
                            ),
                            color = btnTextColor
                        )
                    }
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
