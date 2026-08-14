package com.istidblip.pengehubben.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.istidblip.pengehubben.StockPrice
import com.istidblip.pengehubben.formatCurrency
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun StockCard(
    stock: StockPrice,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isPositive = stock.change >= 0
    val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
    val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown

    var previousPrice by remember { mutableStateOf(stock.price) }
    val flashColor = remember { Animatable(Color.Transparent) }
    val priceChangeDirection = remember(stock.price) {
        when {
            stock.price > previousPrice -> 1
            stock.price < previousPrice -> -1
            else -> 0
        }
    }

    LaunchedEffect(stock.price) {
        if (stock.price != previousPrice && previousPrice != 0.0) {
            val color = if (stock.price > previousPrice) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
            flashColor.animateTo(color, animationSpec = tween(200))
            flashColor.animateTo(Color.Transparent, animationSpec = tween(400))
            previousPrice = stock.price
        } else if (previousPrice == 0.0) {
            previousPrice = stock.price
        }
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(modifier = Modifier.background(flashColor.value)) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = if (isPositive) "Up" else "Down",
                        tint = trendColor
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.price.formatCurrency(currencyCode),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    if (priceChangeDirection != 0) {
                        Icon(
                            imageVector = if (priceChangeDirection > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (priceChangeDirection > 0) Color.Green else Color.Red,
                            modifier = Modifier.size(20.dp).padding(start = 4.dp)
                        )
                    }
                }
                
                Text(
                    text = "${if (isPositive) "+" else ""}${stock.change}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = trendColor
                )
            }
        }
    }
}

@Preview
@Composable
fun StockCardPreview() {
    MaterialTheme {
        StockCard(
            stock = StockPrice("AAPL", "Apple Inc.", 150.0, 1.2, 0L)
        )
    }
}
