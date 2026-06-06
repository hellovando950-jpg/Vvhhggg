package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TradingViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("deriv_trader_prefs", Context.MODE_PRIVATE)

    // UI States
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError = _connectionError.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized = _isAuthorized.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    // Account details
    private val _accountBalance = MutableStateFlow(0.0)
    val accountBalance = _accountBalance.asStateFlow()

    private val _accountCurrency = MutableStateFlow("USD")
    val accountCurrency = _accountCurrency.asStateFlow()

    private val _accountEmail = MutableStateFlow("")
    val accountEmail = _accountEmail.asStateFlow()

    private val _accountFullName = MutableStateFlow("")
    val accountFullName = _accountFullName.asStateFlow()

    private val _accountLoginId = MutableStateFlow("")
    val accountLoginId = _accountLoginId.asStateFlow()

    // Preferences (Demo vs Real)
    private val _isDemo = MutableStateFlow(sharedPrefs.getBoolean("is_demo", true))
    val isDemo = _isDemo.asStateFlow()

    private val _demoToken = MutableStateFlow(sharedPrefs.getString("demo_token", "") ?: "")
    val demoToken = _demoToken.asStateFlow()

    private val _realToken = MutableStateFlow(sharedPrefs.getString("real_token", "") ?: "")
    val realToken = _realToken.asStateFlow()

    private val _apiToken = MutableStateFlow(if (_isDemo.value) _demoToken.value else _realToken.value)
    val apiToken = _apiToken.asStateFlow()

    private val _appId = MutableStateFlow("33sKaNullz3jmWQs7OXxZ")
    val appId = _appId.asStateFlow()

    // Feedback Event Emitting Flow
    private val _feedbackMessage = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 16)
    val feedbackMessage = _feedbackMessage

    private val _selectedSymbol = MutableStateFlow("R_100")
    val selectedSymbol = _selectedSymbol.asStateFlow()

    // Market Tick Data
    private val _tickHistory = MutableStateFlow<List<Double>>(emptyList())
    val tickHistory = _tickHistory.asStateFlow()

    private val _currentQuote = MutableStateFlow<Double?>(null)
    val currentQuote = _currentQuote.asStateFlow()

    private val _quoteChange = MutableStateFlow(0.0) // positive, negative, or zero
    val quoteChange = _quoteChange.asStateFlow()

    // Proposal States
    private val _currentProposal = MutableStateFlow<ProposalDetails?>(null)
    val currentProposal = _currentProposal.asStateFlow()

    private val _isRequestingProposal = MutableStateFlow(false)
    val isRequestingProposal = _isRequestingProposal.asStateFlow()

    private val _proposalError = MutableStateFlow<String?>(null)
    val proposalError = _proposalError.asStateFlow()

    // Buy/Purchase States
    private val _isBuying = MutableStateFlow(false)
    val isBuying = _isBuying.asStateFlow()

    private val _buyError = MutableStateFlow<String?>(null)
    val buyError = _buyError.asStateFlow()

    // Active Contract tracking (Proposal Open Contract)
    private val _activeContract = MutableStateFlow<ActiveContractDetails?>(null)
    val activeContract = _activeContract.asStateFlow()

    // Historic Trade Items
    private val _tradeHistory = MutableStateFlow<List<TradeHistoryItem>>(emptyList())
    val tradeHistory = _tradeHistory.asStateFlow()

    // Predefined trade configurations
    var contractAmount = 10.0
    var contractDuration = 5 // ticks
    var contractBarrier = "4" // digit barrier (0-9)
    var contractGrowthRate = 0.02 // accumulator growth rate (0.01 - 0.05)
    var selectedContractType = "DIGITUNDER" // DIGITUNDER, DIGITOVER, DIGITMATCH, DIGITDIFF, CALL, PUT, ASIANU, ASIAND, ACCU

    // WebSocket Internals
    private var okHttpClient: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var lastTickSubscriptionId: String? = null
    private var lastPOCSubscriptionId: String? = null
    private var pendingProposalReqId: Int = 100

    init {
        // Load layout history from SharedPreferences if any saved previously
        loadTradeHistory()
        // Initialize and Connect WebSocket
        connectWebSocket()
    }

    private fun loadTradeHistory() {
        val historyStr = sharedPrefs.getString("trade_history_json", null)
        if (historyStr != null) {
            try {
                val array = org.json.JSONArray(historyStr)
                val items = mutableListOf<TradeHistoryItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    items.add(
                        TradeHistoryItem(
                            contractId = obj.getLong("contractId"),
                            symbol = obj.getString("symbol"),
                            type = obj.getString("type"),
                            amount = obj.getDouble("amount"),
                            payout = obj.getDouble("payout"),
                            profit = obj.getDouble("profit"),
                            status = obj.getString("status"),
                            timestamp = obj.getLong("timestamp")
                        )
                    )
                }
                _tradeHistory.value = items.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                Log.e("TradingViewModel", "Failed to deserialize trade history", e)
            }
        }
    }

    private fun saveTradeHistory(items: List<TradeHistoryItem>) {
        try {
            val array = org.json.JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("contractId", item.contractId)
                    put("symbol", item.symbol)
                    put("type", item.type)
                    put("amount", item.amount)
                    put("payout", item.payout)
                    put("profit", item.profit)
                    put("status", item.status)
                    put("timestamp", item.timestamp)
                }
                array.put(obj)
            }
            sharedPrefs.edit().putString("trade_history_json", array.toString()).apply()
        } catch (e: Exception) {
            Log.e("TradingViewModel", "Failed to serialize trade history", e)
        }
    }

    fun triggerFeedback(message: String) {
        viewModelScope.launch {
            _feedbackMessage.emit(message)
        }
    }

    fun toggleAccountType() {
        val newDemo = !_isDemo.value
        _isDemo.value = newDemo
        sharedPrefs.edit().putBoolean("is_demo", newDemo).apply()

        // Sync token value
        val activeToken = if (newDemo) _demoToken.value else _realToken.value
        _apiToken.value = activeToken

        triggerFeedback("Switched to ${if (newDemo) "Demo (Virtual)" else "Real"} Account Mode")
        connectWebSocket()
    }

    fun updateSettings(newToken: String, newAppId: String = "") {
        val token = newToken.trim()
        if (_isDemo.value) {
            _demoToken.value = token
            sharedPrefs.edit().putString("demo_token", token).apply()
        } else {
            _realToken.value = token
            sharedPrefs.edit().putString("real_token", token).apply()
        }
        _apiToken.value = token

        triggerFeedback("Settings stored for ${if (_isDemo.value) "Demo" else "Real"} Account")
        connectWebSocket()
    }

    fun selectSymbol(symbolCode: String) {
        if (_selectedSymbol.value == symbolCode) return
        _selectedSymbol.value = symbolCode
        _tickHistory.value = emptyList()
        _currentQuote.value = null

        triggerFeedback("Selected Market: $symbolCode")

        // Unsubscribe ticks first, then subscribe to the new one
        viewModelScope.launch(Dispatchers.IO) {
            forgetTicks()
            subscribeTicks(symbolCode)
            requestProposal() // Refresh proposal for new symbol
        }
    }

    fun updateContractParameters(
        amount: Double = contractAmount,
        duration: Int = contractDuration,
        barrier: String = contractBarrier,
        growthRate: Double = contractGrowthRate,
        type: String = selectedContractType
    ) {
        contractAmount = amount
        contractDuration = duration
        contractBarrier = barrier
        contractGrowthRate = growthRate
        selectedContractType = type

        requestProposal()
    }

    @Synchronized
    fun connectWebSocket() {
        // Disconnect existing if any
        disconnectWebSocket()

        _connectionState.value = ConnectionState.CONNECTING
        _connectionError.value = null

        val serverUrl = "wss://ws.derivws.com/websockets/v3?app_id=33sKaNullz3jmWQs7OXxZ"
        Log.d("TradingViewModel", "Connecting to Deriv WS: $serverUrl")

        okHttpClient = OkHttpClient.Builder()
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()

        val token = _apiToken.value
        val request = Request.Builder()
            .url(serverUrl)
            .apply {
                if (token.isNotEmpty()) {
                    addHeader("Authorization", "Bearer $token")
                }
            }
            .build()

        webSocket = okHttpClient?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("TradingViewModel", "WS Connection Opened!")
                _connectionState.value = ConnectionState.CONNECTED
                triggerFeedback("Connected to Deriv Trading Network.")

                // Send Authorize if token is available
                if (token.isNotEmpty()) {
                    Log.d("TradingViewModel", "Attempting authorization...")
                    sendAuthorize(token)
                } else {
                    _isAuthorized.value = false
                    _authError.value = "Missing API Token. Enter your token to trade!"
                }

                // Subscribe to selected symbols
                subscribeTicks(_selectedSymbol.value)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleWebSocketMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("TradingViewModel", "WS Closing: $reason")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("TradingViewModel", "WS Closed")
                _connectionState.value = ConnectionState.DISCONNECTED
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("TradingViewModel", "WS Connection Failure: ${t.message}", t)
                _connectionState.value = ConnectionState.ERROR
                _connectionError.value = t.localizedMessage ?: "Connection failure"
                _isAuthorized.value = false
                triggerFeedback("Connection Failure. Please verify your internet or token.")
            }
        })
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Normal closing")
        webSocket = null
        okHttpClient = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    private fun sendAuthorize(token: String) {
        val authObj = JSONObject().apply {
            put("authorize", token)
        }
        sendJson(authObj)
    }

    private fun subscribeTicks(symbol: String) {
        val tickObj = JSONObject().apply {
            put("ticks", symbol)
        }
        sendJson(tickObj)
    }

    private fun forgetTicks() {
        val forgetAll = JSONObject().apply {
            put("forget_all", "ticks")
        }
        sendJson(forgetAll)
    }

    fun requestProposal() {
        if (!_isAuthorized.value) {
            _proposalError.value = "Auth token required for real proposal quotes"
            _currentProposal.value = null
            return
        }

        _isRequestingProposal.value = true
        _proposalError.value = null

        try {
            val reqId = ++pendingProposalReqId
            val propObj = JSONObject().apply {
                put("proposal", 1)
                put("req_id", reqId)
                put("amount", contractAmount)
                put("basis", "stake")
                put("currency", _accountCurrency.value.ifEmpty { "USD" })
                put("symbol", _selectedSymbol.value)
                put("contract_type", selectedContractType)

                // Different contracts require different keys
                when (selectedContractType) {
                    "ACCU" -> {
                        put("growth_rate", contractGrowthRate)
                    }
                    "CALL", "PUT" -> {
                        put("duration", contractDuration)
                        put("duration_unit", "t")
                    }
                    "ASIANU", "ASIAND" -> {
                        put("duration", contractDuration)
                        put("duration_unit", "t")
                    }
                    "DIGITUNDER", "DIGITOVER", "DIGITMATCH", "DIGITDIFF" -> {
                        put("duration", contractDuration)
                        put("duration_unit", "t")
                        put("barrier", contractBarrier)
                    }
                    "DIGITEVEN", "DIGITODD" -> {
                        put("duration", contractDuration)
                        put("duration_unit", "t")
                    }
                    "ONETOUCH", "NOTOUCH" -> {
                        put("duration", contractDuration)
                        put("duration_unit", "t")
                        put("barrier", if (contractBarrier.isEmpty() || contractBarrier == "0") "+1.5" else "+$contractBarrier")
                    }
                }
            }

            Log.d("TradingViewModel", "Sending Proposal Request: $propObj")
            sendJson(propObj)
        } catch (e: Exception) {
            _proposalError.value = e.localizedMessage ?: "Failed to generate proposal payload"
            _isRequestingProposal.value = false
        }
    }

    fun purchaseCurrentProposal() {
        val proposal = _currentProposal.value
        if (proposal == null) {
            _buyError.value = "No active proposal available. Please wait for pricing..."
            return
        }

        _isBuying.value = true
        _buyError.value = null

        val buyObj = JSONObject().apply {
            put("buy", proposal.id)
            put("price", proposal.askPrice)
        }

        Log.d("TradingViewModel", "Buying contract: $buyObj")
        sendJson(buyObj)
    }

    private fun subscribeContractUpdates(contractId: Long) {
        // Forget previous POC if any
        forgetContractUpdates()

        val pocObj = JSONObject().apply {
            put("proposal_open_contract", 1)
            put("contract_id", contractId)
            put("subscribe", 1)
        }
        Log.d("TradingViewModel", "Subscribing to Open Contract updates: $pocObj")
        sendJson(pocObj)
    }

    fun forgetContractUpdates() {
        _activeContract.value = null
        val forgetAll = JSONObject().apply {
            put("forget_all", "proposal_open_contract")
        }
        sendJson(forgetAll)
    }

    private fun sendJson(json: JSONObject) {
        val socket = webSocket
        if (socket != null && _connectionState.value == ConnectionState.CONNECTED) {
            val token = _apiToken.value
            if (token.isNotEmpty()) {
                json.put("bearer_token", "Bearer $token")
            }
            socket.send(json.toString())
        } else {
            Log.e("TradingViewModel", "Cannot send message. WS is not connected.")
        }
    }

    private fun handleWebSocketMessage(text: String) {
        try {
            val response = JSONObject(text)
            val msgType = response.optString("msg_type")

            // Parse common error
            val errorObj = response.optJSONObject("error")
            if (errorObj != null) {
                val errorCode = errorObj.optString("code")
                val errorMessage = errorObj.optString("message")
                Log.e("TradingViewModel", "API Error: [$errorCode] $errorMessage")

                when (msgType) {
                    "authorize" -> {
                        _isAuthorized.value = false
                        _authError.value = errorMessage
                    }
                    "proposal" -> {
                        _proposalError.value = errorMessage
                        _isRequestingProposal.value = false
                        _currentProposal.value = null
                    }
                    "buy" -> {
                        _buyError.value = errorMessage
                        _isBuying.value = false
                    }
                    "proposal_open_contract" -> {
                        Log.e("TradingViewModel", "POC Error: $errorMessage")
                    }
                }
                return
            }

            // Normal msg_type handlers
            when (msgType) {
                "authorize" -> {
                    val auth = response.optJSONObject("authorize")
                    if (auth != null) {
                        _isAuthorized.value = true
                        _authError.value = null
                        _accountBalance.value = auth.optDouble("balance", 0.0)
                        _accountCurrency.value = auth.optString("currency", "USD")
                        _accountEmail.value = auth.optString("email", "")
                        val fName = auth.optString("fullname", "")
                        _accountFullName.value = fName
                        _accountLoginId.value = auth.optString("loginid", "")

                        Log.d("TradingViewModel", "Successfully Authorized! Balance: ${_accountBalance.value}")
                        triggerFeedback("Authorized: " + (if (fName.isEmpty()) "Client Account" else fName) + " ($" + String.format(java.util.Locale.US, "%,.2f", _accountBalance.value) + " " + _accountCurrency.value + ")")

                        // Re-trigger proposal once authorized
                        requestProposal()
                    }
                }

                "tick" -> {
                    val tick = response.optJSONObject("tick")
                    if (tick != null) {
                        val symbol = tick.optString("symbol")
                        if (symbol == _selectedSymbol.value) {
                            val price = tick.optDouble("quote", 0.0)
                            val subId = tick.optString("id")
                            if (subId.isNotEmpty()) {
                                lastTickSubscriptionId = subId
                            }

                            val oldPrice = _currentQuote.value
                            _currentQuote.value = price
                            if (oldPrice != null) {
                                _quoteChange.value = price - oldPrice
                            } else {
                                _quoteChange.value = 0.0
                            }

                            // Keep last 30 quotes in history for drawing the chart
                            val history = _tickHistory.value.toMutableList()
                            history.add(price)
                            if (history.size > 34) {
                                history.removeAt(0)
                            }
                            _tickHistory.value = history
                        }
                    }
                }

                "proposal" -> {
                    val prop = response.optJSONObject("proposal")
                    if (prop != null) {
                        val id = prop.optString("id")
                        val payout = prop.optDouble("payout", 0.0)
                        val askPrice = prop.optDouble("ask_price", 0.0)
                        val longcode = prop.optString("longcode", "")
                        val spot = prop.optDouble("spot", 0.0)
                        val profit = payout - askPrice
                        val roi = if (askPrice > 0) (profit / askPrice) * 100 else 0.0

                        _currentProposal.value = ProposalDetails(
                            id = id,
                            payout = payout,
                            askPrice = askPrice,
                            longCode = longcode,
                            spot = spot,
                            profit = profit,
                            roi = roi
                        )
                        _isRequestingProposal.value = false
                        _proposalError.value = null
                    }
                }

                "buy" -> {
                    val buy = response.optJSONObject("buy")
                    if (buy != null) {
                        val contractId = buy.optLong("contract_id")
                        val balanceAfter = buy.optDouble("balance_after", 0.0)
                        _accountBalance.value = balanceAfter
                        _isBuying.value = false
                        _buyError.value = null

                        triggerFeedback("Contract purchased successfully! ID: #$contractId")

                        // Active tracking
                        subscribeContractUpdates(contractId)
                    }
                }

                "proposal_open_contract" -> {
                    val poc = response.optJSONObject("proposal_open_contract")
                    if (poc != null) {
                        val status = poc.optString("status") // open, won, lost
                        val profit = poc.optDouble("profit", 0.0)
                        val contractId = poc.optLong("contract_id")
                        val currentSpot = poc.optDouble("current_spot", 0.0)
                        val entrySpotValue = poc.optDouble("entry_tick")
                        val entrySpot = if (entrySpotValue.isNaN()) null else entrySpotValue
                        val isExpired = poc.optInt("is_expired", 0) == 1 || status != "open"
                        val longcode = poc.optString("longcode")
                        val type = poc.optString("contract_type")
                        val subId = poc.optString("id")

                        if (subId.isNotEmpty()) {
                            lastPOCSubscriptionId = subId
                        }

                        val active = ActiveContractDetails(
                            contractId = contractId,
                            status = status,
                            profit = profit,
                            currentSpot = currentSpot,
                            entrySpot = entrySpot,
                            isExpired = isExpired,
                            longCode = longcode,
                            type = type
                        )
                        _activeContract.value = active

                        if (isExpired) {
                            // Save to trade history list!
                            val finalPayout = poc.optDouble("payout", 0.0)
                            val buyPrice = poc.optDouble("buy_price", 0.0)
                            val finalProfit = profit

                            val historyItem = TradeHistoryItem(
                                contractId = contractId,
                                symbol = _selectedSymbol.value,
                                type = type,
                                amount = buyPrice,
                                payout = finalPayout,
                                profit = finalProfit,
                                status = status,
                                timestamp = System.currentTimeMillis()
                            )

                            addTradeToHistory(historyItem)

                            triggerFeedback("Contract No. $contractId closed: ${status.uppercase()} (${if (finalProfit >= 0f) "+" else ""}${String.format(java.util.Locale.US, "%.2f", finalProfit)} USD)")

                            // Unsubscribe this contract as completed
                            forgetContractUpdates()

                            // Also, refresh account balance right after contract closes
                            val token = _apiToken.value
                            if (token.isNotEmpty()) {
                                sendAuthorize(token)
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("TradingViewModel", "Error parsing WebSocket message content: $text", e)
        }
    }

    private fun addTradeToHistory(item: TradeHistoryItem) {
        val historyList = _tradeHistory.value.toMutableList()
        // Prevent duplicate logs
        if (historyList.none { it.contractId == item.contractId }) {
            historyList.add(0, item)
            _tradeHistory.value = historyList
            saveTradeHistory(historyList)
        }
    }

    fun clearTradeHistory() {
        _tradeHistory.value = emptyList()
        sharedPrefs.edit().remove("trade_history_json").apply()
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}

// Data structures and Enums
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class ProposalDetails(
    val id: String,
    val payout: Double,
    val askPrice: Double,
    val longCode: String,
    val spot: Double,
    val profit: Double,
    val roi: Double
)

data class ActiveContractDetails(
    val contractId: Long,
    val status: String, // open, won, lost
    val profit: Double,
    val currentSpot: Double,
    val entrySpot: Double?,
    val isExpired: Boolean,
    val longCode: String,
    val type: String
)

data class TradeHistoryItem(
    val contractId: Long,
    val symbol: String,
    val type: String,
    val amount: Double,
    val payout: Double,
    val profit: Double,
    val status: String, // won, lost
    val timestamp: Long
)
