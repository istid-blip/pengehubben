package com.istidblip.pengehubben.networking

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

interface StockRepository {
    suspend fun getStockPrice(symbol: String): StockPrice
    fun observeStockPrice(symbol: String): Flow<StockPrice>
}

class FinnhubStockRepository(
    private val client: HttpClient,
    private val apiKey: String = "DEMO_KEY"
) : StockRepository {

    override suspend fun getStockPrice(symbol: String): StockPrice {
        return client.get("https://finnhub.io/api/v1/quote") {
            parameter("symbol", symbol)
            parameter("token", apiKey)
        }.body()
    }

    override fun observeStockPrice(symbol: String): Flow<StockPrice> = flow {
        try {
            // Initial price
            emit(getStockPrice(symbol))

            client.webSocket("wss://ws.finnhub.io?token=$apiKey") {
                send(Frame.Text("{\"type\":\"subscribe\",\"symbol\":\"$symbol\"}"))
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val response = frame.readText()
                        val json = Json.parseToJsonElement(response).jsonObject
                        if (json["type"]?.jsonPrimitive?.content == "trade") {
                            val data = json["data"]?.jsonArray
                            data?.forEach { trade ->
                                val t = trade.jsonObject
                                if (t["s"]?.jsonPrimitive?.content == symbol) {
                                    val price = t["p"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                                    val timestamp = t["t"]?.jsonPrimitive?.longOrNull ?: 0L
                                    // Note: WebSockets trade data doesn't include daily change.
                                    // For simplicity, we emit a partial StockPrice or fetch update.
                                    // Fallback: emit updated price and 0 change for now, or keep last change.
                                    emit(StockPrice(
                                        currentPrice = price,
                                        change = 0.0, // Should be calculated or fetched
                                        percentChange = 0.0,
                                        highPrice = 0.0,
                                        lowPrice = 0.0,
                                        openPrice = 0.0,
                                        previousClose = 0.0,
                                        timestamp = timestamp
                                    ))
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("WebSocket error for $symbol: ${e.message}. Falling back to polling.")
            while (true) {
                delay(10000)
                try {
                    emit(getStockPrice(symbol))
                } catch (pe: Exception) {
                    println("Polling error for $symbol: ${pe.message}")
                }
            }
        }
    }
}
