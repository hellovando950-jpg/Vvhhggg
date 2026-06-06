package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    TradingDashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

// Popular Deriv Symbols Predefined List
val PopularSymbols = listOf(
    // Synthetic Indices
    SymbolInfo("Volatility 10 Index", "R_10", "Synthetic Indices"),
    SymbolInfo("Volatility 25 Index", "R_25", "Synthetic Indices"),
    SymbolInfo("Volatility 50 Index", "R_50", "Synthetic Indices"),
    SymbolInfo("Volatility 75 Index", "R_75", "Synthetic Indices"),
    SymbolInfo("Volatility 100 Index", "R_100", "Synthetic Indices"),
    SymbolInfo("Volatility 10 (1s) Index", "1HZ10V", "Synthetic Indices"),
    SymbolInfo("Volatility 100 (1s) Index", "1HZ100V", "Synthetic Indices"),
    SymbolInfo("Bear Market Index", "RDBEAR", "Synthetic Indices"),
    SymbolInfo("Bull Market Index", "RDBULL", "Synthetic Indices"),
    SymbolInfo("Crash 300 Index", "C_300", "Crash/Boom Indices"),
    SymbolInfo("Crash 500 Index", "CRASH500", "Crash/Boom Indices"),
    SymbolInfo("Crash 1000 Index", "CRASH1000", "Crash/Boom Indices"),
    SymbolInfo("Boom 300 Index", "B_300", "Crash/Boom Indices"),
    SymbolInfo("Boom 500 Index", "BOOM500", "Crash/Boom Indices"),
    SymbolInfo("Boom 1000 Index", "BOOM1000", "Crash/Boom Indices"),
    
    // Forex Major Pairs
    SymbolInfo("EUR / USD", "frxEURUSD", "Forex Major Pairs"),
    SymbolInfo("GBP / USD", "frxGBPUSD", "Forex Major Pairs"),
    SymbolInfo("USD / JPY", "frxUSDJPY", "Forex Major Pairs"),
    SymbolInfo("AUD / USD", "frxAUDUSD", "Forex Major Pairs"),
    SymbolInfo("EUR / GBP", "frxEURGBP", "Forex Major Pairs"),
    
    // Commodities
    SymbolInfo("Gold / USD", "frxXAUUSD", "Precious Commodities"),
    SymbolInfo("Silver / USD", "frxXAGUSD", "Precious Commodities"),
    
    // Cryptocurrencies
    SymbolInfo("BTC / USD", "cryBTCUSD", "Cryptocurrencies"),
    SymbolInfo("ETH / USD", "cryETHUSD", "Cryptocurrencies")
)

data class SymbolInfo(
    val name: String,
    val code: String,
    val category: String
)

@Composable
fun TradingDashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: TradingViewModel = viewModel()
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()
    val accountBalance by viewModel.accountBalance.collectAsStateWithLifecycle()
    val accountCurrency by viewModel.accountCurrency.collectAsStateWithLifecycle()
    val accountFullName by viewModel.accountFullName.collectAsStateWithLifecycle()
    val currentQuote by viewModel.currentQuote.collectAsStateWithLifecycle()
    val selectedSymbol by viewModel.selectedSymbol.collectAsStateWithLifecycle()
    val tickHistory by viewModel.tickHistory.collectAsStateWithLifecycle()
    val quoteChange by viewModel.quoteChange.collectAsStateWithLifecycle()
    val currentProposal by viewModel.currentProposal.collectAsStateWithLifecycle()
    val isRequestingProposal by viewModel.isRequestingProposal.collectAsStateWithLifecycle()
    val proposalError by viewModel.proposalError.collectAsStateWithLifecycle()
    val isBuying by viewModel.isBuying.collectAsStateWithLifecycle()
    val buyError by viewModel.buyError.collectAsStateWithLifecycle()
    val activeContract by viewModel.activeContract.collectAsStateWithLifecycle()
    val tradeHistory by viewModel.tradeHistory.collectAsStateWithLifecycle()

    val apiTokenFlow by viewModel.apiToken.collectAsStateWithLifecycle()
    val isDemo by viewModel.isDemo.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.feedbackMessage.collect { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            HeaderBar(
                connectionState = connectionState,
                isAuthorized = isAuthorized,
                balance = accountBalance,
                currency = accountCurrency,
                fullName = accountFullName,
                isDemo = isDemo,
                onAccountTypeToggle = { viewModel.toggleAccountType() },
                onSettingsClick = { showSettings = !showSettings }
            )

            // Settings/Auth Expandable Panel
            AnimatedVisibility(
                visible = showSettings || !isAuthorized,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsPanel(
                    savedToken = apiTokenFlow,
                    isAuthorized = isAuthorized,
                    isDemo = isDemo,
                    onSaveSettings = { token ->
                        viewModel.updateSettings(token)
                        showSettings = false
                    }
                )
            }

            // Market Selector Section
            SectionHeader(title = "Select Trading Asset", icon = Icons.Default.Analytics)

            var isDropdownExpanded by remember { mutableStateOf(false) }
            val activeSymbolObj = PopularSymbols.firstOrNull { it.code == selectedSymbol } ?: PopularSymbols.first()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DerivPrimary.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = DerivSurfaceLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ActionBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = activeSymbolObj.category.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ActionBlue
                                )
                            }
                            Column {
                                Text(
                                    text = activeSymbolObj.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Code: ${activeSymbolObj.code}",
                                    fontSize = 11.sp,
                                    color = White60
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (currentQuote != null) {
                                val trendColor = if (quoteChange > 0) BullishGreen else if (quoteChange < 0) BearishRed else White90
                                Text(
                                    text = String.format(Locale.US, "%,.2f", currentQuote),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = trendColor
                                )
                            }
                            Icon(
                                imageVector = if (isDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = White90
                            )
                        }
                    }
                }

                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false },
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .background(DerivSurface)
                        .border(1.dp, DerivSurfaceLight, RoundedCornerShape(8.dp))
                ) {
                    PopularSymbols.groupBy { it.category }.forEach { (category, symbols) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = category.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = HighlightGold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            },
                            enabled = false,
                            onClick = {}
                        )

                        symbols.forEach { symbol ->
                            val isSymbolSelected = symbol.code == selectedSymbol
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = symbol.name,
                                            color = if (isSymbolSelected) DerivPrimaryLight else Color.White,
                                            fontWeight = if (isSymbolSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = symbol.code,
                                            color = White60,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectSymbol(symbol.code)
                                    isDropdownExpanded = false
                                }
                            )
                        }

                        HorizontalDivider(color = White60.copy(alpha = 0.05f))
                    }
                }
            }

            // Visual Chart Section
            val symbolInfo = PopularSymbols.firstOrNull { it.code == selectedSymbol }
            MainChartSection(
                symbolName = symbolInfo?.name ?: selectedSymbol,
                symbolCode = selectedSymbol,
                history = tickHistory,
                currentQuote = currentQuote,
                quoteChange = quoteChange
            )

            // Active running Contract status tracking (Takes immediate priority if present)
            activeContract?.let { contract ->
                ActiveContractTrackingCard(
                    contract = contract,
                    symbolCode = selectedSymbol,
                    onCloseClick = { viewModel.forgetContractUpdates() }
                )
            }

            // Contract / Trade configuration pane
            TradeConfigurationPane(
                viewModel = viewModel,
                isAuthorized = isAuthorized,
                currentProposal = currentProposal,
                isRequestingProposal = isRequestingProposal,
                proposalError = proposalError,
                isBuying = isBuying,
                buyError = buyError,
                onPurchase = { viewModel.purchaseCurrentProposal() }
            )

            // Recent Resolved Trades Log
            RecentTradesSection(
                tradeHistory = tradeHistory,
                onClearHistory = { viewModel.clearTradeHistory() }
            )
        }
    }
}

@Composable
fun HeaderBar(
    connectionState: ConnectionState,
    isAuthorized: Boolean,
    balance: Double,
    currency: String,
    fullName: String,
    isDemo: Boolean,
    onAccountTypeToggle: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Title + Connection state tracker
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Deriv Trader",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = Color.White
                    )
                    ConnectionPulse(state = connectionState)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Quick Demo / Real environment switcher trigger
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isDemo) BullishGreen.copy(alpha = 0.15f) else HighlightGold.copy(alpha = 0.15f))
                            .border(1.dp, if (isDemo) BullishGreen else HighlightGold, RoundedCornerShape(6.dp))
                            .clickable { onAccountTypeToggle() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDemo) "DEMO" else "REAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDemo) BullishGreen else HighlightGold
                        )
                    }

                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "Real-time Stream Connected"
                            ConnectionState.CONNECTING -> "Handshaking..."
                            ConnectionState.DISCONNECTED -> "Offline Mode"
                            ConnectionState.ERROR -> "Interrupted"
                        },
                        fontSize = 11.sp,
                        color = White60
                    )
                }
            }

            // Right Account and Settings Panel Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAuthorized) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f %s", balance, currency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDemo) BullishGreen else HighlightGold
                        )
                        Text(
                            text = fullName.ifEmpty { if (isDemo) "Demo Virtual Account" else "Real Client" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = White60,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BearishRed.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "UNAUTHORIZED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BearishRed
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = DerivSurfaceLight
                    )
                ) {
                    Icon(
                        imageVector = if (isAuthorized) Icons.Default.Settings else Icons.Default.VpnKey,
                        contentDescription = "Settings",
                        tint = if (isAuthorized) White90 else HighlightGold
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionPulse(state: ConnectionState) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val color = when (state) {
        ConnectionState.CONNECTED -> BullishGreen
        ConnectionState.CONNECTING -> HighlightGold
        ConnectionState.DISCONNECTED -> NeutralGray
        ConnectionState.ERROR -> BearishRed
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

@Composable
fun SettingsPanel(
    savedToken: String,
    isAuthorized: Boolean,
    isDemo: Boolean,
    onSaveSettings: (String) -> Unit
) {
    var rawToken by remember(savedToken) { mutableStateOf(savedToken) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = DerivSurfaceLight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isAuthorized) Color.Transparent else HighlightGold.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = HighlightGold
                )
                Text(
                    text = "Deriv API ${if (isDemo) "Demo" else "Real"} Access Setup",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "Secure handshakes are encrypted. Enter your Deriv Personal Access Token (PAT) under the active ${if (isDemo) "DEMO" else "REAL"} environment below.",
                fontSize = 12.sp,
                color = White60
            )

            // Directions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "How to generate your token:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ActionBlue
                )
                Text(
                    text = "1. Log into your account on www.deriv.com\n" +
                            "2. Go to Account Settings > Security and Safety > API Token\n" +
                            "3. Name your token and select 'Read' and 'Trade' checkboxes\n" +
                            "4. Copy your token and paste below for quick live trading!",
                    fontSize = 11.sp,
                    color = White90,
                    lineHeight = 15.sp
                )
            }

            OutlinedTextField(
                value = rawToken,
                onValueChange = { rawToken = it },
                label = { Text("API Token (${if (isDemo) "Demo Token" else "Real Token"})", color = White60) },
                placeholder = { Text("Paste token e.g. a1b2c3d4...") },
                singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = ActionBlue,
                    unfocusedBorderColor = NeutralGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = { onSaveSettings(rawToken) },
                colors = ButtonDefaults.buttonColors(containerColor = ActionBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save and Authenticate ${if (isDemo) "Demo" else "Real"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = DerivPrimaryLight,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = White90,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SymbolCard(
    symbolName: String,
    symbolCode: String,
    category: String,
    currentQuote: Double?,
    quoteChange: Double,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(155.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isSelected) DerivPrimary else Color.Transparent
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DerivSurfaceLight else DerivSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) HighlightGold else ActionBlue
            )

            Text(
                text = symbolName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (currentQuote != null) {
                val trendColor = if (quoteChange > 0) BullishGreen else if (quoteChange < 0) BearishRed else White90
                val arrow = if (quoteChange > 0) "▲" else if (quoteChange < 0) "▼" else ""

                Text(
                    text = String.format(Locale.US, "%,.2f", currentQuote),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = trendColor
                )

                Text(
                    text = String.format(Locale.US, "%s %+.2f", arrow, quoteChange),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = trendColor
                )
            } else {
                Text(
                    text = if (isSelected) "Loading..." else "Click to View",
                    fontSize = 11.sp,
                    color = White60
                )
                Spacer(modifier = Modifier.height(13.dp))
            }
        }
    }
}

@Composable
fun MainChartSection(
    symbolName: String,
    symbolCode: String,
    history: List<Double>,
    currentQuote: Double?,
    quoteChange: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DerivSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Stats Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = symbolName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Real-time Tick Tick Tracker",
                        fontSize = 11.sp,
                        color = White60
                    )
                }

                if (currentQuote != null) {
                    val color = if (quoteChange > 0) BullishGreen else if (quoteChange < 0) BearishRed else Color.White
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = String.format(Locale.US, "%,.2f", currentQuote),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = color
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (quoteChange >= 0) "▲" else "▼",
                                fontSize = 10.sp,
                                color = color
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format(Locale.US, "%+.4f", quoteChange),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Glowing Line Chart Canvas
            if (history.isNotEmpty()) {
                val chartColor = if (quoteChange >= 0) BullishGreen else BearishRed
                TickChart(
                    prices = history,
                    chartColor = chartColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = DerivPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting first price tick from WebSocket...",
                            color = White60,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Disclaimer on Real Market Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = BullishGreen,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Direct feed app ID: ${symbolCode}",
                    fontSize = 9.sp,
                    color = White60
                )
            }
        }
    }
}

@Composable
fun TickChart(prices: List<Double>, chartColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (prices.size < 2) return@Canvas

        val minPrice = prices.minOrNull() ?: 0.0
        val maxPrice = prices.maxOrNull() ?: 1.0
        val priceRange = if (maxPrice - minPrice == 0.0) 1.0 else maxPrice - minPrice

        val path = Path()
        val spacing = size.width / (prices.size - 1)

        prices.forEachIndexed { index, price ->
            val x = index * spacing
            val y = size.height - (((price - minPrice) / priceRange) * size.height).toFloat()

            // Safe bound capping to avoid drawing outside the canvas
            val boundedY = y.coerceIn(4f, size.height - 4f)

            if (index == 0) {
                path.moveTo(x, boundedY)
            } else {
                path.lineTo(x, boundedY)
            }
        }

        // Draw Glowing lines
        drawPath(
            path = path,
            color = chartColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw Area fill beneath the path lines
        val areaPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = areaPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    chartColor.copy(alpha = 0.2f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height
            )
        )

        // Pulsing terminal node node on current spot
        val lastX = size.width
        val lastY = (size.height - (((prices.last() - minPrice) / priceRange) * size.height)).toFloat().coerceIn(4f, size.height - 4f)

        drawCircle(
            color = chartColor,
            radius = 5.dp.toPx(),
            center = Offset(lastX, lastY)
        )

        drawCircle(
            color = chartColor.copy(alpha = 0.35f),
            radius = 11.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}

@Composable
fun ActiveContractTrackingCard(
    contract: ActiveContractDetails,
    symbolCode: String,
    onCloseClick: () -> Unit
) {
    val trendingColor = if (contract.profit >= 0) BullishGreen else BearishRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = DerivSurfaceLight),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, trendingColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(trendingColor)
                    )
                    Text(
                        text = "ACTIVE CONTRACT: ID #${contract.contractId}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = White60,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Big Live Profit Output
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Profit/Loss",
                        fontSize = 11.sp,
                        color = White60
                    )
                    Text(
                        text = String.format(Locale.US, "%+,.2f USD", contract.profit),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = trendingColor
                    )
                }

                // Status Badge Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(trendingColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = contract.status.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = trendingColor
                    )
                }
            }

            HorizontalDivider(color = White60.copy(alpha = 0.1f))

            // Tick specifics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Entry Spot", fontSize = 11.sp, color = White60)
                    Text(
                        text = if (contract.entrySpot != null) String.format(Locale.US, "%,.2f", contract.entrySpot) else "Fetching...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Current Spot Price", fontSize = 11.sp, color = White60)
                    Text(
                        text = String.format(Locale.US, "%,.2f", contract.currentSpot),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Text(
                text = contract.longCode,
                fontSize = 11.sp,
                color = White90,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun TradeConfigurationPane(
    viewModel: TradingViewModel,
    isAuthorized: Boolean,
    currentProposal: ProposalDetails?,
    isRequestingProposal: Boolean,
    proposalError: String?,
    isBuying: Boolean,
    buyError: String?,
    onPurchase: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = DerivSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CONTRACT TYPE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = White60,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Contract Category Row
            val contractTypes = listOf(
                ContractTypeOption("Rise / Fall", "CALL", "Up/Down"),
                ContractTypeOption("Over / Under", "DIGITUNDER", "Digits"),
                ContractTypeOption("Matches / Differs", "DIGITMATCH", "Digits"),
                ContractTypeOption("Even / Odd", "DIGITEVEN", "Digits"),
                ContractTypeOption("Touch / No Touch", "ONETOUCH", "Barriers"),
                ContractTypeOption("Accumulators", "ACCU", "Multiplying"),
                ContractTypeOption("Asians", "ASIANU", "Averages")
            )

            // Dynamic selection state
            var activeCat by remember { mutableStateOf("CALL") }

            LazyRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(contractTypes) { opt ->
                    val isCatSelected = when (opt.code) {
                        "CALL" -> activeCat == "CALL" || activeCat == "PUT"
                        "DIGITUNDER" -> activeCat == "DIGITUNDER" || activeCat == "DIGITOVER"
                        "DIGITMATCH" -> activeCat == "DIGITMATCH" || activeCat == "DIGITDIFF"
                        "DIGITEVEN" -> activeCat == "DIGITEVEN" || activeCat == "DIGITODD"
                        "ONETOUCH" -> activeCat == "ONETOUCH" || activeCat == "NOTOUCH"
                        "ACCU" -> activeCat == "ACCU"
                        "ASIANU" -> activeCat == "ASIANU" || activeCat == "ASIAND"
                        else -> false
                    }

                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCatSelected) DerivPrimary else DerivSurfaceLight)
                            .border(
                                width = 1.dp,
                                color = if (isCatSelected) DerivPrimaryLight else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                activeCat = opt.code
                                viewModel.updateContractParameters(type = opt.code)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(opt.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(opt.sub, fontSize = 9.sp, color = if (isCatSelected) Color.White.copy(alpha = 0.8f) else White60)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub-options depending on the highlighted category
            when (activeCat) {
                "CALL", "PUT" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "CALL"
                                viewModel.updateContractParameters(type = "CALL")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "CALL") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TrendingUp, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RISE (CALL)")
                        }

                        Button(
                            onClick = {
                                activeCat = "PUT"
                                viewModel.updateContractParameters(type = "PUT")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "PUT") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.TrendingDown, null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FALL (PUT)")
                        }
                    }
                }

                "DIGITUNDER", "DIGITOVER" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "DIGITOVER"
                                viewModel.updateContractParameters(type = "DIGITOVER")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITOVER") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DIGIT OVER")
                        }

                        Button(
                            onClick = {
                                activeCat = "DIGITUNDER"
                                viewModel.updateContractParameters(type = "DIGITUNDER")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITUNDER") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DIGIT UNDER")
                        }
                    }
                }

                "DIGITMATCH", "DIGITDIFF" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "DIGITMATCH"
                                viewModel.updateContractParameters(type = "DIGITMATCH")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITMATCH") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MATCHES")
                        }

                        Button(
                            onClick = {
                                activeCat = "DIGITDIFF"
                                viewModel.updateContractParameters(type = "DIGITDIFF")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITDIFF") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DIFFERS")
                        }
                    }
                }

                "DIGITEVEN", "DIGITODD" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "DIGITEVEN"
                                viewModel.updateContractParameters(type = "DIGITEVEN")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITEVEN") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DIGIT EVEN")
                        }

                        Button(
                            onClick = {
                                activeCat = "DIGITODD"
                                viewModel.updateContractParameters(type = "DIGITODD")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "DIGITODD") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DIGIT ODD")
                        }
                    }
                }

                "ONETOUCH", "NOTOUCH" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "ONETOUCH"
                                viewModel.updateContractParameters(type = "ONETOUCH")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "ONETOUCH") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("TOUCH")
                        }

                        Button(
                            onClick = {
                                activeCat = "NOTOUCH"
                                viewModel.updateContractParameters(type = "NOTOUCH")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "NOTOUCH") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("NO TOUCH")
                        }
                    }
                }

                "ASIANU", "ASIAND" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                activeCat = "ASIANU"
                                viewModel.updateContractParameters(type = "ASIANU")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "ASIANU") BullishGreen else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ASIAN UP")
                        }

                        Button(
                            onClick = {
                                activeCat = "ASIAND"
                                viewModel.updateContractParameters(type = "ASIAND")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.selectedContractType == "ASIAND") BearishRed else DerivSurfaceLight
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ASIAN DOWN")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sub Controller inputs depending on contract
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stake Input
                Column(modifier = Modifier.weight(1f)) {
                    Text("STAKE (USD)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White60)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (viewModel.contractAmount > 1.0) {
                                    viewModel.updateContractParameters(amount = viewModel.contractAmount - 1.0)
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(DerivSurfaceLight, CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, null, tint = Color.White)
                        }

                        Text(
                            text = String.format(Locale.US, "$%.0f", viewModel.contractAmount),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                viewModel.updateContractParameters(amount = viewModel.contractAmount + 5.0)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(DerivSurfaceLight, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }

                // Show Duration Selector unless it's Accumulators (which have no duration)
                if (viewModel.selectedContractType != "ACCU") {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DURATION (TICKS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White60)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = viewModel.contractDuration.toFloat(),
                                onValueChange = {
                                    viewModel.updateContractParameters(duration = it.toInt())
                                },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = ActionBlue,
                                    activeTrackColor = ActionBlue,
                                    inactiveTrackColor = NeutralGray
                                ),
                                modifier = Modifier.weight(1.0f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${viewModel.contractDuration}t",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    // Growth Rate parameter for Accumulators
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GROWTH RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = White60)
                        Spacer(modifier = Modifier.height(6.dp))

                        val rates = listOf(0.01, 0.02, 0.03, 0.04, 0.05)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(rates) { rate ->
                                val isGrowthSelected = viewModel.contractGrowthRate == rate
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isGrowthSelected) HighlightGold else DerivSurfaceLight)
                                        .clickable {
                                            viewModel.updateContractParameters(growthRate = rate)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${(rate * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGrowthSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Digit Target selector if it is a Digit option
            if (viewModel.selectedContractType in listOf("DIGITUNDER", "DIGITOVER", "DIGITMATCH", "DIGITDIFF")) {
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    val label = if (viewModel.selectedContractType in listOf("DIGITMATCH", "DIGITDIFF")) "TARGET DIGIT (MATCH/DIFF)" else "BARRIER DIGIT (OVER/UNDER)"
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = White60
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (digit in 0..9) {
                            val isDigitSelected = viewModel.contractBarrier == digit.toString()
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(if (isDigitSelected) DerivPrimary else DerivSurfaceLight)
                                    .clickable {
                                        viewModel.updateContractParameters(barrier = digit.toString())
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = digit.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDigitSelected) Color.White else White90
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(color = White60.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(16.dp))

            // Live Proposal Status Loading vs Value Card
            if (!isAuthorized) {
                // Not Authorized Warning
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BearishRed.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Warning, null, tint = BearishRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Token Authorization Required",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Real-time pricing proposals and trading are disabled. Enter your own API Token at the top of the screen to unlock trading!",
                            fontSize = 11.sp,
                            color = White90,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                // Authorized Proposal status
                when {
                    isRequestingProposal -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = HighlightGold, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Requesting live proposal quote...", color = White90, fontSize = 12.sp)
                        }
                    }

                    proposalError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BearishRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Contract Parameter Error: $proposalError",
                                color = BearishRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    currentProposal != null -> {
                        val prop = currentProposal
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Proposal payout / metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KeyValueMetric(label = "Stake/Ask Price", value = String.format(Locale.US, "$%,.2f", prop.askPrice))
                                KeyValueMetric(label = "Potential Profit", value = String.format(Locale.US, "+$%,.2f", prop.profit), color = BullishGreen)
                                KeyValueMetric(label = "Payout Limit", value = String.format(Locale.US, "$%,.2f", prop.payout))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Return (ROI) percentage:",
                                    fontSize = 11.sp,
                                    color = White60
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(HighlightGold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%+.1f%% ROI", prop.roi),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = HighlightGold
                                    )
                                }
                            }

                            Text(
                                text = prop.longCode,
                                fontSize = 11.sp,
                                color = White90,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 14.sp
                            )

                            buyError?.let {
                                Text(
                                    text = "Purchase Attempt Failed: $it",
                                    color = BearishRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // High impact buy button
                            Button(
                                onClick = onPurchase,
                                enabled = !isBuying,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (viewModel.selectedContractType.endsWith("UNDER") ||
                                        viewModel.selectedContractType.endsWith("DIFF") ||
                                        viewModel.selectedContractType == "PUT" ||
                                        viewModel.selectedContractType == "ASIAND"
                                    ) BearishRed else BullishGreen,
                                    disabledContainerColor = NeutralGray
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                if (isBuying) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Executing option purchase...")
                                } else {
                                    Icon(Icons.Default.TrendingUp, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PURCHASE CONTRACT FOR $${String.format(Locale.US, "%.0f", prop.askPrice)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Please tweak parameters to request price quote calculation.",
                            color = White60,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyValueMetric(label: String, value: String, color: Color = Color.White) {
    Column {
        Text(text = label, fontSize = 9.sp, color = White60)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun RecentTradesSection(
    tradeHistory: List<TradeHistoryItem>,
    onClearHistory: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss dd-MMM", Locale.US) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = ActionBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "RESOLVED TRADES LOG",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = White90,
                    letterSpacing = 1.sp
                )
            }

            if (tradeHistory.isNotEmpty()) {
                TextButton(onClick = onClearHistory) {
                    Text("Clear All", color = BearishRed, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tradeHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DerivSurface, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = NeutralGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "History empty. Completed trades appear here.",
                        fontSize = 12.sp,
                        color = White60,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = DerivSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    tradeHistory.forEach { item ->
                        TradeHistoryRow(item = item, formatter = sdf)
                        HorizontalDivider(color = White60.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun TradeHistoryRow(item: TradeHistoryItem, formatter: SimpleDateFormat) {
    val isWin = item.status == "won"
    val color = if (isWin) BullishGreen else BearishRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.symbol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(ActionBlue.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.type,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ActionBlue
                    )
                }
            }

            Text(
                text = "ID: #${item.contractId} • Stake: ${String.format(Locale.US, "$%.2f", item.amount)}",
                fontSize = 10.sp,
                color = White60
            )

            Text(
                text = formatter.format(Date(item.timestamp)),
                fontSize = 9.sp,
                color = NeutralGray
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = if (isWin) "WIN" else "LOSS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = String.format(Locale.US, "%+,.2f USD", item.profit),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

// Struct declarations
data class ContractTypeOption(
    val label: String,
    val code: String,
    val sub: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
