package com.istidblip.pengehubben.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.istidblip.pengehubben.DashboardViewModel
import com.istidblip.pengehubben.InstrumentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSearch(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Stocks", "Forex", "Indices", "Crypto")
    
    val filteredResults = remember(results, selectedTab) {
        val type = when (selectedTab) {
            0 -> InstrumentType.STOCK
            1 -> InstrumentType.FOREX
            2 -> InstrumentType.INDEX
            3 -> InstrumentType.CRYPTO
            else -> InstrumentType.STOCK
        }
        results.filter { it.type == type }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = {
                            query = it
                            viewModel.searchStocks(it)
                        },
                        placeholder = { Text("Search ${tabs[selectedTab].lowercase()}...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = ""; viewModel.searchStocks("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (filteredResults.isEmpty() && query.isNotEmpty() && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No ${tabs[selectedTab].lowercase()} found for \"$query\"")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredResults) { stock ->
                        ListItem(
                            headlineContent = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(stock.symbol)
                                    if (stock.type != InstrumentType.STOCK) {
                                        Badge(
                                            modifier = Modifier.padding(start = 8.dp),
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ) {
                                            Text(stock.type.name, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            },
                            supportingContent = { Text(stock.name ?: "") },
                            trailingContent = {
                                IconButton(onClick = { viewModel.addStock(stock); onBack() }) {
                                    Icon(Icons.Default.Add, "Add")
                                }
                            },
                            modifier = Modifier.clickable { viewModel.addStock(stock); onBack() }
                        )
                    }
                }
            }
        }
    }
}
