package com.alecdev.quickcalc.tile

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.ListenableFuture

private const val RESOURCES_VERSION = "0"

class MainTileService : TileService() {

    override fun onResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        return CallbackToFutureAdapter.getFuture { completer ->
            completer.set(
                ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
            )
            "resourcesRequest"
        }
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        val lastClickId = requestParams.state?.lastClickableId
        val expression = if (!lastClickId.isNullOrEmpty() && lastClickId.startsWith("click_")) {
            handleTileInput(this, lastClickId)
        } else {
            getSharedPreferences("tile_prefs", Context.MODE_PRIVATE).getString("expression", "") ?: ""
        }

        val deviceParams = requestParams.deviceParameters ?: androidx.wear.tiles.DeviceParametersBuilders.DeviceParameters.Builder()
            .setScreenWidthDp(192)
            .setScreenHeightDp(192)
            .build()

        val singleTileTimeline = androidx.wear.protolayout.TimelineBuilders.Timeline.Builder().addTimelineEntry(
            androidx.wear.protolayout.TimelineBuilders.TimelineEntry.Builder().setLayout(
                androidx.wear.protolayout.LayoutElementBuilders.Layout.Builder().setRoot(tileLayout(this, expression, deviceParams)).build()
            ).build()
        ).build()

        return CallbackToFutureAdapter.getFuture { completer ->
            completer.set(
                TileBuilders.Tile.Builder()
                    .setResourcesVersion(RESOURCES_VERSION)
                    .setTileTimeline(singleTileTimeline)
                    .build()
            )
            "tileRequest"
        }
    }
}

private fun handleTileInput(context: Context, clickableId: String): String {
    val prefs = context.getSharedPreferences("tile_prefs", Context.MODE_PRIVATE)
    var expression = prefs.getString("expression", "") ?: ""
    val input = clickableId.removePrefix("click_")

    when (input) {
        "C" -> {
            expression = ""
        }
        "⌫" -> {
            if (expression.isNotEmpty()) {
                expression = expression.dropLast(1)
            }
        }
        "＝" -> {
            if (expression.isNotEmpty()) {
                try {
                    val sanitized = expression.replace('÷', '/').replace('×', '*').replace('−', '-')
                    val result = net.objecthunter.exp4j.ExpressionBuilder(sanitized).build().evaluate()
                    val df = java.text.DecimalFormat("#.########")
                    expression = df.format(result)
                } catch (e: Exception) {
                    // do nothing on error
                }
            }
        }
        "+", "−", "×", "÷" -> {
            val isLastCharOp = expression.isNotEmpty() && expression.last() in listOf('+', '−', '×', '÷')
            if (expression.isNotEmpty() && !isLastCharOp) {
                expression += input
            } else if (expression.isEmpty() && input == "−") {
                expression += input
            }
        }
        else -> {
            if (expression == "Error") {
                expression = ""
            }
            expression += input
        }
    }

    prefs.edit().putString("expression", expression).apply()
    return expression
}

private fun tileLayout(
    context: Context,
    expression: String,
    deviceParameters: DeviceParameters
): LayoutElementBuilders.LayoutElement {
    val displayText = expression.ifEmpty { "0" }
    val textColor = if (expression.isEmpty()) 0xFF8E8E93.toInt() else 0xFFFFFFFF.toInt()

    val displayFont = LayoutElementBuilders.FontStyle.Builder()
        .setSize(DimensionBuilders.SpProp.Builder().setValue(24f).build())
        .setColor(ColorBuilders.ColorProp.Builder().setArgb(textColor).build())
        .setPreferredFontFamilies("google-sans-flex", "sans-serif-rounded")
        .build()

    val displayElement = LayoutElementBuilders.Text.Builder()
        .setText(displayText)
        .setFontStyle(displayFont)
        .build()

    val launchAppClickable = ModifiersBuilders.Clickable.Builder()
        .setId("launch_app")
        .setOnClick(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(context.packageName)
                        .setClassName("com.alecdev.quickcalc.presentation.MainActivity")
                        .build()
                )
                .build()
        )
        .build()

    val displayContainer = LayoutElementBuilders.Box.Builder()
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setClickable(launchAppClickable)
                .build()
        )
        .addContent(displayElement)
        .build()

    // match app: padding start/end=12dp, so grid width = screenWidth - 24
    val screenWidth = deviceParameters.screenWidthDp
    val horizontalPadding = 12f
    val gridWidth = screenWidth - (horizontalPadding * 2f)
    val buttonGap = 1f
    val totalGaps = buttonGap * 4f
    val buttonWidth = (gridWidth - totalGaps) / 5f
    // match app: aspectRatio(1.2) means height = width / 1.2
    val buttonHeight = buttonWidth / 1.2f

    val row1 = tileRow(context, listOf("7", "8", "9", "÷", "C"), buttonWidth, buttonHeight, buttonGap)
    val row2 = tileRow(context, listOf("4", "5", "6", "×", "⌫"), buttonWidth, buttonHeight, buttonGap)
    val row3 = tileRow(context, listOf("1", "2", "3", "−", "＝"), buttonWidth, buttonHeight, buttonGap)
    val row4 = tileRow(context, listOf("", "0", ".", "+", ""), buttonWidth, buttonHeight, buttonGap)

    val rowGap = 4f

    val gridColumn = LayoutElementBuilders.Column.Builder()
        .addContent(displayContainer)
        .addContent(layoutSpacer(4f))
        .addContent(row1)
        .addContent(layoutSpacer(rowGap))
        .addContent(row2)
        .addContent(layoutSpacer(rowGap))
        .addContent(row3)
        .addContent(layoutSpacer(rowGap))
        .addContent(row4)
        .build()

    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.ExpandedDimensionProp.Builder().build())
        .setHeight(DimensionBuilders.ExpandedDimensionProp.Builder().build())
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .addContent(gridColumn)
        .build()
}

private fun tileRow(
    context: Context,
    buttons: List<String>,
    buttonWidth: Float,
    buttonHeight: Float,
    gap: Float
): LayoutElementBuilders.LayoutElement {
    val rowBuilder = LayoutElementBuilders.Row.Builder()
    for ((index, button) in buttons.withIndex()) {
        if (index > 0) {
            rowBuilder.addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setWidth(DimensionBuilders.DpProp.Builder().setValue(gap).build())
                    .setHeight(DimensionBuilders.DpProp.Builder().setValue(0f).build())
                    .build()
            )
        }
        if (button.isEmpty()) {
            rowBuilder.addContent(
                LayoutElementBuilders.Spacer.Builder()
                    .setWidth(DimensionBuilders.DpProp.Builder().setValue(buttonWidth).build())
                    .setHeight(DimensionBuilders.DpProp.Builder().setValue(buttonHeight).build())
                    .build()
            )
        } else {
            val colors = getM3TileColorsForButton(context, button)
            rowBuilder.addContent(tileButton(context, button, colors.first, colors.second, buttonWidth, buttonHeight))
        }
    }
    return rowBuilder.build()
}

private fun getM3TileColorsForButton(context: Context, button: String): Pair<Int, Int> {
    val isApi31 = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
    val (backgroundColor, textColor) = if (isApi31) {
        try {
            when (button) {
                in listOf("+", "−", "×", "÷") -> Pair(
                    context.getColor(android.R.color.system_neutral2_700),
                    context.getColor(android.R.color.system_neutral2_100)
                )
                "C" -> Pair(
                    context.getColor(android.R.color.system_accent3_700),
                    context.getColor(android.R.color.system_accent3_100)
                )
                "＝", "⌫" -> Pair(
                    context.getColor(android.R.color.system_accent1_700),
                    context.getColor(android.R.color.system_accent1_100)
                )
                else -> Pair(
                    context.getColor(android.R.color.system_neutral1_800),
                    context.getColor(android.R.color.system_neutral1_100)
                )
            }
        } catch (e: Exception) {
            when (button) {
                in listOf("+", "−", "×", "÷") -> Pair(0xFF4A4458.toInt(), 0xFFE8DEF8.toInt())
                "C" -> Pair(0xFF633B48.toInt(), 0xFFFFD8E4.toInt())
                "＝", "⌫" -> Pair(0xFF4F378B.toInt(), 0xFFEADDFF.toInt())
                else -> Pair(0xFF2C2C2F.toInt(), 0xFFE6E1E5.toInt())
            }
        }
    } else {
        when (button) {
            in listOf("+", "−", "×", "÷") -> Pair(0xFF4A4458.toInt(), 0xFFE8DEF8.toInt())
            "C" -> Pair(0xFF633B48.toInt(), 0xFFFFD8E4.toInt())
            "＝", "⌫" -> Pair(0xFF4F378B.toInt(), 0xFFEADDFF.toInt())
            else -> Pair(0xFF2C2C2F.toInt(), 0xFFE6E1E5.toInt())
        }
    }
    return Pair(backgroundColor, textColor)
}

private fun tileButton(
    context: Context,
    text: String,
    backgroundColorArgb: Int,
    contentColorArgb: Int,
    width: Float,
    height: Float
): LayoutElementBuilders.LayoutElement {
    val clickable = ModifiersBuilders.Clickable.Builder()
        .setId("click_$text")
        .setOnClick(
            ActionBuilders.LoadAction.Builder().build()
        )
        .build()

    // match app font sizes
    val fontSizeSp = when (text) {
        in listOf("+", "−", "×", "÷", "＝", "C", "⌫") -> 20f
        else -> 18f
    }

    val fontStyle = LayoutElementBuilders.FontStyle.Builder()
        .setSize(DimensionBuilders.SpProp.Builder().setValue(fontSizeSp).build())
        .setColor(ColorBuilders.ColorProp.Builder().setArgb(contentColorArgb).build())
        .setPreferredFontFamilies("google-sans-flex", "sans-serif-rounded")
        .build()

    val textElement = LayoutElementBuilders.Text.Builder()
        .setText(text)
        .setFontStyle(fontStyle)
        .build()

    // pill shape: corner radius = height / 2
    val cornerRadius = height / 2f
    val corner = ModifiersBuilders.Corner.Builder()
        .setRadius(DimensionBuilders.DpProp.Builder().setValue(cornerRadius).build())
        .build()

    val background = ModifiersBuilders.Background.Builder()
        .setColor(ColorBuilders.ColorProp.Builder().setArgb(backgroundColorArgb).build())
        .setCorner(corner)
        .build()

    val modifiers = ModifiersBuilders.Modifiers.Builder()
        .setClickable(clickable)
        .setBackground(background)
        .build()

    return LayoutElementBuilders.Box.Builder()
        .setWidth(DimensionBuilders.DpProp.Builder().setValue(width).build())
        .setHeight(DimensionBuilders.DpProp.Builder().setValue(height).build())
        .setModifiers(modifiers)
        .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
        .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
        .addContent(textElement)
        .build()
}

private fun layoutSpacer(size: Float): LayoutElementBuilders.LayoutElement {
    val dpSize = DimensionBuilders.DpProp.Builder().setValue(size).build()
    return LayoutElementBuilders.Spacer.Builder()
        .setWidth(dpSize)
        .setHeight(dpSize)
        .build()
}