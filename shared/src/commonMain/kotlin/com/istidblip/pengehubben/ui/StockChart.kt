package com.istidblip.pengehubben.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.istidblip.pengehubben.networking.StockCandle

@Composable
fun StockChart(
    candles: List<StockCandle>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (candles.isEmpty()) return

    val minPrice = candles.minOf { it.close }
    val maxPrice = candles.maxOf { it.close }
    val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (candles.size - 1).coerceAtLeast(1)

        val path = Path()
        
        candles.forEachIndexed { index, candle ->
            val x = index * stepX
            val y = height - ((candle.close - minPrice) / priceRange * height).toFloat()
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}
