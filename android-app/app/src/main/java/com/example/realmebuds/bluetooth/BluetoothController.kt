package com.example.realmebuds.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.realmebuds.protocol.RealmeProtocol
import com.example.realmebuds.protocol.RealmeProtocol.toHexString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class BluetoothDeviceItem(
    val name: String,
    val address: String
)

data class LogEntry(
    val timestamp: String,
    val level: String,
    val message: String
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val deviceName: String, val address: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class BluetoothController(private val context: Context) {

    companion object {
        // Standard Bluetooth Serial Port Profile (SPP) UUID
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** Upper bound for the blocking RFCOMM connect attempt. */
        const val CONNECT_TIMEOUT_MS = 8_000L

        /** How long to wait for the first response byte. */
        const val RESPONSE_TIMEOUT_MS = 1_200L

        /** Return once the stream has been quiet for this long. */
        const val RESPONSE_IDLE_MS = 120L

        /** Battery poll interval while connected. */
        const val BATTERY_POLL_INTERVAL_MS = 8_000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    /**
     * Serialises every write+read pair on the single shared RFCOMM socket.
     * `setGameMode()`, `setAncMode()`, `queryBattery()` and the 8 s battery
     * poll all run on independent coroutines; without this lock two of them
     * could interleave their bytes and steal each other's responses.
     * Every full transaction (packet build -> write -> read) runs under it.
     */
    private val ioMutex = Mutex()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    /**
     * Monotonic 0..255 transfer id. The *current* value goes into the packet,
     * then it advances with `(x + 1) and 0xFF` after **every write attempt**,
     * success or not — for commands and battery queries alike, matching the
     * Windows controller (`t200x_core.py`). Only mutated under [ioMutex].
     */
    private var transferId: Int = 0

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _activeAncMode = MutableStateFlow<RealmeProtocol.AncMode?>(null)
    val activeAncMode: StateFlow<RealmeProtocol.AncMode?> = _activeAncMode.asStateFlow()

    private val _isGameModeOn = MutableStateFlow<Boolean?>(null)
    val isGameModeOn: StateFlow<Boolean?> = _isGameModeOn.asStateFlow()

    private val _batteryLeft = MutableStateFlow<Int?>(null)
    val batteryLeft: StateFlow<Int?> = _batteryLeft.asStateFlow()

    private val _batteryRight = MutableStateFlow<Int?>(null)
    val batteryRight: StateFlow<Int?> = _batteryRight.asStateFlow()

    private val _batteryCase = MutableStateFlow<Int?>(null)
    val batteryCase: StateFlow<Int?> = _batteryCase.asStateFlow()

    private var batteryPollJob: Job? = null

    private val _logEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val logEntries: StateFlow<List<LogEntry>> = _logEntries.asStateFlow()

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    /** Result of a bounded RFCOMM connect attempt. */
    private sealed interface ConnectOutcome {
        object Success : ConnectOutcome
        object TimedOut : ConnectOutcome
        data class Failed(val reason: String) : ConnectOutcome
    }

    @Synchronized
    fun log(level: String, message: String) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            level = level,
            message = message
        )
        _logEntries.value = (_logEntries.value + entry).takeLast(200)
    }

    fun clearLogs() {
        _logEntries.value = emptyList()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceItem> {
        val adapter = bluetoothAdapter ?: return emptyList()
        return try {
            adapter.bondedDevices?.map { device ->
                BluetoothDeviceItem(
                    name = device.name ?: "Unknown Device",
                    address = device.address
                )
            }?.sortedWith(compareByDescending<BluetoothDeviceItem> {
                val n = it.name.lowercase()
                n.contains("realme") || n.contains("buds") || n.contains("t200") || n.contains("t300") || n.contains("t100")
            }.thenBy { it.name }) ?: emptyList()
        } catch (e: SecurityException) {
            log("ERROR", "Bluetooth permission not granted: ${e.message}")
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = ConnectionState.Error("Bluetooth is disabled or not available")
            log("ERROR", "Bluetooth is disabled or not available")
            return
        }

        scope.launch {
            // Tear down any previous session without racing in-flight transactions.
            ioMutex.withLock { disconnectInternal(silent = true) }

            val device: BluetoothDevice = try {
                adapter.getRemoteDevice(address)
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Invalid device address: $address")
                log("ERROR", "Invalid device address: $address")
                return@launch
            }

            val deviceName = try { device.name ?: address } catch (e: SecurityException) { address }
            _connectionState.value = ConnectionState.Connecting(deviceName)
            log("INFO", "Connecting to $deviceName ($address)...")

            try {
                // Try standard SPP RFCOMM socket first, insecure RFCOMM as fallback.
                val newSocket: BluetoothSocket = try {
                    device.createRfcommSocketToServiceRecord(SPP_UUID)
                } catch (e: Exception) {
                    try {
                        device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    } catch (e2: Exception) {
                        failConnect(deviceName, "could not create an RFCOMM socket")
                        return@launch
                    }
                }

                // Published before connecting so a concurrent disconnect() can close it.
                socket = newSocket

                when (val outcome = connectWithTimeout(newSocket)) {
                    ConnectOutcome.Success -> Unit
                    ConnectOutcome.TimedOut -> {
                        failConnect(
                            deviceName,
                            "connection timed out after ${CONNECT_TIMEOUT_MS / 1000} seconds — " +
                                "make sure the earbuds are in range and not connected elsewhere"
                        )
                        return@launch
                    }
                    is ConnectOutcome.Failed -> {
                        failConnect(deviceName, outcome.reason)
                        return@launch
                    }
                }

                // The user may have disconnected (or started another connect)
                // while the socket was negotiating — bail out and release it.
                // Publication of the streams + startup drain happens under the
                // same mutex as every write+read, so a command issued the
                // instant we go Connected can never race the drain.
                val ready = ioMutex.withLock {
                    if (socket !== newSocket || _connectionState.value !is ConnectionState.Connecting) {
                        closeQuietly(newSocket)
                        false
                    } else {
                        outputStream = newSocket.outputStream
                        inputStream = newSocket.inputStream

                        _connectionState.value = ConnectionState.Connected(deviceName, address)
                        log("INFO", "Connected to $deviceName successfully!")

                        // Drain initial startup bytes
                        delay(400)
                        drainStartupTraffic()
                        true
                    }
                }
                if (!ready) return@launch

                if (_connectionState.value is ConnectionState.Connected) {
                    startBatteryPolling()
                }

            } catch (e: Exception) {
                failConnect(deviceName, e.message ?: "unexpected ${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * Runs the blocking [BluetoothSocket.connect] on a worker thread and gives
     * up after [CONNECT_TIMEOUT_MS]. On timeout the socket is closed so the
     * blocked native connect() call is unblocked and cannot leak.
     */
    private suspend fun connectWithTimeout(sock: BluetoothSocket): ConnectOutcome {
        val attempt: Deferred<ConnectOutcome> = scope.async(Dispatchers.IO) {
            try {
                sock.connect()
                ConnectOutcome.Success
            } catch (e: Exception) {
                ConnectOutcome.Failed(e.message ?: "connection was refused")
            }
        }

        val outcome = withTimeoutOrNull(CONNECT_TIMEOUT_MS) { attempt.await() }
            ?: ConnectOutcome.TimedOut

        if (outcome != ConnectOutcome.Success) {
            // Cancelling alone does not interrupt a blocked native connect();
            // closing the socket does.
            attempt.cancel()
            closeQuietly(sock)
        }
        return outcome
    }

    /**
     * Resets state to Disconnected and surfaces a human-readable [reason].
     * If the user already disconnected on purpose, that takes precedence and
     * no error banner is shown.
     */
    private suspend fun failConnect(deviceName: String, reason: String) {
        val userDisconnected = _connectionState.value is ConnectionState.Disconnected
        ioMutex.withLock { disconnectInternal(silent = true) }
        if (userDisconnected) return
        val message = "Could not connect to $deviceName: $reason"
        _connectionState.value = ConnectionState.Error(message)
        log("ERROR", message)
    }

    fun disconnect() {
        scope.launch {
            // Waits for any in-flight write+read to finish first (bounded by
            // the response timeout), so the socket is never closed mid-write.
            ioMutex.withLock { disconnectInternal(silent = false) }
        }
    }

    private fun disconnectInternal(silent: Boolean = false) {
        batteryPollJob?.cancel()
        batteryPollJob = null

        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (ignored: Exception) {}

        socket = null
        outputStream = null
        inputStream = null

        _connectionState.value = ConnectionState.Disconnected
        _activeAncMode.value = null
        _isGameModeOn.value = null
        _batteryLeft.value = null
        _batteryRight.value = null
        _batteryCase.value = null

        if (!silent) {
            log("INFO", "Disconnected from device")
        }
    }

    private fun closeQuietly(sock: BluetoothSocket?) {
        try {
            sock?.close()
        } catch (ignored: Exception) {}
    }

    private fun drainStartupTraffic() {
        try {
            val stream = inputStream ?: return
            val buffer = ByteArray(2048)
            while (stream.available() > 0) {
                stream.read(buffer)
            }
        } catch (ignored: Exception) {}
    }

    /**
     * Reads a response without blocking for the whole timeout:
     * * wait up to [timeoutMs] for the first byte,
     * * then return once [idleMs] pass with no new data.
     *
     * Polling every 20 ms keeps this a plain blocking call (no selector
     * needed); the deadline guarantees it can never spin forever.
     *
     * @return the bytes read, or an empty array for a null/closed stream or a
     * read error (already logged).
     */
    private fun readResponse(
        stream: InputStream?,
        timeoutMs: Long = RESPONSE_TIMEOUT_MS,
        idleMs: Long = RESPONSE_IDLE_MS
    ): ByteArray {
        if (stream == null) return ByteArray(0)
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        val start = System.currentTimeMillis()
        var lastByteAt = start
        var receivedAny = false

        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                val avail = stream.available()
                if (avail > 0) {
                    val read = stream.read(buffer, 0, minOf(avail, buffer.size))
                    if (read > 0) {
                        out.write(buffer, 0, read)
                        receivedAny = true
                        lastByteAt = System.currentTimeMillis()
                        continue
                    }
                }
                // First byte: keep waiting until the overall timeout elapses.
                // Afterwards: return as soon as the stream has been quiet.
                if (receivedAny && System.currentTimeMillis() - lastByteAt >= idleMs) {
                    break
                }
                Thread.sleep(20)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            } catch (e: IOException) {
                log("ERROR", "Read error: ${e.message ?: "I/O error"}")
                break
            }
        }
        return out.toByteArray()
    }

    fun setGameMode(enabled: Boolean) {
        scope.launch {
            val desc = "Game Mode ${if (enabled) "ON" else "OFF"}"
            val resp = executeTransaction(desc) { id ->
                RealmeProtocol.makeGameModePacket(enabled, id)
            }
            if (resp != null) {
                _isGameModeOn.value = enabled
                log("SUCCESS", "$desc confirmed by earbuds!")
            }
        }
    }

    fun setAncMode(mode: RealmeProtocol.AncMode) {
        scope.launch {
            val desc = "ANC: ${mode.displayName}"
            val resp = executeTransaction(desc) { id ->
                RealmeProtocol.makeAncPacket(mode.value, id)
            }
            if (resp != null) {
                _activeAncMode.value = mode
                log("SUCCESS", "${mode.displayName} confirmed by earbuds!")
            }
        }
    }

    fun queryBattery() {
        scope.launch {
            queryBatteryInternal()
        }
    }

    private fun startBatteryPolling() {
        batteryPollJob?.cancel()
        batteryPollJob = scope.launch {
            delay(500)
            while (_connectionState.value is ConnectionState.Connected) {
                queryBatteryInternal()
                delay(BATTERY_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun queryBatteryInternal() {
        executeTransaction("Battery Query") { id ->
            RealmeProtocol.makeBatteryQueryPacket(id)
        }
    }

    /**
     * Runs one full transaction — build the packet with the current transfer
     * id, write it, read the response — atomically behind [ioMutex], so a
     * battery poll can never overlap a user command on the shared socket.
     *
     * @return the raw response, or `null` when not connected / no answer.
     */
    private suspend fun executeTransaction(
        description: String,
        buildPacket: (transferId: Int) -> ByteArray
    ): ByteArray? {
        if (_connectionState.value !is ConnectionState.Connected) {
            log("ERROR", "Cannot send [$description]: Not connected")
            return null
        }
        return ioMutex.withLock {
            // Re-check: the connection may have dropped while we waited.
            if (_connectionState.value !is ConnectionState.Connected) {
                log("ERROR", "Cannot send [$description]: Not connected")
                return@withLock null
            }
            val packet = buildPacket(transferId)
            log("TX", "[$description] ${packet.toHexString()}")
            sendPacketInternal(packet)
        }
    }

    /**
     * Writes [packet] and waits for its response. Must be called while
     * holding [ioMutex]; guards against a null/closed stream and against
     * writing after a disconnect.
     */
    private fun sendPacketInternal(packet: ByteArray): ByteArray? {
        if (_connectionState.value !is ConnectionState.Connected) return null
        val out = outputStream ?: return null
        val stream = inputStream ?: return null

        return try {
            try {
                out.write(packet)
                out.flush()
            } finally {
                // transfer_id rule (kept in sync with t200x_core.py): the id
                // used in the packet is advanced after every write attempt,
                // whether or not the write (or the response) succeeded.
                transferId = (transferId + 1) and 0xFF
            }

            val resp = readResponse(stream)
            if (resp.isNotEmpty()) {
                log("RX", resp.toHexString())
                val battery = RealmeProtocol.parseBatteryResponse(resp)
                if (battery != null) {
                    _batteryLeft.value = battery.left
                    _batteryRight.value = battery.right
                    _batteryCase.value = battery.case
                    val caseStr = if ((battery.case ?: 0) > 0) "${battery.case}%" else "--"
                    log("BATTERY", "🎧 L: ${battery.left ?: "--"}% | 🎧 R: ${battery.right ?: "--"}% | 🔋 Case: $caseStr")
                }
                resp
            } else {
                log("WARN", "No response received within timeout.")
                null
            }
        } catch (e: IOException) {
            log("ERROR", "Bluetooth I/O error: ${e.message ?: "connection lost"}")
            disconnectInternal()
            null
        }
    }
}
