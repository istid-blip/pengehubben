package com.istidblip.pengehubben

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.istidblip.pengehubben.networking.FinnhubStockRepository
import com.istidblip.pengehubben.networking.StockRepository
import com.istidblip.pengehubben.networking.SupabaseRepository
import com.istidblip.pengehubben.networking.createHttpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val supabaseRepo: SupabaseRepository = SupabaseRepository(),
    private val stockRepo: StockRepository = FinnhubStockRepository(
        client = createHttpClient(),
        apiKey = BuildConfig.FINNHUB_API_KEY
    )
) : ViewModel() {
    private val _modules = MutableStateFlow<List<DashboardModule>>(emptyList())
    val modules: StateFlow<List<DashboardModule>> = _modules.asStateFlow()

    private val _searchResults = MutableStateFlow<List<StockPrice>>(emptyList())
    val searchResults: StateFlow<List<StockPrice>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedStock = MutableStateFlow<StockPrice?>(null)
    val selectedStock: StateFlow<StockPrice?> = _selectedStock.asStateFlow()

    private val observationJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            try {
                println("Initializing DashboardViewModel...")
                // Load initial dashboard config
                val config = supabaseRepo.getDashboardConfig()
                if (config != null) {
                    println("Dashboard config loaded successfully.")
                    _modules.value = config
                } else {
                    println("No dashboard config found, using fallback.")
                    // Fallback to mock data if no config found
                    _modules.value = listOf(
                        DashboardModule.Summary("summary-1", totalValue = 15000.0, dailyChange = 1.5)
                    )
                }

                // Sync with tracked stocks from Supabase
                supabaseRepo.getTrackedStocksFlow().collectLatest { symbols ->
                    println("Received ${symbols.size} tracked stocks from Supabase.")
                    
                    // Stop observing stocks that are no longer tracked
                    val removedSymbols = observationJobs.keys - symbols.toSet()
                    removedSymbols.forEach { symbol ->
                        observationJobs[symbol]?.cancel()
                        observationJobs.remove(symbol)
                    }

                    // Start observing new symbols
                    symbols.forEach { symbol ->
                        if (!observationJobs.containsKey(symbol)) {
                            observationJobs[symbol] = viewModelScope.launch {
                                stockRepo.observeStockPrice(symbol).collectLatest { networkPrice ->
                                    updateStockPriceInModules(symbol, networkPrice)
                                }
                            }
                        }
                    }
                    
                    // Initial update of modules to include all tracked stocks (some might be loading)
                    val currentStockModules = _modules.value.filterIsInstance<DashboardModule.Stock>()
                    val newModules = _modules.value.filter { it !is DashboardModule.Stock }.toMutableList()
                    
                    symbols.forEach { symbol ->
                        val existing = currentStockModules.find { it.stockPrice.symbol == symbol }
                        if (existing != null) {
                            newModules.add(existing)
                        } else {
                            // Placeholder while loading
                            newModules.add(DashboardModule.Stock(
                                id = "stock-${symbol.lowercase()}",
                                stockPrice = StockPrice(symbol, 0.0, 0.0, 0)
                            ))
                        }
                    }
                    _modules.value = newModules
                    supabaseRepo.saveDashboardConfig(_modules.value)
                }
            } catch (e: Exception) {
                println("Critical error in DashboardViewModel init: ${e.message}")
            }
        }
    }

    private fun updateStockPriceInModules(symbol: String, networkPrice: com.istidblip.pengehubben.networking.StockPrice) {
        val updatedPrice = StockPrice(
            symbol = symbol,
            price = networkPrice.currentPrice,
            change = networkPrice.percentChange,
            timestamp = networkPrice.timestamp
        )

        val currentModules = _modules.value.toMutableList()
        val index = currentModules.indexOfFirst { it is DashboardModule.Stock && it.stockPrice.symbol == symbol }
        
        if (index != -1) {
            currentModules[index] = DashboardModule.Stock(
                id = (currentModules[index] as DashboardModule.Stock).id,
                stockPrice = updatedPrice
            )
            _modules.value = currentModules
        }
    }

    fun searchStocks(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isSearching.value = true
            // Mock search results for now, should use stockRepo in production
            val mockAllStocks = listOf(
                StockPrice("AAPL", 150.0, 1.2, 0),
                StockPrice("GOOGL", 2800.0, -0.5, 0),
                StockPrice("MSFT", 300.0, 0.8, 0),
                StockPrice("AMZN", 3300.0, 1.5, 0),
                StockPrice("TSLA", 700.0, -2.0, 0),
                StockPrice("META", 350.0, 0.3, 0),
                StockPrice("NFLX", 500.0, -1.0, 0)
            )
            _searchResults.value = mockAllStocks.filter { 
                it.symbol.contains(query, ignoreCase = true) 
            }
            _isSearching.value = false
        }
    }

    fun addStock(stock: StockPrice) {
        viewModelScope.launch {
            if (!_modules.value.filterIsInstance<DashboardModule.Stock>().any { it.stockPrice.symbol == stock.symbol }) {
                supabaseRepo.addTrackedStock(stock.symbol)
            }
        }
    }

    fun removeStock(symbol: String) {
        viewModelScope.launch {
            supabaseRepo.removeTrackedStock(symbol)
        }
        if (_selectedStock.value?.symbol == symbol) {
            _selectedStock.value = null
        }
    }

    fun selectStock(stock: StockPrice?) {
        _selectedStock.value = stock
    }
}
