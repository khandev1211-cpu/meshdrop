package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.crypto.CryptoUtils
import com.example.data.TransferEntity
import com.example.ui.theme.BorderCyan
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CosmicCyan
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DeepSpaceNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlowWhite
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NebulaPurple
import com.example.ui.theme.SignalOrange
import com.example.ui.theme.SlateGray
import com.example.ui.theme.SpaceBlack
import com.example.viewmodel.ConnectionStatus
import com.example.viewmodel.MockFile
import com.example.viewmodel.P2pViewModel
import com.example.viewmodel.PeerDevice
import com.example.viewmodel.TransferRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            Toast.makeText(this, "MeshDrop Permissions Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "MeshDrop requires permissions to scan and connect offline", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check and request permissions
        checkAndRequestPermissions()

        setContent {
            MyApplicationTheme {
                MainAppScreen()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestPermissionLauncher.launch(ungranted.toTypedArray())
        }
    }
}

// Navigation Screens
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Handshake : Screen("handshake")
    object History : Screen("history")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    val viewModel: P2pViewModel = viewModel()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()

    val statusPadding = WindowInsets.statusBars.asPaddingValues()
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // High-tech rounded box logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = SpaceBlack,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MeshDrop",
                                fontWeight = FontWeight.Bold,
                                color = GlowWhite,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "SECURE P2P MESH",
                                fontWeight = FontWeight.ExtraBold,
                                color = CosmicCyan,
                                style = MaterialTheme.typography.labelSmall,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                },
                actions = {
                    // Quick Simulation Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "SIM",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSimulationMode) CosmicCyan else SlateGray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Switch(
                            checked = isSimulationMode,
                            onCheckedChange = { viewModel.setSimulationMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SpaceBlack,
                                checkedTrackColor = CosmicCyan,
                                uncheckedThumbColor = SlateGray,
                                uncheckedTrackColor = CardSlate
                            ),
                            modifier = Modifier.scale(0.85f).testTag("simulation_toggle")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBlack,
                    titleContentColor = GlowWhite
                ),
                modifier = Modifier.padding(top = statusPadding.calculateTopPadding())
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SpaceBlack,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(bottom = navPadding.calculateBottomPadding())
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.Dashboard,
                    onClick = { currentScreen = Screen.Dashboard },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == Screen.Dashboard) Icons.Filled.Wifi else Icons.Outlined.Wifi,
                            contentDescription = "Transfer"
                        )
                    },
                    label = { Text("Transfer") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpaceBlack,
                        selectedTextColor = CosmicCyan,
                        indicatorColor = CosmicCyan,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.Handshake,
                    onClick = { currentScreen = Screen.Handshake },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == Screen.Handshake) Icons.Filled.Security else Icons.Outlined.Lock,
                            contentDescription = "Crypto"
                        )
                    },
                    label = { Text("UnderTheHood") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpaceBlack,
                        selectedTextColor = CosmicCyan,
                        indicatorColor = CosmicCyan,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    ),
                    modifier = Modifier.testTag("nav_handshake")
                )

                NavigationBarItem(
                    selected = currentScreen == Screen.History,
                    onClick = { currentScreen = Screen.History },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == Screen.History) Icons.Filled.History else Icons.Outlined.History,
                            contentDescription = "History"
                        )
                    },
                    label = { Text("Logs") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SpaceBlack,
                        selectedTextColor = CosmicCyan,
                        indicatorColor = CosmicCyan,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    ),
                    modifier = Modifier.testTag("nav_history")
                )
            }
        },
        containerColor = SpaceBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel)
                Screen.Handshake -> HandshakeScreen(viewModel)
                Screen.History -> HistoryScreen(viewModel)
            }
        }
    }
}

// Extension to scale components easily
fun Modifier.scale(scale: Float) = this.then(
    Modifier.size((48 * scale).dp) // simplistic scaling modifier
)

// ---------------- DASHBOARD SCREEN ----------------

@Composable
fun DashboardScreen(viewModel: P2pViewModel) {
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val connectedPeer by viewModel.connectedPeer.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val role by viewModel.role.collectAsState()

    val transferProgress by viewModel.transferProgress.collectAsState()
    val transferSpeedMbps by viewModel.transferSpeedMbps.collectAsState()
    val bytesTransferred by viewModel.bytesTransferred.collectAsState()
    val timeRemainingSeconds by viewModel.timeRemainingSeconds.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(SpaceBlack),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Simulation alert
        if (isSimulationMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSlate),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CosmicCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Demo Simulation Mode: Active. Experience the full key handshake and chunk file streaming directly on this emulator.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlowWhite
                    )
                }
            }
        }

        // Active transfer view OR main dashboard views
        when (connectionStatus) {
            ConnectionStatus.HANDSHAKING,
            ConnectionStatus.SECURED,
            ConnectionStatus.TRANSFERRING,
            ConnectionStatus.COMPLETED -> {
                ActiveTransferProgressView(
                    viewModel = viewModel,
                    status = connectionStatus,
                    progress = transferProgress,
                    speed = transferSpeedMbps,
                    transferred = bytesTransferred,
                    total = selectedFile?.size ?: 0,
                    eta = timeRemainingSeconds,
                    role = role ?: TransferRole.SENDER,
                    fileName = selectedFile?.name ?: "Unknown"
                )
            }
            else -> {
                // Normal Dashboard Mode: Role Selection, Peer Discovery, File Selection
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // File Selection Header
                    item {
                        FileSelectionCard(
                            selectedFile = selectedFile,
                            viewModel = viewModel
                        )
                    }

                    // Role buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.startDiscovery()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("broadcast_send_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicCyan,
                                    contentColor = SpaceBlack
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = SpaceBlack)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SEND", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.startDiscovery()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("broadcast_receive_button"),
                                border = borderStroke(1.dp, BorderCyan),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = GlowWhite
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = GlowWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RECEIVE", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            }
                        }
                    }

                    // Display Error Message if any
                    errorMessage?.let { error ->
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Radar Scanner / Peer Search status
                    if (connectionStatus == ConnectionStatus.DISCOVERING) {
                        item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 24.dp)
                            ) {
                                Text(
                                    text = "SCANNING OFFLINE MESH...",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = CosmicCyan,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ScanningRadar(
                                    modifier = Modifier
                                        .size(200.dp)
                                        .testTag("scanning_radar")
                                )
                            }
                        }
                    }

                    // Peer Results
                    if (connectionStatus == ConnectionStatus.PEERS_FOUND || discoveredPeers.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DISCOVERED DIRECT PEERS (${discoveredPeers.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SlateGray,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.startDiscovery() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CosmicCyan)
                                }
                            }
                        }

                        items(discoveredPeers) { peer ->
                            PeerDeviceItem(
                                peer = peer,
                                onConnect = { role ->
                                    viewModel.connectToPeer(peer, role)
                                }
                            )
                        }
                    } else if (connectionStatus == ConnectionStatus.IDLE) {
                        item {
                            EmptyStateDashboard()
                        }
                    }
                }
            }
        }
    }
}

// Helper border creation
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@Composable
fun EmptyStateDashboard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Wifi,
            contentDescription = null,
            tint = BorderCyan,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Direct Link Established",
            color = GlowWhite,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "MeshDrop transmits files completely offline. Initiate a SEND or RECEIVE broadcast to search local peers via Wi-Fi Direct.",
            color = SlateGray,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun FileSelectionCard(selectedFile: MockFile?, viewModel: P2pViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SECURE FILE PAYLOAD",
                color = SlateGray,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            selectedFile?.let { file ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            color = GlowWhite,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${file.category} • ",
                                color = SlateGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = formatSize(file.size),
                                color = CosmicCyan,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.testTag("change_payload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Change payload",
                            tint = CosmicCyan
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = BorderCyan, modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = "SELECT REPOSITORY FILE:",
                        color = SlateGray,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    viewModel.mockFiles.forEach { file ->
                        val isSelected = file == selectedFile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) BorderCyan.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable {
                                    viewModel.selectFile(file)
                                    expanded = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = file.name,
                                color = if (isSelected) CosmicCyan else GlowWhite,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatSize(file.size),
                                color = SlateGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PeerDeviceItem(peer: PeerDevice, onConnect: (TransferRole) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CosmicCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = CosmicCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.name,
                    color = GlowWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${peer.macAddress} • SIGNAL: ${peer.signalStrength}/5",
                    color = SlateGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onConnect(TransferRole.SENDER) },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicCyan, contentColor = SpaceBlack),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("SEND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpaceBlack)
                }

                OutlinedButton(
                    onClick = { onConnect(TransferRole.RECEIVER) },
                    border = borderStroke(1.dp, BorderCyan),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlowWhite),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("RCV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlowWhite)
                }
            }
        }
    }
}

@Composable
fun ScanningRadar(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep"
    )
    val pulseRadius1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_1"
    )
    val pulseRadius2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_pulse_2"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2
        val maxRadius = minOf(width, height) / 2

        // concentric pulses
        drawCircle(
            color = CosmicCyan.copy(alpha = (1f - pulseRadius1) * 0.4f),
            radius = maxRadius * pulseRadius1,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = CosmicCyan.copy(alpha = (1f - pulseRadius2) * 0.4f),
            radius = maxRadius * pulseRadius2,
            center = Offset(centerX, centerY),
            style = Stroke(width = 2.dp.toPx())
        )

        // concentric static rings
        for (i in 1..4) {
            drawCircle(
                color = BorderCyan.copy(alpha = 0.35f),
                radius = maxRadius * (i / 4f),
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Radar Sweep Ray
        val sweepAngleRad = Math.toRadians(rotation.toDouble())
        val endX = centerX + maxRadius * Math.cos(sweepAngleRad).toFloat()
        val endY = centerY + maxRadius * Math.sin(sweepAngleRad).toFloat()

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(CosmicCyan, Color.Transparent),
                start = Offset(centerX, centerY),
                end = Offset(endX, endY)
            ),
            start = Offset(centerX, centerY),
            end = Offset(endX, endY),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Central Core Beacon
        drawCircle(
            color = CosmicCyan,
            radius = 6.dp.toPx(),
            center = Offset(centerX, centerY)
        )
        drawCircle(
            color = SpaceBlack,
            radius = 2.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

// ---------------- ACTIVE TRANSFER SCREEN ----------------

@Composable
fun ActiveTransferProgressView(
    viewModel: P2pViewModel,
    status: ConnectionStatus,
    progress: Float,
    speed: Double,
    transferred: Long,
    total: Long,
    eta: Long,
    role: TransferRole,
    fileName: String
) {
    // Dynamic statuses
    val statusText = when (status) {
        ConnectionStatus.HANDSHAKING -> "Negotiating ECDH Key Exchange..."
        ConnectionStatus.SECURED -> "Secure Symmetric AES-256-GCM Channel Initialized"
        ConnectionStatus.TRANSFERRING -> if (role == TransferRole.SENDER) "Encrypting & Transmitting..." else "Receiving & Decrypting..."
        ConnectionStatus.COMPLETED -> "Secure Transfer Completed!"
        else -> "Synchronizing network sockets..."
    }

    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.HANDSHAKING -> NebulaPurple
            ConnectionStatus.SECURED -> ElectricBlue
            ConnectionStatus.TRANSFERRING -> CosmicCyan
            ConnectionStatus.COMPLETED -> CyberGreen
            else -> SlateGray
        },
        label = "status_color"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (status == ConnectionStatus.COMPLETED) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.8.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // File Information Details
            Text(
                text = fileName,
                fontWeight = FontWeight.Bold,
                color = GlowWhite,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${if (role == TransferRole.SENDER) "Outbound Transfer" else "Inbound Transfer"} • ${formatSize(total)}",
                color = SlateGray,
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Glowing Progress Indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape),
                    color = statusColor,
                    trackColor = BorderCyan.copy(alpha = 0.4f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "PROGRESS", color = SlateGray, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = "${String.format("%.1f", progress * 100)}%",
                        color = GlowWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "SPEED", color = SlateGray, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = if (status == ConnectionStatus.TRANSFERRING) "$speed Mbps" else "--- Mbps",
                        color = CosmicCyan,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "TIME LEFT", color = SlateGray, style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = if (status == ConnectionStatus.TRANSFERRING) "${eta}s" else if (status == ConnectionStatus.COMPLETED) "Completed" else "---",
                        color = if (status == ConnectionStatus.COMPLETED) CyberGreen else GlowWhite,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Divider(color = BorderCyan)
            Spacer(modifier = Modifier.height(16.dp))

            // Encryption visualization streams
            ChunkStreamVisualizer(viewModel)

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Buttons
            if (status == ConnectionStatus.COMPLETED) {
                Button(
                    onClick = { viewModel.resetState() },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("done_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("DONE & DISCONNECT", fontWeight = FontWeight.Bold, color = SpaceBlack)
                }
            } else {
                OutlinedButton(
                    onClick = { viewModel.cancelActiveTransfer() },
                    border = borderStroke(1.dp, CrimsonRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("cancel_transfer_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ABORT SECURE LINK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChunkStreamVisualizer(viewModel: P2pViewModel) {
    val plaintextHex by viewModel.chunkPlaintextHex.collectAsState()
    val ciphertextHex by viewModel.chunkCiphertextHex.collectAsState()
    val chunkIV by viewModel.chunkIV.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val keyThumbprint by viewModel.cryptoFingerprint.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpaceBlack.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CosmicCyan,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "REAL-TIME CRYPTO STREAM",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "AES-256-GCM",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (status == ConnectionStatus.HANDSHAKING) {
            Text(
                text = ">>> WAITING FOR ECDH SYMMETRIC DERIVATION...",
                color = NebulaPurple,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            // Display IV, Plaintext, and Ciphertext
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "IV NOUNCE: ",
                        color = SlateGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (chunkIV.isNotBlank()) chunkIV else "------------------------",
                        color = CosmicCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PLAINTEXT: ",
                        color = SlateGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (plaintextHex.isNotBlank()) plaintextHex else "------------------------",
                        color = GlowWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CIPHERHEX: ",
                        color = SlateGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (ciphertextHex.isNotBlank()) ciphertextHex else "------------------------",
                        color = ElectricBlue,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "KEY THUMB: ",
                        color = SlateGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (keyThumbprint.isNotBlank()) keyThumbprint else "DERIVING KEYS...",
                        color = CyberGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- UNDER THE HOOD SCREEN ----------------

@Composable
fun HandshakeScreen(viewModel: P2pViewModel) {
    val ownPublicHex by viewModel.cryptoOwnPublicHex.collectAsState()
    val ownPrivateHex by viewModel.cryptoOwnPrivateHex.collectAsState()
    val peerPublicHex by viewModel.cryptoPeerPublicHex.collectAsState()
    val symmetricKeyHex by viewModel.cryptoSymmetricKeyHex.collectAsState()
    val fingerprint by viewModel.cryptoFingerprint.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(SpaceBlack),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "CRYPTOGRAPHIC PIPELINE",
                fontWeight = FontWeight.Bold,
                color = CosmicCyan,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "Every byte sent in MeshDrop is authenticated & encrypted. See the mathematical parameters currently protecting your network.",
                color = SlateGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Section 1: ECDH Ephemeral Exchange
        item {
            CryptoParameterBlock(
                title = "1. EPHEMERAL ECDH KEY PAIR",
                description = "Each transfer instantiates a temporary Elliptic Curve Diffie-Hellman pair. Private keys never touch the air.",
                elements = listOf(
                    "Own Private Key" to if (ownPrivateHex.isNotBlank()) ownPrivateHex else "IDLE / NOT GENERATED",
                    "Own Public Key" to if (ownPublicHex.isNotBlank()) ownPublicHex else "IDLE / NOT GENERATED",
                    "Peer Public Key" to if (peerPublicHex.isNotBlank()) peerPublicHex else "WAITING FOR P2P EXCHANGE"
                ),
                colorSchemeAccent = NebulaPurple
            )
        }

        // Section 2: Shared Symmetric Key
        item {
            CryptoParameterBlock(
                title = "2. SYMMETRIC AES-256 KEY",
                description = "Both nodes compute the mathematical intersection (shared secret) to derive the symmetric encryption key using SHA-256 hashing.",
                elements = listOf(
                    "Derived Symmetric Key" to if (symmetricKeyHex.isNotBlank()) symmetricKeyHex else "WAITING FOR ECDH AGREEMENT",
                    "Session Fingerprint" to if (fingerprint.isNotBlank()) fingerprint else "WAITING..."
                ),
                colorSchemeAccent = CosmicCyan
            )
        }

        // Section 3: Galois Counter Mode (GCM)
        item {
            CryptoParameterBlock(
                title = "3. AES-256-GCM PACKET FLOW",
                description = "Chunks are processed through Galois/Counter Mode. GCM adds an Authentication Tag to confirm data has not been modified in transmission.",
                elements = listOf(
                    "Cipher Protocol" to "AES-256-GCM (Low-Level Java Cryptography Architecture)",
                    "Smart Chunking Block" to "Buffered IO Stream (64KB adaptive frames)",
                    "Integrity Mechanism" to "SHA-256 end-to-end payload comparison"
                ),
                colorSchemeAccent = CyberGreen
            )
        }
    }
}

@Composable
fun CryptoParameterBlock(
    title: String,
    description: String,
    elements: List<Pair<String, String>>,
    colorSchemeAccent: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                color = colorSchemeAccent,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = description,
                color = SlateGray,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Divider(color = BorderCyan, modifier = Modifier.padding(bottom = 12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                elements.forEach { (label, value) ->
                    Column {
                        Text(
                            text = label.uppercase(),
                            color = colorSchemeAccent.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SpaceBlack)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = value,
                                color = GlowWhite,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TRANSFER LOG / HISTORY SCREEN ----------------

@Composable
fun HistoryScreen(viewModel: P2pViewModel) {
    val historyList by viewModel.transferHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(SpaceBlack),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TRANSMISSION HISTORY",
                    fontWeight = FontWeight.Bold,
                    color = CosmicCyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "A local secure log of physical and simulated peer interactions.",
                    color = SlateGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (historyList.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearDatabaseHistory() },
                    modifier = Modifier.testTag("wipe_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = CrimsonRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BorderCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "History Clear",
                        color = GlowWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete a secure direct file transfer to log entries.",
                        color = SlateGray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(historyList) { entry ->
                    HistoryItemCard(entry)
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(entry: TransferEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSlate),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Sender/Receiver tag
                val tagColor = if (entry.isSender) ElectricBlue else CosmicCyan
                val tagText = if (entry.isSender) "SENT" else "RCVD"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tagText,
                        color = tagColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = entry.fileName,
                    color = GlowWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Safe Padlock status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secured",
                        tint = CyberGreen,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "SECURED",
                        color = CyberGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = BorderCyan.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Logistics data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "PEER NODE", color = SlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = entry.peerDeviceName, color = GlowWhite, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(text = "SIZE", color = SlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = formatSize(entry.fileSize), color = GlowWhite, style = MaterialTheme.typography.bodySmall)
                }
                Column {
                    Text(text = "SPEED", color = SlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${entry.speedMbps} Mbps", color = CosmicCyan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "DATE & TIME", color = SlateGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatDate(entry.timestamp),
                        color = GlowWhite,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            // Encryption parameters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpaceBlack)
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VERIFIED SHA256: ",
                    color = SlateGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.secureHash,
                    color = SlateGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "KEY: ${entry.encryptionKey}",
                    color = NebulaPurple,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------------- UTILS & REUSABLE CONVERTERS ----------------

fun formatSize(sizeInBytes: Long): String {
    if (sizeInBytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(sizeInBytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", sizeInBytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
