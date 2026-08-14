package com.istidblip.pengehubben

import kotlinx.serialization.Serializable

@Serializable
data class StockPrice(
    val symbol: String,
    val name: String? = null,
    val price: Double,
    val change: Double,
    val timestamp: Long
)

@Serializable
data class PortfolioItem(
    val symbol: String,
    val quantity: Double,
    val averagePrice: Double
)

fun Double.formatCurrency(currencyCode: String = "USD"): String {
    val symbol = when (currencyCode) {
        "NOK" -> "kr"
        else -> "$"
    }
    val absoluteValue = if (this < 0) -this else this
    val parts = absoluteValue.toString().split(".")
    val integerPart = parts[0]
    val decimalPart = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    
    val formattedInteger = integerPart.reversed().chunked(3).joinToString(",").reversed()
    val sign = if (this < 0) "-" else ""
    
    return if (currencyCode == "NOK") {
        "$sign$formattedInteger,$decimalPart $symbol"
    } else {
        "$sign$symbol$formattedInteger.$decimalPart"
    }
}

@Serializable
sealed class DashboardModule {
    abstract val id: String
    abstract val moduleType: String

    @Serializable
    data class Stock(
        override val id: String,
        override val moduleType: String = "stock",
        val stockPrice: StockPrice
    ) : DashboardModule()

    @Serializable
    data class Summary(
        override val id: String,
        override val moduleType: String = "summary",
        val totalValue: Double,
        val dailyChange: Double
    ) : DashboardModule()
}
