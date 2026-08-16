package com.istidblip.pengehubben.networking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockPrice(
    @SerialName("c") val currentPrice: Double = 0.0,
    @SerialName("d") val change: Double = 0.0,
    @SerialName("dp") val percentChange: Double = 0.0,
    @SerialName("h") val highPrice: Double = 0.0,
    @SerialName("l") val lowPrice: Double = 0.0,
    @SerialName("o") val openPrice: Double = 0.0,
    @SerialName("pc") val previousClose: Double = 0.0,
    @SerialName("t") val timestamp: Long = 0
)
