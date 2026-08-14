package com.istidblip.pengehubben

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.istidblip.pengehubben.networking.FinnhubStockRepository
import com.istidblip.pengehubben.networking.StockRepository
import com.istidblip.pengehubben.networking.SupabaseRepository
import com.istidblip.pengehubben.networking.createHttpClient
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.ktor.util.date.GMTDate

class DashboardViewModel(
    private val supabaseRepo: SupabaseRepository = SupabaseRepository(),
    private val stockRepo: StockRepository = FinnhubStockRepository(
        client = createHttpClient(),
        apiKey = BuildConfig.FINNHUB_API_KEY
    )
) : ViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        println("DASHBOARD_VM_ERROR: Uncaught exception in ViewModel: ${throwable.message}")
        throwable.printStackTrace()
    }

    private val _modules = MutableStateFlow<List<DashboardModule>>(emptyList())
    val modules: StateFlow<List<DashboardModule>> = _modules.asStateFlow()

    private val _searchResults = MutableStateFlow<List<StockPrice>>(emptyList())
    val searchResults: StateFlow<List<StockPrice>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("USD")
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _usdToNokRate = MutableStateFlow(10.5) // Fallback rate
    val usdToNokRate: StateFlow<Double> = _usdToNokRate.asStateFlow()

    private val _selectedStock = MutableStateFlow<StockPrice?>(null)
    val selectedStock: StateFlow<StockPrice?> = _selectedStock.asStateFlow()

    private val _stockCandles = MutableStateFlow<List<com.istidblip.pengehubben.networking.StockCandle>>(emptyList())
    val stockCandles: StateFlow<List<com.istidblip.pengehubben.networking.StockCandle>> = _stockCandles.asStateFlow()

    private val observationJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch(exceptionHandler) {
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
                            observationJobs[symbol] = viewModelScope.launch(exceptionHandler) {
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
                                stockPrice = StockPrice(symbol, null, 0.0, 0.0, 0)
                            ))
                        }
                    }
                    _modules.value = newModules
                    
                    // Lagre kun hvis vi faktisk har moduler og brukeren er logget inn (ikke bare fallback data)
                    if (symbols.isNotEmpty()) {
                        supabaseRepo.saveDashboardConfig(_modules.value)
                    }
                }
            } catch (e: Exception) {
                println("Critical error in DashboardViewModel init: ${e.message}")
            }
        }
    }

    private fun updateStockPriceInModules(symbol: String, networkPrice: com.istidblip.pengehubben.networking.StockPrice) {
        val currentModules = _modules.value.toMutableList()
        val index = currentModules.indexOfFirst { it is DashboardModule.Stock && it.stockPrice.symbol == symbol }
        
        if (index != -1) {
            val existingStock = (currentModules[index] as DashboardModule.Stock).stockPrice
            val updatedPrice = StockPrice(
                symbol = symbol,
                name = existingStock.name,
                price = networkPrice.currentPrice,
                change = networkPrice.percentChange,
                timestamp = networkPrice.timestamp
            )
            currentModules[index] = DashboardModule.Stock(
                id = (currentModules[index] as DashboardModule.Stock).id,
                stockPrice = updatedPrice
            )
            
            // Sikrer at oppdateringen av StateFlow skjer på Main-tråden for iOS-stabilitet
            viewModelScope.launch(Dispatchers.Main) {
                _modules.value = currentModules
            }
        }
    }

    fun searchStocks(query: String) {
        viewModelScope.launch {
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isSearching.value = true
            val results = stockRepo.searchSymbols(query)
            _searchResults.value = results.map { result ->
                StockPrice(
                    symbol = result.symbol,
                    name = result.description,
                    price = 0.0,
                    change = 0.0,
                    timestamp = 0
                )
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
        if (stock != null) {
            viewModelScope.launch {
                try {
                    // Bruker Ktor sin GMTDate som en trygg fallback for å få tid
                    val nowSeconds = GMTDate().timestamp / 1000
                    val thirtyDaysAgo = nowSeconds - (30 * 24 * 60 * 60)
                    _stockCandles.value = stockRepo.getStockCandles(stock.symbol, thirtyDaysAgo, nowSeconds)
                } catch (e: Exception) {
                    println("Klarte ikke hente historikk: ${e.message}")
                }
            }
        } else {
            _stockCandles.value = emptyList()
        }
    }

    fun toggleCurrency() {
        _selectedCurrency.value = if (_selectedCurrency.value == "USD") "NOK" else "USD"
    }

    fun getConvertedPrice(usdPrice: Double): Double {
        return if (_selectedCurrency.value == "NOK") {
            usdPrice * _usdToNokRate.value
        } else {
            usdPrice
        }
    }
}
