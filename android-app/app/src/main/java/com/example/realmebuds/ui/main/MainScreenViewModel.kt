package com.example.realmebuds.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.realmebuds.bluetooth.BluetoothController
import com.example.realmebuds.bluetooth.BluetoothDeviceItem
import com.example.realmebuds.bluetooth.ConnectionState
import com.example.realmebuds.bluetooth.LogEntry
import com.example.realmebuds.protocol.RealmeProtocol
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothController = BluetoothController(application.applicationContext)

    val connectionState: StateFlow<ConnectionState> = bluetoothController.connectionState
    val activeAncMode: StateFlow<RealmeProtocol.AncMode?> = bluetoothController.activeAncMode
    val isGameModeOn: StateFlow<Boolean?> = bluetoothController.isGameModeOn
    val logEntries: StateFlow<List<LogEntry>> = bluetoothController.logEntries

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceItem?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceItem?> = _selectedDevice.asStateFlow()

    // Live battery levels from BluetoothController
    val batteryLeft: StateFlow<Int?> = bluetoothController.batteryLeft
    val batteryRight: StateFlow<Int?> = bluetoothController.batteryRight
    val batteryCase: StateFlow<Int?> = bluetoothController.batteryCase

    // ------------------------------------------------------------------
    // UI-only state (derived from the Bluetooth layer, never replaces it)
    // ------------------------------------------------------------------

    /**
     * The most recent connection error. Stays visible after the controller falls
     * back to [ConnectionState.Disconnected] so failures are not silently lost;
     * cleared as soon as a new attempt starts or a connection succeeds.
     */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /** True while [refreshDevices] is querying the bonded-device list. */
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /**
     * True while a command (ANC / game mode) is waiting for the earbuds to
     * confirm. Cleared when the corresponding state confirms, or on timeout.
     */
    private val _isCommandInFlight = MutableStateFlow(false)
    val isCommandInFlight: StateFlow<Boolean> = _isCommandInFlight.asStateFlow()

    private var commandTimeoutJob: Job? = null

    // Dialog sheets visibility
    private val _showDeviceDialog = MutableStateFlow(false)
    val showDeviceDialog: StateFlow<Boolean> = _showDeviceDialog.asStateFlow()

    private val _showHelpDialog = MutableStateFlow(false)
    val showHelpDialog: StateFlow<Boolean> = _showHelpDialog.asStateFlow()

    private val _showProtocolMonitor = MutableStateFlow(false)
    val showProtocolMonitor: StateFlow<Boolean> = _showProtocolMonitor.asStateFlow()

    init {
        refreshDevices()

        viewModelScope.launch {
            connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Error -> _lastError.value = state.message
                    // Starting (or having) a connection resets any previous failure.
                    is ConnectionState.Connecting -> _lastError.value = null
                    is ConnectionState.Connected -> _lastError.value = null
                    // Keep the last error visible after the drop to Disconnected.
                    ConnectionState.Disconnected -> Unit
                }
            }
        }

        viewModelScope.launch {
            combine(activeAncMode, isGameModeOn) { anc, game -> anc to game }.collect {
                if (_isCommandInFlight.value) {
                    commandTimeoutJob?.cancel()
                    _isCommandInFlight.value = false
                }
            }
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            val devices = try {
                withContext(Dispatchers.IO) {
                    bluetoothController.getPairedDevices()
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                emptyList()
            } finally {
                _isScanning.value = false
            }
            _pairedDevices.value = devices
            if (_selectedDevice.value == null && devices.isNotEmpty()) {
                _selectedDevice.value = devices.first()
            }
        }
    }

    fun selectDevice(device: BluetoothDeviceItem) {
        _selectedDevice.value = device
    }

    fun toggleConnection() {
        val current = connectionState.value
        if (current is ConnectionState.Connected || current is ConnectionState.Connecting) {
            bluetoothController.disconnect()
        } else {
            val target = selectedDevice.value
            if (target != null) {
                bluetoothController.connect(target.address)
            } else {
                _showDeviceDialog.value = true
            }
        }
    }

    fun connectTo(device: BluetoothDeviceItem) {
        _selectedDevice.value = device
        bluetoothController.connect(device.address)
        _showDeviceDialog.value = false
    }

    /** Re-reads the battery levels of the connected earbuds / case. */
    fun refreshBattery() {
        bluetoothController.queryBattery()
    }

    /** Manually hides the current error banner. */
    fun dismissError() {
        _lastError.value = null
    }

    fun setAncMode(mode: RealmeProtocol.AncMode) {
        bluetoothController.setAncMode(mode)
        if (activeAncMode.value != mode) {
            markCommandInFlight()
        }
    }

    fun setGameMode(enabled: Boolean) {
        bluetoothController.setGameMode(enabled)
        if (isGameModeOn.value != enabled) {
            markCommandInFlight()
        }
    }

    fun openDeviceDialog() { _showDeviceDialog.value = true }
    fun closeDeviceDialog() { _showDeviceDialog.value = false }

    fun openHelpDialog() { _showHelpDialog.value = true }
    fun closeHelpDialog() { _showHelpDialog.value = false }

    fun openProtocolMonitor() { _showProtocolMonitor.value = true }
    fun closeProtocolMonitor() { _showProtocolMonitor.value = false }

    fun clearLogs() {
        bluetoothController.clearLogs()
    }

    private fun markCommandInFlight() {
        _isCommandInFlight.value = true
        commandTimeoutJob?.cancel()
        commandTimeoutJob = viewModelScope.launch {
            delay(2_500L)
            _isCommandInFlight.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.disconnect()
    }
}
