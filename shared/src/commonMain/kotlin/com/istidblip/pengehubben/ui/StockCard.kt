package com.istidblip.pengehubben.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.istidblip.pengehubben.InstrumentType
import com.istidblip.pengehubben.StockPrice
import com.istidblip.pengehubben.formatCompactCurrency
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

    var previousPrice by remember { mutableStateOf(stock.price) }
    val flashColor = remember { Animatable(Color.Transparent) }

    val displayName = remember(stock.symbol) {
        stock.symbol.split(":").last().removePrefix("^")
    }

    LaunchedEffect(stock.price) {
        if (stock.price != previousPrice && previousPrice != 0.0) {
            val color = if (stock.price > previousPrice) Color.Green.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f)
            flashColor.animateTo(color, animationSpec = tween(150))
            flashColor.animateTo(Color.Transparent, animationSpec = tween(300))
            previousPrice = stock.price
        } else if (previousPrice == 0.0) {
            previousPrice = stock.price
        }
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = MaterialTheme.shapes.medium,
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.background(flashColor.value)) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = stock.price.formatCompactCurrency(currencyCode),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace // Monospace for numbers
                        ),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (stock.symbol != displayName) {
                            Text(
                                text = stock.symbol,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (stock.type != InstrumentType.STOCK) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.extraSmall,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = stock.type.name,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "${if (isPositive) "+" else ""}${stock.change}%",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = trendColor
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun StockCardPreview() {
    MaterialTheme {
        Column(modifier = Modifier.width(160.dp)) {
            StockCard(stock = StockPrice("AAPL", "Apple Inc.", 150.25, 1.2, 0L))
            StockCard(stock = StockPrice("BINANCE:BTCUSDT", "Bitcoin", 65432.10, -2.4, 0L, InstrumentType.CRYPTO))
            StockCard(stock = StockPrice("OANDA:EUR_USD", "Euro/Dollar", 1.0854, 0.05, 0L, InstrumentType.FOREX))
        }
    }
}
