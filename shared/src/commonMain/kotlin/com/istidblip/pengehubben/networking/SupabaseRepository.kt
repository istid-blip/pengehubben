package com.istidblip.pengehubben.networking

import com.istidblip.pengehubben.DashboardModule
import com.istidblip.pengehubben.Supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class TrackedStockEntity(
    val symbol: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val user_id: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DashboardConfigEntity(
    val modules: List<DashboardModule>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val user_id: String? = null
)

class SupabaseRepository {
    
    suspend fun getTrackedStocks(): List<String> {
        return try {
            Supabase.client.from("tracked_stocks")
                .select(columns = Columns.list("symbol"))
                .decodeList<TrackedStockEntity>()
                .map { it.symbol }
        } catch (e: Exception) {
            println("Error fetching tracked stocks: ${e.message}")
            emptyList()
        }
    }

    fun getTrackedStocksFlow(): Flow<List<String>> = flow {
        try {
            // Initial fetch
            emit(getTrackedStocks())
            
            // Listen for changes
            val channel = Supabase.client.channel("tracked_stocks_changes")
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "tracked_stocks"
            }.map { 
                getTrackedStocks()
            }
            
            channel.subscribe()
            emitAll(changeFlow)
        } catch (e: Exception) {
            println("Error in tracked stocks flow: ${e.message}")
            emit(emptyList())
        }
    }

    suspend fun addTrackedStock(symbol: String) {
        try {
            Supabase.client.from("tracked_stocks").insert(TrackedStockEntity(symbol))
        } catch (e: Exception) {
            println("Error adding tracked stock: ${e.message}")
        }
    }

    suspend fun removeTrackedStock(symbol: String) {
        try {
            Supabase.client.from("tracked_stocks").delete {
                filter {
                    eq("symbol", symbol)
                }
            }
        } catch (e: Exception) {
            println("Error removing tracked stock: ${e.message}")
        }
    }

    suspend fun saveDashboardConfig(modules: List<DashboardModule>) {
        try {
            Supabase.client.from("dashboard_config").upsert(DashboardConfigEntity(modules))
        } catch (e: Exception) {
            println("Error saving dashboard config: ${e.message}")
        }
    }

    suspend fun getDashboardConfig(): List<DashboardModule>? {
        return try {
            Supabase.client.from("dashboard_config")
                .select()
                .decodeSingleOrNull<DashboardConfigEntity>()
                ?.modules
        } catch (e: Exception) {
            println("Error getting dashboard config: ${e.message}")
            null
        }
    }
}
