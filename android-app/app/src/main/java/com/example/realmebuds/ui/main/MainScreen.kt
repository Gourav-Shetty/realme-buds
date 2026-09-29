package com.example.realmebuds.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.realmebuds.R
import com.example.realmebuds.bluetooth.BluetoothDeviceItem
import com.example.realmebuds.bluetooth.ConnectionState
import com.example.realmebuds.bluetooth.LogEntry
import com.example.realmebuds.protocol.RealmeProtocol
import com.example.realmebuds.theme.BatteryLevelHigh
import com.example.realmebuds.theme.BatteryLevelHighTint
import com.example.realmebuds.theme.BatteryLevelLow
import com.example.realmebuds.theme.BatteryLevelLowTint
import com.example.realmebuds.theme.BatteryLevelMedium
import com.example.realmebuds.theme.BatteryLevelMediumTint
import com.example.realmebuds.theme.BatteryLevelUnknown
import com.example.realmebuds.theme.BatteryLevelUnknownTint
import com.example.realmebuds.theme.RealmeCardBorder
import com.example.realmebuds.theme.RealmeCardWhite
import com.example.realmebuds.theme.RealmeCircleActive
import com.example.realmebuds.theme.RealmeCircleInactive
import com.example.realmebuds.theme.RealmeDivider
import com.example.realmebuds.theme.RealmeHeroCircle
import com.example.realmebuds.theme.RealmeHeroGlowBottom
import com.example.realmebuds.theme.RealmeHeroGlowTop
import com.example.realmebuds.theme.RealmeIconActive
import com.example.realmebuds.theme.RealmeIconInactive
import com.example.realmebuds.theme.RealmePageBg
import com.example.realmebuds.theme.RealmePillBg
import com.example.realmebuds.theme.RealmeSurfaceSoft
import com.example.realmebuds.theme.RealmeSwitchActive
import com.example.realmebuds.theme.RealmeSwitchInactiveThumb
import com.example.realmebuds.theme.RealmeSwitchInactiveTrack
import com.example.realmebuds.theme.RealmeTextDark
import com.example.realmebuds.theme.RealmeTextGrey
import com.example.realmebuds.theme.RealmeTextLight
import com.example.realmebuds.theme.RealmeType
import com.example.realmebuds.theme.StatusDanger
import com.example.realmebuds.theme.StatusDangerBorder
import com.example.realmebuds.theme.StatusDangerTint
import com.example.realmebuds.theme.StatusNeutralTint
import com.example.realmebuds.theme.StatusSuccess
import com.example.realmebuds.theme.StatusSuccessTint
import com.example.realmebuds.theme.StatusWarning
import com.example.realmebuds.theme.StatusWarningTint
import com.example.realmebuds.theme.TermBackground
import com.example.realmebuds.theme.TermError
import com.example.realmebuds.theme.TermInfo
import com.example.realmebuds.theme.TermMuted
import com.example.realmebuds.theme.TermRx
import com.example.realmebuds.theme.TermSuccess
import com.example.realmebuds.theme.TermTx
import com.example.realmebuds.theme.TermWarn
import kotlin.math.cos
import kotlin.math.sin

/** Hard cap on rendered log rows so the monitor stays cheap on old devices. */
private const val MAX_RENDERED_LOG_LINES = 200

/** Layout rhythm shared by every card on the screen. */
private val CardShape = RoundedCornerShape(22.dp)
private val CardPadding = 20.dp
private val ScreenGutter = 16.dp
private val CardGap = 14.dp

/** High level hero states, used to cross-fade the hero content. */
private enum class HeroMode { CONNECTED, CONNECTING, SCANNING, DISCONNECTED }

// ---------------------------------------------------------------------------
// Colour helpers — top-level so nothing is allocated inside composition loops.
// ---------------------------------------------------------------------------

private fun batteryLevelColor(level: Int?): Color = when {
    level == null || level <= 0 -> BatteryLevelUnknown
    level >= 50 -> BatteryLevelHigh
    level >= 20 -> BatteryLevelMedium
    else -> BatteryLevelLow
}

private fun batteryLevelTint(level: Int?): Color = when {
    level == null || level <= 0 -> BatteryLevelUnknownTint
    level >= 50 -> BatteryLevelHighTint
    level >= 20 -> BatteryLevelMediumTint
    else -> BatteryLevelLowTint
}

private fun logLevelColor(level: String): Color = when (level) {
    "TX" -> TermTx
    "RX" -> TermRx
    "SUCCESS" -> TermSuccess
    "ERROR" -> TermError
    "WARN" -> TermWarn
    else -> TermInfo
}

private data class StatusStyle(val label: String, val foreground: Color, val background: Color)

private fun statusStyleFor(state: ConnectionState): StatusStyle = when (state) {
    is ConnectionState.Connected -> StatusStyle("Connected", StatusSuccess, StatusSuccessTint)
    is ConnectionState.Connecting -> StatusStyle("Connecting…", StatusWarning, StatusWarningTint)
    is ConnectionState.Error -> StatusStyle("Connection issue", StatusDanger, StatusDangerTint)
    ConnectionState.Disconnected -> StatusStyle("Disconnected", RealmeTextGrey, StatusNeutralTint)
}

// ============================================================
// SCREEN
// ============================================================

@Composable
fun MainScreen(
    onItemClick: ((NavKey) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current

    // Collect UI state
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
    val activeAncMode by viewModel.activeAncMode.collectAsStateWithLifecycle()
    val isGameModeOn by viewModel.isGameModeOn.collectAsStateWithLifecycle()
    val batteryLeft by viewModel.batteryLeft.collectAsStateWithLifecycle()
    val batteryRight by viewModel.batteryRight.collectAsStateWithLifecycle()
    val batteryCase by viewModel.batteryCase.collectAsStateWithLifecycle()
    val logEntries by viewModel.logEntries.collectAsStateWithLifecycle()

    val showDeviceDialog by viewModel.showDeviceDialog.collectAsStateWithLifecycle()
    val showHelpDialog by viewModel.showHelpDialog.collectAsStateWithLifecycle()
    val showProtocolMonitor by viewModel.showProtocolMonitor.collectAsStateWithLifecycle()

    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val isCommandInFlight by viewModel.isCommandInFlight.collectAsStateWithLifecycle()

    val isConnected = connectionState is ConnectionState.Connected
    val deviceTitle = when (val state = connectionState) {
        is ConnectionState.Connected -> state.deviceName
        is ConnectionState.Connecting -> state.deviceName
        else -> selectedDevice?.name ?: "realme Buds T200x"
    }

    // Bluetooth Permissions handling
    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            viewModel.refreshDevices()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            viewModel.refreshDevices()
        }
    }

    // Page-level enter animation (fade + gentle rise).
    var pageVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { pageVisible = true }

    val scrollState = rememberScrollState()

    // Stable lambdas — no allocation on every recomposition.
    val onConnectClick: () -> Unit = remember(viewModel, permissionsGranted) {
        {
            if (!permissionsGranted) {
                permissionLauncher.launch(requiredPermissions)
            } else {
                viewModel.toggleConnection()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RealmePageBg)
    ) {
        AnimatedVisibility(
            visible = pageVisible,
            enter = fadeIn(tween(320)) + slideInVertically(tween(400)) { it / 10 },
            exit = fadeOut(tween(140))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = ScreenGutter, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Bar
                TopBar(
                    deviceName = deviceTitle,
                    connectionState = connectionState,
                    onBackClicked = { /* Optional Back */ },
                    onMenuClicked = { viewModel.openDeviceDialog() },
                    onToggleConnection = { viewModel.toggleConnection() }
                )

                // Connection errors are surfaced right beneath the status pill.
                AnimatedVisibility(
                    visible = lastError != null,
                    enter = fadeIn(tween(200)) + expandVertically(tween(260)),
                    exit = fadeOut(tween(140)) + shrinkVertically(tween(220))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ErrorBanner(
                            message = lastError.orEmpty(),
                            onDismiss = { viewModel.dismissError() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(CardGap))

                // Earbuds & Battery Hero Section
                EarbudsHero(
                    connectionState = connectionState,
                    deviceName = deviceTitle,
                    batteryLeft = batteryLeft,
                    batteryRight = batteryRight,
                    batteryCase = batteryCase,
                    isScanning = isScanning,
                    pairedDeviceCount = pairedDevices.size,
                    onConnectClick = onConnectClick,
                    onRefreshBattery = { viewModel.refreshBattery() }
                )

                Spacer(modifier = Modifier.height(CardGap))

                // Card 1: Noise Control Card
                NoiseControlCard(
                    activeMode = activeAncMode,
                    isConnected = isConnected,
                    isBusy = isCommandInFlight,
                    onModeSelected = { viewModel.setAncMode(it) },
                    onHelpClicked = { viewModel.openHelpDialog() }
                )

                Spacer(modifier = Modifier.height(CardGap))

                // Card 2: Other Card (Game Mode & Protocol Monitor)
                OtherCard(
                    gameMode = isGameModeOn == true,
                    isConnected = isConnected,
                    onGameModeToggle = { viewModel.setGameMode(it) },
                    onMonitorClick = { viewModel.openProtocolMonitor() }
                )

                Spacer(modifier = Modifier.height(26.dp))
            }
        }

        // Dialogs
        if (showDeviceDialog) {
            DeviceSelectorDialog(
                devices = pairedDevices,
                selectedDevice = selectedDevice,
                connectionState = connectionState,
                onSelectDevice = { viewModel.connectTo(it) },
                onDisconnect = { viewModel.toggleConnection() },
                onRefresh = { viewModel.refreshDevices() },
                onDismiss = { viewModel.closeDeviceDialog() }
            )
        }

        if (showHelpDialog) {
            NoiseControlHelpDialog(onDismiss = { viewModel.closeHelpDialog() })
        }

        if (showProtocolMonitor) {
            ProtocolMonitorDialog(
                logs = logEntries,
                onClear = { viewModel.clearLogs() },
                onDismiss = { viewModel.closeProtocolMonitor() }
            )
        }
    }
}

// ============================================================
// TOP BAR
// ============================================================

@Composable
fun TopBar(
    deviceName: String,
    connectionState: ConnectionState,
    onBackClicked: () -> Unit,
    onMenuClicked: () -> Unit,
    onToggleConnection: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val statusStyle = statusStyleFor(connectionState)
    val isConnected = connectionState is ConnectionState.Connected

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back icon (44dp hit target)
            TopBarButton(label = "Back", onClick = onBackClicked) {
                BackGlyph(color = RealmeTextDark)
            }

            // Title + live status pill
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = deviceName,
                    style = RealmeType.ScreenTitle,
                    color = RealmeTextDark,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(style = statusStyle)
            }

            // Overflow menu (44dp hit target)
            Box {
                TopBarButton(label = "More options", onClick = { menuExpanded = true }) {
                    MenuGlyph(color = RealmeTextDark)
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(RealmeCardWhite)
                ) {
                    DropdownMenuItem(
                        text = { Text("Select Device", color = RealmeTextDark, style = RealmeType.Body) },
                        onClick = {
                            menuExpanded = false
                            onMenuClicked()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isConnected) "Disconnect" else "Connect",
                                color = if (isConnected) StatusDanger else RealmeTextDark,
                                style = RealmeType.Body,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleConnection()
                        }
                    )
                }
            }
        }

        HorizontalDivider(color = RealmeDivider, thickness = 1.dp)
    }
}

/** 44dp circular icon button with an accessible label. */
@Composable
private fun TopBarButton(
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val description = label
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics { this.contentDescription = description }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun StatusPill(style: StatusStyle) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(style.background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(style.foreground)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = style.label,
            style = RealmeType.StatusPill,
            color = style.foreground,
            maxLines = 1
        )
    }
}

// ============================================================
// ERROR BANNER
// ============================================================

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StatusDangerTint)
            .border(1.dp, StatusDangerBorder, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WarningGlyph(color = StatusDanger, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Connection problem",
                style = RealmeType.StatusPill,
                color = StatusDanger,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = message,
                style = RealmeType.Caption,
                color = RealmeTextDark
            )
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .semantics { this.contentDescription = "Dismiss error" }
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            CloseGlyph(color = RealmeTextGrey, modifier = Modifier.size(16.dp))
        }
    }
}

// ============================================================
// EARBUDS & BATTERY HERO SECTION
// ============================================================

@Composable
fun EarbudsHero(
    connectionState: ConnectionState,
    deviceName: String,
    batteryLeft: Int?,
    batteryRight: Int?,
    batteryCase: Int?,
    isScanning: Boolean,
    pairedDeviceCount: Int,
    onConnectClick: () -> Unit,
    onRefreshBattery: () -> Unit
) {
    val heroMode = when {
        connectionState is ConnectionState.Connected -> HeroMode.CONNECTED
        connectionState is ConnectionState.Connecting -> HeroMode.CONNECTING
        isScanning -> HeroMode.SCANNING
        else -> HeroMode.DISCONNECTED
    }
    val heroBrush = remember {
        Brush.verticalGradient(colors = listOf(RealmeHeroGlowTop, RealmeHeroGlowBottom))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Product render on a soft rounded stage with a subtle radial-ish glow.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(CardShape)
                .background(heroBrush)
                .border(1.dp, RealmeCardBorder, CardShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(144.dp)
                    .clip(CircleShape)
                    .background(RealmeHeroCircle),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.realme_buds_case),
                    contentDescription = "realme Buds T200x charging case",
                    modifier = Modifier.size(128.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = heroMode,
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
            transitionSpec = {
                (fadeIn(tween(240)) + slideInVertically(tween(320)) { it / 12 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically(tween(220)) { -it / 12 })
            }
        ) { mode ->
            when (mode) {
                HeroMode.CONNECTED -> ConnectedBatteryBlock(
                    batteryLeft = batteryLeft,
                    batteryRight = batteryRight,
                    batteryCase = batteryCase,
                    onRefreshBattery = onRefreshBattery
                )

                HeroMode.CONNECTING -> ConnectBlock(
                    deviceName = deviceName,
                    subtitle = "Connecting to $deviceName…",
                    connecting = true,
                    onConnectClick = onConnectClick
                )

                HeroMode.SCANNING -> ConnectBlock(
                    deviceName = deviceName,
                    subtitle = "Scanning for paired devices…",
                    connecting = false,
                    onConnectClick = onConnectClick
                )

                HeroMode.DISCONNECTED -> ConnectBlock(
                    deviceName = deviceName,
                    subtitle = when {
                        pairedDeviceCount == 0 ->
                            "No paired devices found — pair your buds in Android settings"
                        connectionState is ConnectionState.Error ->
                            "Couldn't connect — check Bluetooth and try again"
                        else -> "Ready to connect"
                    },
                    connecting = false,
                    onConnectClick = onConnectClick
                )
            }
        }
    }
}

@Composable
private fun ConnectedBatteryBlock(
    batteryLeft: Int?,
    batteryRight: Int?,
    batteryCase: Int?,
    onRefreshBattery: () -> Unit
) {
    val caseLevel = batteryCase?.takeIf { it > 0 }
    val isReading = batteryLeft == null && batteryRight == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Battery",
                style = RealmeType.RowTitle,
                color = RealmeTextGrey,
                maxLines = 1
            )
            Spacer(modifier = Modifier.weight(1f))
            RefreshButton(onClick = onRefreshBattery)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryPill(label = "L", level = batteryLeft, glyph = BatteryGlyphType.BUD)
            BatteryPill(label = "R", level = batteryRight, glyph = BatteryGlyphType.BUD)
            BatteryPill(label = "Case", level = caseLevel, glyph = BatteryGlyphType.CASE)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isReading) {
            ReadingRow()
        }
    }
}

enum class BatteryGlyphType { BUD, CASE }

@Composable
fun BatteryPill(label: String, level: Int?, glyph: BatteryGlyphType) {
    val targetColor = batteryLevelColor(level)
    val targetTint = batteryLevelTint(level)
    val color by animateColorAsState(targetColor, tween(320), label = "batteryColor")
    val tint by animateColorAsState(targetTint, tween(320), label = "batteryTint")
    val display = if (level != null && level > 0) "$level%" else "--"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(tint)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (glyph) {
            BatteryGlyphType.BUD -> HeadphoneGlyph(color = color, modifier = Modifier.size(14.dp))
            BatteryGlyphType.CASE -> BatteryGlyph(color = color, filled = level != null && level > 0)
        }
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = RealmeType.Chip,
            color = RealmeTextGrey,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = display,
            style = RealmeType.MetricValue,
            color = color,
            maxLines = 1
        )
    }
}

/** Compact, gently pulsing "reading…" hint shown until the first battery report. */
@Composable
private fun ReadingRow() {
    val transition = rememberInfiniteTransition(label = "batteryReading")
    val readingAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "batteryReadingAlpha"
    )

    Row(
        modifier = Modifier.graphicsLayer { this.alpha = readingAlpha },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(RealmeTextLight)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Reading battery…",
            style = RealmeType.Caption,
            color = RealmeTextGrey,
            maxLines = 1
        )
    }
}

@Composable
private fun RefreshButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(160),
        label = "refreshScale"
    )

    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(RealmePillBg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RefreshGlyph(color = RealmeTextGrey, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Refresh",
            style = RealmeType.Chip,
            color = RealmeTextDark,
            maxLines = 1
        )
    }
}

/** Primary CTA used for the disconnected / scanning / connecting hero states. */
@Composable
private fun ConnectBlock(
    deviceName: String,
    subtitle: String,
    connecting: Boolean,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = deviceName,
            style = RealmeType.RowTitle,
            color = RealmeTextDark,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = RealmeType.Caption,
            color = RealmeTextGrey,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onConnectClick,
            enabled = !connecting,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = RealmeCircleActive,
                contentColor = RealmeIconActive,
                disabledContainerColor = RealmePillBg,
                disabledContentColor = RealmeTextGrey
            ),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 14.dp)
        ) {
            if (connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = RealmeTextGrey
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Connecting…", style = RealmeType.ButtonLabel)
            } else {
                Text(text = "Connect", style = RealmeType.ButtonLabel)
            }
        }
    }
}

// ============================================================
// NOISE CONTROL CARD
// ============================================================

@Composable
fun NoiseControlCard(
    activeMode: RealmeProtocol.AncMode?,
    isConnected: Boolean,
    isBusy: Boolean,
    onModeSelected: (RealmeProtocol.AncMode) -> Unit,
    onHelpClicked: () -> Unit
) {
    SectionCard {
        Column(modifier = Modifier.padding(CardPadding)) {
            // Header with optional busy hint + help icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Noise control",
                    style = RealmeType.SectionTitle,
                    color = RealmeTextDark
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedVisibility(
                        visible = isBusy,
                        enter = fadeIn(tween(180)) + expandHorizontally(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkHorizontally(tween(180))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 1.8.dp,
                                color = RealmeTextGrey
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Applying…",
                                style = RealmeType.Caption,
                                color = RealmeTextGrey,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .semantics { this.contentDescription = "About noise control" }
                            .clickable(onClick = onHelpClicked),
                        contentAlignment = Alignment.Center
                    ) {
                        InfoGlyph(color = RealmeTextGrey, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3 Circular Modes:
            // 1. Noise cancellation (0x08) 2. Off (0x01) 3. Transparency (0x02)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CircularModeButton(
                    title = "Noise\ncancellation",
                    iconType = IconType.NOISE_CANCELLATION,
                    isActive = activeMode == RealmeProtocol.AncMode.NOISE_CANCELLING,
                    enabled = isConnected,
                    onClick = { onModeSelected(RealmeProtocol.AncMode.NOISE_CANCELLING) }
                )

                CircularModeButton(
                    title = "Off",
                    iconType = IconType.OFF,
                    isActive = activeMode == RealmeProtocol.AncMode.NORMAL,
                    enabled = isConnected,
                    onClick = { onModeSelected(RealmeProtocol.AncMode.NORMAL) }
                )

                CircularModeButton(
                    title = "Transparency",
                    iconType = IconType.TRANSPARENCY,
                    isActive = activeMode == RealmeProtocol.AncMode.TRANSPARENCY,
                    enabled = isConnected,
                    onClick = { onModeSelected(RealmeProtocol.AncMode.TRANSPARENCY) }
                )
            }

            // State is never signalled by colour alone.
            AnimatedVisibility(
                visible = !isConnected,
                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Connect your earbuds to change noise control modes.",
                        style = RealmeType.Caption,
                        color = RealmeTextGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

enum class IconType {
    NOISE_CANCELLATION, OFF, TRANSPARENCY
}

@Composable
fun CircularModeButton(
    title: String,
    iconType: IconType,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = if (isActive) RealmeCircleActive else RealmeCircleInactive,
        animationSpec = tween(240),
        label = "modeBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isActive) RealmeIconActive else RealmeIconInactive,
        animationSpec = tween(240),
        label = "modeIcon"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) RealmeTextDark else RealmeTextGrey,
        animationSpec = tween(240),
        label = "modeText"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(260),
        label = "modeRing"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(240),
        label = "modeDim"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            pressed -> 0.92f
            isActive -> 1.04f
            else -> 1f
        },
        animationSpec = tween(if (pressed) 90 else 240),
        label = "modeScale"
    )

    val modeDescription = if (isActive) "$title, selected" else title

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            )
            .graphicsLayer { alpha = dimAlpha }
            .semantics(mergeDescendants = true) {
                this.contentDescription = modeDescription
            }
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Selected ring, animated in/out without any layout jump.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = ringAlpha }
                    .border(1.5.dp, RealmeCircleActive, CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                ModeIconCanvas(iconType = iconType, color = iconColor)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = RealmeType.Chip,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ModeIconCanvas(iconType: IconType, color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)

        when (iconType) {
            IconType.NOISE_CANCELLATION -> {
                // Head profile in center
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(cx, cy - 3.dp.toPx()))
                val path = Path().apply {
                    moveTo(cx - 7.dp.toPx(), cy + 9.dp.toPx())
                    quadraticTo(cx - 7.dp.toPx(), cy + 2.dp.toPx(), cx, cy + 2.dp.toPx())
                    quadraticTo(cx + 7.dp.toPx(), cy + 2.dp.toPx(), cx + 7.dp.toPx(), cy + 9.dp.toPx())
                }
                drawPath(path, color, style = stroke)

                // Cancellation concentric brackets / arcs on sides
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - 11.dp.toPx(), cy - 11.dp.toPx()),
                    size = Size(22.dp.toPx(), 22.dp.toPx()),
                    style = stroke
                )
                drawArc(
                    color = color,
                    startAngle = 315f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(cx - 11.dp.toPx(), cy - 11.dp.toPx()),
                    size = Size(22.dp.toPx(), 22.dp.toPx()),
                    style = stroke
                )
            }
            IconType.OFF -> {
                // Neutral head profile
                drawCircle(color = color, radius = 4.5.dp.toPx(), center = Offset(cx, cy - 3.dp.toPx()))
                val path = Path().apply {
                    moveTo(cx - 7.5.dp.toPx(), cy + 9.dp.toPx())
                    quadraticTo(cx - 7.5.dp.toPx(), cy + 2.dp.toPx(), cx, cy + 2.dp.toPx())
                    quadraticTo(cx + 7.5.dp.toPx(), cy + 2.dp.toPx(), cx + 7.5.dp.toPx(), cy + 9.dp.toPx())
                }
                drawPath(path, color, style = stroke)
            }
            IconType.TRANSPARENCY -> {
                // Head profile in center
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(cx, cy - 3.dp.toPx()))
                val path = Path().apply {
                    moveTo(cx - 7.dp.toPx(), cy + 9.dp.toPx())
                    quadraticTo(cx - 7.dp.toPx(), cy + 2.dp.toPx(), cx, cy + 2.dp.toPx())
                    quadraticTo(cx + 7.dp.toPx(), cy + 2.dp.toPx(), cx + 7.dp.toPx(), cy + 9.dp.toPx())
                }
                drawPath(path, color, style = stroke)

                // Radiating dots / ambient wave arcs on top
                drawArc(
                    color = color,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - 9.dp.toPx(), cy - 12.dp.toPx()),
                    size = Size(18.dp.toPx(), 18.dp.toPx()),
                    style = stroke
                )
            }
        }
    }
}

// ============================================================
// OTHER CARD (GAME MODE, MONITOR, ETC.)
// ============================================================

@Composable
fun OtherCard(
    gameMode: Boolean,
    isConnected: Boolean,
    onGameModeToggle: (Boolean) -> Unit,
    onMonitorClick: () -> Unit
) {
    val gameIconTint by animateColorAsState(
        targetValue = if (isConnected) RealmeTextDark else RealmeTextLight,
        animationSpec = tween(240),
        label = "gameIconTint"
    )

    SectionCard {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                text = "Other",
                style = RealmeType.SectionTitle,
                color = RealmeTextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Game mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    TileIcon {
                        GamePadGlyph(color = gameIconTint, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Game mode",
                            style = RealmeType.RowTitle,
                            color = RealmeTextDark
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Provides a seamless gaming experience with reduced latency and perfectly synchronized sound and visuals.",
                            style = RealmeType.Caption,
                            color = RealmeTextGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Switch(
                    checked = gameMode,
                    onCheckedChange = onGameModeToggle,
                    enabled = isConnected,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RealmeIconActive,
                        checkedTrackColor = RealmeSwitchActive,
                        uncheckedThumbColor = RealmeSwitchInactiveThumb,
                        uncheckedTrackColor = RealmeSwitchInactiveTrack
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = RealmeDivider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Protocol activity log / debug inspector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onMonitorClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TileIcon {
                    TerminalGlyph(color = RealmeTextDark, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protocol activity monitor",
                        style = RealmeType.RowTitle,
                        color = RealmeTextDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "View live Bluetooth TX/RX packet hex transmissions.",
                        style = RealmeType.Caption,
                        color = RealmeTextGrey
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ChevronGlyph(color = RealmeTextLight, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 42dp rounded tonal tile that hosts a leading glyph. */
@Composable
private fun TileIcon(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RealmeSurfaceSoft),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

// ============================================================
// SHARED CARD
// ============================================================

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = RealmeCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, RealmeCardBorder),
        content = content
    )
}

// ============================================================
// DRAWN GLYPHS (no icon-library dependency)
// ============================================================

@Composable
private fun BackGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val cy = size.height / 2f
        val tailX = size.width * 0.62f
        val headX = size.width * 0.30f
        drawLine(color, Offset(headX, cy), Offset(tailX, cy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(
            color,
            Offset(headX, cy),
            Offset(headX + size.width * 0.20f, cy - size.height * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(headX, cy),
            Offset(headX + size.width * 0.20f, cy + size.height * 0.18f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MenuGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val r = 2.dp.toPx()
        val cx = size.width / 2f
        drawCircle(color, radius = r, center = Offset(cx, size.height * 0.26f))
        drawCircle(color, radius = r, center = Offset(cx, size.height * 0.5f))
        drawCircle(color, radius = r, center = Offset(cx, size.height * 0.74f))
    }
}

@Composable
private fun CloseGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.8.dp.toPx()
        val a = size.minDimension * 0.28f
        val b = size.minDimension - a
        drawLine(color, Offset(a, a), Offset(b, b), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(b, a), Offset(a, b), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun ChevronGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height
        drawLine(
            color,
            Offset(w * 0.36f, h * 0.24f),
            Offset(w * 0.66f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(w * 0.66f, h * 0.5f),
            Offset(w * 0.36f, h * 0.76f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun WarningGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.12f)
            lineTo(size.width * 0.94f, size.height * 0.86f)
            lineTo(size.width * 0.06f, size.height * 0.86f)
            close()
        }
        drawPath(path, color, style = stroke)
        val cx = size.width * 0.5f
        drawLine(
            color,
            Offset(cx, size.height * 0.42f),
            Offset(cx, size.height * 0.64f),
            strokeWidth = 1.7.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color, radius = 1.1.dp.toPx(), center = Offset(cx, size.height * 0.75f))
    }
}

@Composable
private fun InfoGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.6.dp.toPx()
        drawCircle(color, radius = size.minDimension / 2f - stroke / 2f, style = Stroke(width = stroke))
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color, radius = 1.2.dp.toPx(), center = Offset(cx, cy - 3.dp.toPx()))
        drawLine(
            color,
            Offset(cx, cy - 0.4.dp.toPx()),
            Offset(cx, cy + 3.6.dp.toPx()),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun RefreshGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeW = 1.7.dp.toPx()
        val inset = 3.dp.toPx()
        val box = size.minDimension - inset * 2
        drawArc(
            color = color,
            startAngle = -50f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(box, box),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Arrow head at the leading end of the arc.
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = box / 2f
        val angleRad = Math.toRadians(-50.0)
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()
        val hx = cx + radius * cosA
        val hy = cy + radius * sinA
        val dirX = -sinA
        val dirY = cosA
        val normX = -dirY
        val normY = dirX
        val wing = 3.8.dp.toPx()
        drawLine(
            color,
            Offset(hx, hy),
            Offset(hx + dirX * wing * 0.9f + normX * wing * 0.7f, hy + dirY * wing * 0.9f + normY * wing * 0.7f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(hx, hy),
            Offset(hx + dirX * wing * 0.9f - normX * wing * 0.7f, hy + dirY * wing * 0.9f - normY * wing * 0.7f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun GamePadGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.7.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(1.dp.toPx(), h * 0.2f),
            size = Size(w - 2.dp.toPx(), h * 0.6f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
            style = Stroke(width = strokeW)
        )
        // D-pad
        val dcx = w * 0.33f
        val dcy = h * 0.5f
        val a = 3.2.dp.toPx()
        drawLine(color, Offset(dcx - a, dcy), Offset(dcx + a, dcy), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawLine(color, Offset(dcx, dcy - a), Offset(dcx, dcy + a), strokeWidth = strokeW, cap = StrokeCap.Round)
        // Buttons
        drawCircle(color, radius = 1.7.dp.toPx(), center = Offset(w * 0.66f, h * 0.5f))
        drawCircle(color, radius = 1.7.dp.toPx(), center = Offset(w * 0.77f, h * 0.38f))
    }
}

@Composable
private fun TerminalGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.6.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(1.dp.toPx(), 2.dp.toPx()),
            size = Size(w - 2.dp.toPx(), h - 4.dp.toPx()),
            cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
            style = Stroke(width = strokeW)
        )
        drawLine(
            color,
            Offset(w * 0.26f, h * 0.36f),
            Offset(w * 0.42f, h * 0.52f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(w * 0.42f, h * 0.52f),
            Offset(w * 0.26f, h * 0.68f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color,
            Offset(w * 0.54f, h * 0.68f),
            Offset(w * 0.76f, h * 0.68f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun HeadphoneGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val strokeW = 1.5.dp.toPx()
        // Headband (upper half arc)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(1.4.dp.toPx(), 3.4.dp.toPx()),
            size = Size(w - 2.8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Ear cups
        val cupW = 3.2.dp.toPx()
        val cupH = 5.dp.toPx()
        val cupY = 7.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(1.dp.toPx(), cupY),
            size = Size(cupW, cupH),
            cornerRadius = CornerRadius(1.4.dp.toPx(), 1.4.dp.toPx())
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w - 1.dp.toPx() - cupW, cupY),
            size = Size(cupW, cupH),
            cornerRadius = CornerRadius(1.4.dp.toPx(), 1.4.dp.toPx())
        )
    }
}

@Composable
private fun BatteryGlyph(color: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 11.dp)) {
        val strokeW = 1.3.dp.toPx()
        val capW = 2.dp.toPx()
        val gap = 1.6.dp.toPx()
        val bodyW = size.width - capW - gap
        val bodyH = size.height - strokeW
        val bodyTop = Offset(strokeW / 2f, strokeW / 2f)

        // Terminal cap
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyW + gap, size.height * 0.3f),
            size = Size(capW, size.height * 0.4f),
            cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
        )
        // Body outline
        drawRoundRect(
            color = color,
            topLeft = bodyTop,
            size = Size(bodyW, bodyH),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = strokeW)
        )
        // Fill
        if (filled) {
            val inset = 2.4.dp.toPx()
            drawRoundRect(
                color = color,
                topLeft = Offset(bodyTop.x + inset, bodyTop.y + inset),
                size = Size(bodyW - inset * 2, bodyH - inset * 2),
                cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
            )
        }
    }
}

// ============================================================
// DIALOGS & SHEETS
// ============================================================

@Composable
fun DeviceSelectorDialog(
    devices: List<BluetoothDeviceItem>,
    selectedDevice: BluetoothDeviceItem?,
    connectionState: ConnectionState,
    onSelectDevice: (BluetoothDeviceItem) -> Unit,
    onDisconnect: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paired Devices",
                    style = RealmeType.DialogTitle,
                    color = RealmeTextDark
                )
                TextButton(onClick = onRefresh) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RefreshGlyph(color = RealmeTextDark, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refresh",
                            color = RealmeTextDark,
                            style = RealmeType.Chip
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (devices.isEmpty()) {
                    Text(
                        text = "No paired Bluetooth devices found.\nPlease pair your realme Buds in Android Settings first.",
                        color = RealmeTextGrey,
                        style = RealmeType.Body
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(220.dp)) {
                        items(devices) { dev ->
                            val isSelected = dev.address == selectedDevice?.address
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) RealmePillBg else Color.Transparent)
                                    .clickable { onSelectDevice(dev) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dev.name,
                                        style = RealmeType.RowTitle,
                                        color = RealmeTextDark
                                    )
                                    Text(
                                        text = dev.address,
                                        style = RealmeType.Caption,
                                        color = RealmeTextGrey
                                    )
                                }
                                if (isSelected && connectionState is ConnectionState.Connected) {
                                    Text(
                                        text = "Connected",
                                        color = StatusSuccess,
                                        style = RealmeType.Chip
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (connectionState is ConnectionState.Connected) {
                TextButton(onClick = onDisconnect) {
                    Text("Disconnect", color = StatusDanger, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = RealmeTextDark, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = RealmeCardWhite,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun NoiseControlHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "About Noise Control",
                style = RealmeType.DialogTitle,
                color = RealmeTextDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "• Noise cancellation: Uses microphones to detect and actively cancel out ambient background noise (traffic, fans, air conditioning).",
                    style = RealmeType.Body,
                    color = RealmeTextDark
                )
                Text(
                    text = "• Off: Turns off active processing for standard passive isolation and maximum battery life.",
                    style = RealmeType.Body,
                    color = RealmeTextDark
                )
                Text(
                    text = "• Transparency: Passes external environmental audio and voices into your ears so you can talk and stay aware of your surroundings.",
                    style = RealmeType.Body,
                    color = RealmeTextDark
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = RealmeTextDark, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = RealmeCardWhite,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun ProtocolMonitorDialog(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    // Cap what we render (and only rebuild when the list identity changes).
    val visibleLogs = remember(logs) { logs.takeLast(MAX_RENDERED_LOG_LINES) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Protocol Monitor",
                    style = RealmeType.DialogTitle,
                    color = RealmeTextDark
                )
                TextButton(onClick = onClear) {
                    Text("Clear", color = StatusDanger, style = RealmeType.Chip)
                }
            }
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = TermBackground)
            ) {
                val listState = rememberLazyListState()

                LaunchedEffect(visibleLogs.size) {
                    if (visibleLogs.isNotEmpty()) {
                        listState.animateScrollToItem(visibleLogs.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    if (visibleLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No packets captured yet.\nConnect to earbuds to view real-time TX/RX frames.",
                                style = RealmeType.Terminal,
                                color = TermMuted
                            )
                        }
                    } else {
                        items(visibleLogs) { entry ->
                            Text(
                                text = "[${entry.timestamp}] [${entry.level}] ${entry.message}",
                                style = RealmeType.Terminal,
                                color = logLevelColor(entry.level)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = RealmeTextDark, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = RealmeCardWhite,
        shape = RoundedCornerShape(24.dp)
    )
}
