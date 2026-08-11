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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSearch(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val results by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

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
                        placeholder = { Text("Search stocks...") },
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
            if (isSearching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (results.isEmpty() && query.isNotEmpty() && !isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for \"$query\"")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { stock ->
                        ListItem(
                            headlineContent = { Text(stock.symbol) },
                            supportingContent = { Text("$${stock.price}") },
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
