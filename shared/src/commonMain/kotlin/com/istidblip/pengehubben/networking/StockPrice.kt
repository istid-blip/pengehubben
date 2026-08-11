package com.istidblip.pengehubben.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockPrice(
    @SerialName("c") val currentPrice: Double,
    @SerialName("d") val change: Double,
    @SerialName("dp") val percentChange: Double,
    @SerialName("h") val highPrice: Double,
    @SerialName("l") val lowPrice: Double,
    @SerialName("o") val openPrice: Double,
    @SerialName("pc") val previousClose: Double,
    @SerialName("t") val timestamp: Long
)
