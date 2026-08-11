package com.istidblip.pengehubben.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.istidblip.pengehubben.DashboardModule
import com.istidblip.pengehubben.DashboardViewModel
import com.istidblip.pengehubben.StockPrice
import com.istidblip.pengehubben.formatCurrency
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ModularDashboard(
    viewModel: DashboardViewModel,
    onNavigateToSearch: () -> Unit
) {
    val modules by viewModel.modules.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<StockPrice>()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            DashboardListPane(
                modules = modules,
                onStockClick = { stock ->
                    viewModel.selectStock(stock)
                    scope.launch {
                        navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, stock)
                    }
                },
                onNavigateToSearch = onNavigateToSearch
            )
        },
        detailPane = {
            StockDetailPane(
                stock = selectedStock,
                onBack = {
                    scope.launch {
                        navigator.navigateBack()
                    }
                }
            )
        }
    )
}

@Composable
fun DashboardListPane(
    modules: List<DashboardModule>,
    onStockClick: (StockPrice) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSearch,
                icon = { Icon(Icons.Default.Search, "Search") },
                text = { Text("Search Stocks") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "My Financial Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (modules.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No modules yet. Tap the button to add some!")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(modules) { module ->
                        when (module) {
                            is DashboardModule.Stock -> StockCard(
                                stock = module.stockPrice,
                                onClick = { onStockClick(module.stockPrice) }
                            )
                            is DashboardModule.Summary -> SummaryCard(module = module)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailPane(
    stock: StockPrice?,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stock?.symbol ?: "Stock Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (stock != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    text = stock.symbol,
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Current Price: ${stock.price.formatCurrency()}",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Change: ${if (stock.change >= 0) "+" else ""}${stock.change}%",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (stock.change >= 0) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFF44336)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Placeholder for chart
                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Performance Chart Placeholder")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Select a stock to see details")
            }
        }
    }
}

@Composable
fun SummaryCard(module: DashboardModule.Summary) {
    val isPositive = module.dailyChange >= 0
    val trendColor = if (isPositive) androidx.compose.ui.graphics.Color(0xFF4CAF50) else androidx.compose.ui.graphics.Color(0xFFF44336)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Total Portfolio Value",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = module.totalValue.formatCurrency(),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "${if (isPositive) "+" else ""}${module.dailyChange}% today",
                style = MaterialTheme.typography.bodyLarge,
                color = trendColor
            )
        }
    }
}
