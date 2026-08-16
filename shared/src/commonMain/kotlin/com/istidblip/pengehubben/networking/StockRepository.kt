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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

interface StockRepository {
    suspend fun getStockPrice(symbol: String): StockPrice
    fun observeStockPrice(symbol: String): Flow<StockPrice>
    suspend fun searchSymbols(query: String): List<SymbolLookupResult>
    suspend fun getStockCandles(symbol: String, from: Long, to: Long, resolution: String = "D"): List<StockCandle>
    suspend fun getCryptoSymbols(exchange: String): List<SymbolLookupResult>
}

@Serializable
data class StockCandleResponse(
    val c: List<Double> = emptyList(),
    val h: List<Double> = emptyList(),
    val l: List<Double> = emptyList(),
    val o: List<Double> = emptyList(),
    val s: String = "no_data",
    val t: List<Long> = emptyList(),
    val v: List<Long> = emptyList()
)

@Serializable
data class StockCandle(
    val close: Double,
    val high: Double,
    val low: Double,
    val open: Double,
    val timestamp: Long,
    val volume: Long
)

@Serializable
data class SymbolLookupResponse(
    val count: Int,
    val result: List<SymbolLookupResult>
)

@Serializable
data class SymbolLookupResult(
    val description: String,
    val displaySymbol: String,
    val symbol: String,
    val type: String = ""
)

class FinnhubStockRepository(
    private val client: HttpClient,
    private val apiKey: String = "DEMO_KEY"
) : StockRepository {

    override suspend fun getStockPrice(symbol: String): StockPrice {
        return try {
            val response = client.get("https://finnhub.io/api/v1/quote") {
                parameter("symbol", symbol)
                parameter("token", apiKey)
            }
            if (response.status.value in 200..299) {
                response.body<StockPrice>()
            } else {
                StockPrice() // Return empty object with defaults
            }
        } catch (e: Exception) {
            println("Error fetching stock price for $symbol: ${e.message}")
            StockPrice()
        }
    }

    override suspend fun searchSymbols(query: String): List<SymbolLookupResult> {
        return try {
            val url = "https://finnhub.io/api/v1/search"
            println("FINNHUB_SEARCH_CALL: url=$url?q=$query&token=$apiKey")
            val response: SymbolLookupResponse = client.get(url) {
                parameter("q", query)
                parameter("token", apiKey)
            }.body()
            println("FINNHUB_SEARCH_RAW: query=$query, count=${response.count}")
            response.result.forEach { 
                println("FINNHUB_RESULT: symbol=${it.symbol}, description=${it.description}, type=${it.type}")
            }
            response.result
        } catch (e: Exception) {
            println("Error searching symbols: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getStockCandles(symbol: String, from: Long, to: Long, resolution: String): List<StockCandle> {
        return try {
            val url = "https://finnhub.io/api/v1/stock/candle"
            println("FINNHUB_CANDLES_CALL: symbol=$symbol, res=$resolution, from=$from, to=$to")
            val response: StockCandleResponse = client.get(url) {
                parameter("symbol", symbol)
                parameter("resolution", resolution)
                parameter("from", from)
                parameter("to", to)
                parameter("token", apiKey)
            }.body()
            
            println("FINNHUB_CANDLES_RAW: symbol=$symbol, status=${response.s}, count=${response.t.size}")
            
            if (response.s == "ok") {
                response.t.indices.map { i ->
                    StockCandle(
                        close = response.c[i],
                        high = response.h[i],
                        low = response.l[i],
                        open = response.o[i],
                        timestamp = response.t[i],
                        volume = response.v[i]
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error fetching candles for $symbol: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCryptoSymbols(exchange: String): List<SymbolLookupResult> {
        return try {
            val url = "https://finnhub.io/api/v1/crypto/symbol"
            val responseText = client.get(url) {
                parameter("exchange", exchange)
                parameter("token", apiKey)
            }.body<String>()
            
            println("FINNHUB_CRYPTO_RAW: exchange=$exchange, length=${responseText.length}")
            
            val response: List<SymbolLookupResult> = Json { ignoreUnknownKeys = true }.decodeFromString(responseText)
            // Tving type til å være krypto siden dette endepunktet kun returnerer krypto
            response.map { it.copy(type = "CRYPTO") }
        } catch (e: Exception) {
            println("Error fetching crypto symbols for $exchange: ${e.message}")
            emptyList()
        }
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
                delay(30000)
                try {
                    emit(getStockPrice(symbol))
                } catch (pe: Exception) {
                    println("Polling error for $symbol: ${pe.message}")
                }
            }
        }
    }
}
