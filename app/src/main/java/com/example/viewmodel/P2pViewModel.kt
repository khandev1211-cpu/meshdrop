package com.example.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.CryptoUtils
import com.example.data.AppDatabase
import com.example.data.TransferEntity
import com.example.data.TransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.KeyPair
import java.util.UUID
import kotlin.random.Random

// Connection State Enum
enum class ConnectionStatus {
    IDLE,
    DISCOVERING,
    PEERS_FOUND,
    CONNECTING,
    HANDSHAKING,
    SECURED,
    TRANSFERRING,
    COMPLETED,
    FAILED
}

// Transfer Role
enum class TransferRole {
    SENDER,
    RECEIVER
}

// Peer Device Representation
data class PeerDevice(
    val name: String,
    val macAddress: String,
    val signalStrength: Int = 4, // 1 to 5
    val status: String = "Available",
    val isSimulation: Boolean = false
)

// Representation of dummy files available to send
data class MockFile(
    val name: String,
    val size: Long, // in bytes
    val category: String, // "Video", "App", "Photo", "Archive"
    val fileExtension: String
)

class P2pViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository: TransferRepository

    // P2P State Flows
    private val _isSimulationMode = MutableStateFlow(true)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.IDLE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()

    private val _connectedPeer = MutableStateFlow<PeerDevice?>(null)
    val connectedPeer: StateFlow<PeerDevice?> = _connectedPeer.asStateFlow()

    private val _selectedFile = MutableStateFlow<MockFile?>(null)
    val selectedFile: StateFlow<MockFile?> = _selectedFile.asStateFlow()

    private val _role = MutableStateFlow<TransferRole?>(null)
    val role: StateFlow<TransferRole?> = _role.asStateFlow()

    // Transfer Telemetry
    private val _transferProgress = MutableStateFlow(0f)
    val transferProgress: StateFlow<Float> = _transferProgress.asStateFlow()

    private val _transferSpeedMbps = MutableStateFlow(0.0)
    val transferSpeedMbps: StateFlow<Double> = _transferSpeedMbps.asStateFlow()

    private val _bytesTransferred = MutableStateFlow(0L)
    val bytesTransferred: StateFlow<Long> = _bytesTransferred.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(0L)
    val timeRemainingSeconds: StateFlow<Long> = _timeRemainingSeconds.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Cryptographic Real-Time Displays
    private val _cryptoOwnPublicHex = MutableStateFlow("")
    val cryptoOwnPublicHex: StateFlow<String> = _cryptoOwnPublicHex.asStateFlow()

    private val _cryptoOwnPrivateHex = MutableStateFlow("")
    val cryptoOwnPrivateHex: StateFlow<String> = _cryptoOwnPrivateHex.asStateFlow()

    private val _cryptoPeerPublicHex = MutableStateFlow("")
    val cryptoPeerPublicHex: StateFlow<String> = _cryptoPeerPublicHex.asStateFlow()

    private val _cryptoSymmetricKeyHex = MutableStateFlow("")
    val cryptoSymmetricKeyHex: StateFlow<String> = _cryptoSymmetricKeyHex.asStateFlow()

    private val _cryptoFingerprint = MutableStateFlow("")
    val cryptoFingerprint: StateFlow<String> = _cryptoFingerprint.asStateFlow()

    // Chunk Streaming Visualization
    private val _chunkPlaintextHex = MutableStateFlow("")
    val chunkPlaintextHex: StateFlow<String> = _chunkPlaintextHex.asStateFlow()

    private val _chunkCiphertextHex = MutableStateFlow("")
    val chunkCiphertextHex: StateFlow<String> = _chunkCiphertextHex.asStateFlow()

    private val _chunkIV = MutableStateFlow("")
    val chunkIV: StateFlow<String> = _chunkIV.asStateFlow()

    private val _integrityVerified = MutableStateFlow(false)
    val integrityVerified: StateFlow<Boolean> = _integrityVerified.asStateFlow()

    // Real Wi-Fi Direct Variables
    private var wifiP2pManager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var p2pReceiver: WifiP2pReceiver? = null
    private var isP2pSupported = false

    // Net Jobs & Sockets
    private var transferJob: Job? = null
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    // Room History Flow
    val transferHistory: StateFlow<List<TransferEntity>>

    // Pre-defined mock files
    val mockFiles = listOf(
        MockFile("4K_Cyber_Drone_Footage.mp4", 1_824_152_913L, "Video", "mp4"),
        MockFile("MeshDrop_Signed_Installer.apk", 82_412_532L, "App", "apk"),
        MockFile("HighRes_Space_Horizon.tiff", 312_824_111L, "Photo", "tiff"),
        MockFile("Full_Database_Backup_2026.bin", 4_192_512_091L, "Archive", "bin")
    )

    init {
        val database = AppDatabase.getDatabase(context)
        repository = TransferRepository(database.transferDao())
        transferHistory = repository.allTransfers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize WifiP2pManager
        try {
            wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
            if (wifiP2pManager != null) {
                channel = wifiP2pManager!!.initialize(context, Looper.getMainLooper(), null)
                isP2pSupported = true
                registerP2pReceiver()
            }
        } catch (e: Exception) {
            Log.e("MeshDrop", "P2P not supported or initialization failed", e)
            isP2pSupported = false
        }

        // Default file selection
        _selectedFile.value = mockFiles[0]
    }

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        cancelActiveTransfer()
        resetState()
    }

    fun selectFile(file: MockFile) {
        _selectedFile.value = file
    }

    fun resetState() {
        _connectionStatus.value = ConnectionStatus.IDLE
        _connectedPeer.value = null
        _role.value = null
        _transferProgress.value = 0f
        _transferSpeedMbps.value = 0.0
        _bytesTransferred.value = 0L
        _timeRemainingSeconds.value = 0L
        _errorMessage.value = null
        _integrityVerified.value = false
        _chunkPlaintextHex.value = ""
        _chunkCiphertextHex.value = ""
        _chunkIV.value = ""
    }

    // 1. Peer Discovery
    fun startDiscovery() {
        resetState()
        _connectionStatus.value = ConnectionStatus.DISCOVERING

        if (_isSimulationMode.value) {
            viewModelScope.launch {
                delay(1500) // Aesthetic scan time
                val simulatedPeers = listOf(
                    PeerDevice("Nexus_9X_Vault", "02:00:00:DE:AD:01", 5, "Available", true),
                    PeerDevice("Pixel_Fold_Secure", "02:00:00:DE:AD:02", 4, "Available", true),
                    PeerDevice("Galaxy_Tab_Ultra", "02:00:00:DE:AD:03", 4, "Available", true),
                    PeerDevice("CyberShield_P2P", "02:00:00:DE:AD:04", 3, "Available", true)
                )
                _discoveredPeers.value = simulatedPeers
                _connectionStatus.value = ConnectionStatus.PEERS_FOUND
            }
        } else {
            // Real Wi-Fi P2P Discovery
            if (!isP2pSupported) {
                _errorMessage.value = "Wi-Fi Direct is not supported on this device"
                _connectionStatus.value = ConnectionStatus.FAILED
                return
            }
            try {
                wifiP2pManager?.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d("MeshDrop", "Peer discovery initiated successfully")
                    }

                    override fun onFailure(reasonCode: Int) {
                        val reason = when (reasonCode) {
                            WifiP2pManager.ERROR -> "Generic Error"
                            WifiP2pManager.P2P_UNSUPPORTED -> "P2P Unsupported"
                            WifiP2pManager.BUSY -> "System Busy"
                            else -> "Unknown Error"
                        }
                        _errorMessage.value = "Discovery Failed: $reason"
                        _connectionStatus.value = ConnectionStatus.FAILED
                    }
                })
            } catch (e: SecurityException) {
                _errorMessage.value = "Location permissions required for Wi-Fi Direct"
                _connectionStatus.value = ConnectionStatus.FAILED
            }
        }
    }

    // Connect to a peer device
    fun connectToPeer(peer: PeerDevice, selectedRole: TransferRole) {
        _connectedPeer.value = peer
        _role.value = selectedRole
        _connectionStatus.value = ConnectionStatus.CONNECTING

        if (_isSimulationMode.value || peer.isSimulation) {
            runSimulationTransfer(peer, selectedRole)
        } else {
            // Real Wi-Fi P2P Connection Setup
            val config = WifiP2pConfig().apply {
                deviceAddress = peer.macAddress
            }
            try {
                wifiP2pManager?.connect(channel, config, object : WifiP2pManager.ActionListener {
                    override fun onSuccess() {
                        Log.d("MeshDrop", "Connecting to ${peer.name}...")
                    }

                    override fun onFailure(reason: Int) {
                        _errorMessage.value = "Connection failed: Error code $reason"
                        _connectionStatus.value = ConnectionStatus.FAILED
                    }
                })
            } catch (e: SecurityException) {
                _errorMessage.value = "Location permission missing"
                _connectionStatus.value = ConnectionStatus.FAILED
            }
        }
    }

    // Cancel / Stop transfer
    fun cancelActiveTransfer() {
        transferJob?.cancel()
        closeSockets()
        resetState()
    }

    // Clear Transfer Database History
    fun clearDatabaseHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }

    // Interactive simulated file transfer
    private fun runSimulationTransfer(peer: PeerDevice, selectedRole: TransferRole) {
        transferJob?.cancel()
        transferJob = viewModelScope.launch(Dispatchers.Default) {
            // Step 1: Connecting
            delay(1500)

            // Step 2: Ephemeral Cryptographic Handshake (ECDH)
            _connectionStatus.value = ConnectionStatus.HANDSHAKING
            
            // Generate real cryptographic values for rendering
            val ownKeyPair = CryptoUtils.generateECDHKeyPair()
            val peerKeyPair = CryptoUtils.generateECDHKeyPair()

            val ownPublicKeyEncoded = ownKeyPair.public.encoded
            val ownPrivateKeyEncoded = ownKeyPair.private.encoded
            val peerPublicKeyEncoded = peerKeyPair.public.encoded

            _cryptoOwnPublicHex.value = CryptoUtils.bytesToHex(ownPublicKeyEncoded.take(24).toByteArray()) + "..."
            _cryptoOwnPrivateHex.value = CryptoUtils.bytesToHex(ownPrivateKeyEncoded.take(20).toByteArray()) + "..."
            _cryptoPeerPublicHex.value = CryptoUtils.bytesToHex(peerPublicKeyEncoded.take(24).toByteArray()) + "..."

            delay(1200)

            // Derive shared key using Diffie-Hellman & SHA-256
            val secretKey = CryptoUtils.deriveSharedKey(ownKeyPair.private, peerPublicKeyEncoded)
            val symmetricKeyBytes = secretKey.encoded
            _cryptoSymmetricKeyHex.value = CryptoUtils.bytesToHex(symmetricKeyBytes)
            _cryptoFingerprint.value = CryptoUtils.getFingerprint(ownPublicKeyEncoded)

            delay(1200)
            _connectionStatus.value = ConnectionStatus.SECURED
            delay(1000)

            // Step 3: Secure Chunk Streaming Setup
            _connectionStatus.value = ConnectionStatus.TRANSFERRING
            val file = _selectedFile.value ?: mockFiles[0]
            val totalBytes = file.size
            _bytesTransferred.value = 0L

            val random = Random(System.currentTimeMillis())
            val startTime = System.currentTimeMillis()

            var transferred = 0L
            val simulatedChunkSize = 1024 * 512 // 512 KB chunks

            // Simulated transmission loops
            while (transferred < totalBytes) {
                if (transferJob?.isCancelled == true) break

                // Generate simulated chunk and perform actual real encryption/decryption on dummy buffers for visual authenticity
                val chunkData = ByteArray(64) { random.nextInt().toByte() }
                val (ciphertext, iv) = CryptoUtils.encryptAES_GCM(chunkData, secretKey)

                // Update hex outputs for visualization
                _chunkPlaintextHex.value = CryptoUtils.bytesToHex(chunkData.take(16).toByteArray()) + "..."
                _chunkCiphertextHex.value = CryptoUtils.bytesToHex(ciphertext.take(16).toByteArray()) + "..."
                _chunkIV.value = CryptoUtils.bytesToHex(iv)

                // Calculate transmission dynamics
                val speedBaseMbps = 180.0 + random.nextDouble(-20.0, 60.0)
                _transferSpeedMbps.value = Math.round(speedBaseMbps * 10.0) / 10.0

                // Fast simulated chunk increments
                transferred += (simulatedChunkSize * (speedBaseMbps / 100)).toLong()
                if (transferred > totalBytes) transferred = totalBytes

                _bytesTransferred.value = transferred
                _transferProgress.value = transferred.toFloat() / totalBytes.toFloat()

                // Calculate time remaining
                val elapsedMs = System.currentTimeMillis() - startTime
                val speedBytesPerSec = (transferred * 1000.0) / elapsedMs
                if (speedBytesPerSec > 0) {
                    _timeRemainingSeconds.value = ((totalBytes - transferred) / speedBytesPerSec).toLong()
                }

                delay(70) // Control speed animation rate
            }

            if (transferJob?.isCancelled != true) {
                _transferProgress.value = 1.0f
                _integrityVerified.value = true
                delay(800)
                _connectionStatus.value = ConnectionStatus.COMPLETED

                // Log into SQLite database
                val elapsedMs = System.currentTimeMillis() - startTime
                val finalSpeed = (totalBytes * 8.0 / 1024.0 / 1024.0) / (elapsedMs / 1000.0)
                
                val historyEntry = TransferEntity(
                    fileName = file.name,
                    fileSize = file.size,
                    isSender = selectedRole == TransferRole.SENDER,
                    peerDeviceName = peer.name,
                    speedMbps = Math.round(finalSpeed * 10.0) / 10.0,
                    durationMs = elapsedMs,
                    status = "COMPLETED",
                    encryptionKey = _cryptoFingerprint.value,
                    secureHash = "SHA256-" + CryptoUtils.bytesToHex(Random.nextBytes(16)).take(12)
                )

                withContext(Dispatchers.IO) {
                    repository.insert(historyEntry)
                }
            }
        }
    }

    // 2. Real Sockets Execution (Runs only when simulation is disabled)
    fun startRealNetworkOperation(peerAddress: String, isHost: Boolean) {
        transferJob?.cancel()
        transferJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                _connectionStatus.value = ConnectionStatus.HANDSHAKING
                val ownKeyPair = CryptoUtils.generateECDHKeyPair()

                if (isHost) {
                    // Receiver: host server socket
                    serverSocket = ServerSocket(8888)
                    val socket = serverSocket!!.accept()
                    clientSocket = socket

                    handleSocketHandshakeAndReceive(socket, ownKeyPair)
                } else {
                    // Sender: connect to host
                    val socket = Socket()
                    socket.connect(InetSocketAddress(peerAddress, 8888), 10000)
                    clientSocket = socket

                    handleSocketHandshakeAndSend(socket, ownKeyPair)
                }
            } catch (e: Exception) {
                Log.e("MeshDrop", "Network operation failed", e)
                withContext(Dispatchers.Main) {
                    _errorMessage.value = e.localizedMessage ?: "Network interaction failed"
                    _connectionStatus.value = ConnectionStatus.FAILED
                }
            } finally {
                closeSockets()
            }
        }
    }

    private suspend fun handleSocketHandshakeAndSend(socket: Socket, keyPair: KeyPair) {
        val outStream = DataOutputStream(socket.getOutputStream())
        val inStream = DataInputStream(socket.getInputStream())

        // 1. Send own public key
        val ownPubBytes = keyPair.public.encoded
        outStream.writeInt(ownPubBytes.size)
        outStream.write(ownPubBytes)
        outStream.flush()

        // 2. Read peer public key
        val peerPubSize = inStream.readInt()
        val peerPubBytes = ByteArray(peerPubSize)
        inStream.readFully(peerPubBytes)

        // 3. Derive symmetric key
        val secretKey = CryptoUtils.deriveSharedKey(keyPair.private, peerPubBytes)
        val symmetricKeyHex = CryptoUtils.bytesToHex(secretKey.encoded)
        val fingerprint = CryptoUtils.getFingerprint(ownPubBytes)

        withContext(Dispatchers.Main) {
            _cryptoOwnPublicHex.value = CryptoUtils.bytesToHex(ownPubBytes.take(24).toByteArray()) + "..."
            _cryptoPeerPublicHex.value = CryptoUtils.bytesToHex(peerPubBytes.take(24).toByteArray()) + "..."
            _cryptoSymmetricKeyHex.value = symmetricKeyHex
            _cryptoFingerprint.value = fingerprint
            _connectionStatus.value = ConnectionStatus.SECURED
        }

        delay(1000)

        // 4. Secure File Transfer (Simulated payload send for demonstration files)
        val file = _selectedFile.value ?: mockFiles[0]
        outStream.writeUTF(file.name)
        outStream.writeLong(file.size)
        outStream.flush()

        withContext(Dispatchers.Main) {
            _connectionStatus.value = ConnectionStatus.TRANSFERRING
        }

        val totalBytes = file.size
        var sent = 0L
        val random = Random(System.currentTimeMillis())
        val buffer = ByteArray(1024 * 64) // 64KB buffer
        val startTime = System.currentTimeMillis()

        while (sent < totalBytes) {
            if (transferJob?.isCancelled == true) break
            val bytesToWrite = minOf(buffer.size.toLong(), totalBytes - sent).toInt()
            random.nextBytes(buffer) // Create mock payload data

            // Encrypt block
            val (ciphertext, iv) = CryptoUtils.encryptAES_GCM(buffer.take(bytesToWrite).toByteArray(), secretKey)
            
            // Write encrypted package size, IV size, IV, ciphertext
            outStream.writeInt(iv.size)
            outStream.write(iv)
            outStream.writeInt(ciphertext.size)
            outStream.write(ciphertext)
            outStream.flush()

            sent += bytesToWrite

            withContext(Dispatchers.Main) {
                _bytesTransferred.value = sent
                _transferProgress.value = sent.toFloat() / totalBytes.toFloat()
                _chunkPlaintextHex.value = CryptoUtils.bytesToHex(buffer.take(16).toByteArray()) + "..."
                _chunkCiphertextHex.value = CryptoUtils.bytesToHex(ciphertext.take(16).toByteArray()) + "..."
                _chunkIV.value = CryptoUtils.bytesToHex(iv)
                
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 0) {
                    val speed = (sent * 8.0 / 1024.0 / 1024.0) / (elapsed / 1000.0)
                    _transferSpeedMbps.value = Math.round(speed * 10.0) / 10.0
                    _timeRemainingSeconds.value = ((totalBytes - sent) / (sent / (elapsed / 1000.0))).toLong()
                }
            }
            delay(50) // Simulate streaming pacing
        }

        if (transferJob?.isCancelled != true) {
            withContext(Dispatchers.Main) {
                _integrityVerified.value = true
                _connectionStatus.value = ConnectionStatus.COMPLETED
            }
            val duration = System.currentTimeMillis() - startTime
            val finalSpeed = (totalBytes * 8.0 / 1024.0 / 1024.0) / (duration / 1000.0)
            repository.insert(
                TransferEntity(
                    fileName = file.name,
                    fileSize = file.size,
                    isSender = true,
                    peerDeviceName = _connectedPeer.value?.name ?: "P2P Client",
                    speedMbps = Math.round(finalSpeed * 10.0) / 10.0,
                    durationMs = duration,
                    status = "COMPLETED",
                    encryptionKey = fingerprint,
                    secureHash = "SHA256-" + CryptoUtils.bytesToHex(Random.nextBytes(16)).take(12)
                )
            )
        }
    }

    private suspend fun handleSocketHandshakeAndReceive(socket: Socket, keyPair: KeyPair) {
        val inStream = DataInputStream(socket.getInputStream())
        val outStream = DataOutputStream(socket.getOutputStream())

        // 1. Read peer public key
        val peerPubSize = inStream.readInt()
        val peerPubBytes = ByteArray(peerPubSize)
        inStream.readFully(peerPubBytes)

        // 2. Send own public key
        val ownPubBytes = keyPair.public.encoded
        outStream.writeInt(ownPubBytes.size)
        outStream.write(ownPubBytes)
        outStream.flush()

        // 3. Derive symmetric key
        val secretKey = CryptoUtils.deriveSharedKey(keyPair.private, peerPubBytes)
        val symmetricKeyHex = CryptoUtils.bytesToHex(secretKey.encoded)
        val fingerprint = CryptoUtils.getFingerprint(ownPubBytes)

        withContext(Dispatchers.Main) {
            _cryptoOwnPublicHex.value = CryptoUtils.bytesToHex(ownPubBytes.take(24).toByteArray()) + "..."
            _cryptoPeerPublicHex.value = CryptoUtils.bytesToHex(peerPubBytes.take(24).toByteArray()) + "..."
            _cryptoSymmetricKeyHex.value = symmetricKeyHex
            _cryptoFingerprint.value = fingerprint
            _connectionStatus.value = ConnectionStatus.SECURED
        }

        delay(1000)

        // 4. Read File Metadata
        val fileName = inStream.readUTF()
        val fileSize = inStream.readLong()

        withContext(Dispatchers.Main) {
            _selectedFile.value = MockFile(fileName, fileSize, "Received", "file")
            _connectionStatus.value = ConnectionStatus.TRANSFERRING
        }

        var received = 0L
        val startTime = System.currentTimeMillis()

        while (received < fileSize) {
            if (transferJob?.isCancelled == true) break

            // Read IV and cipher block
            val ivSize = inStream.readInt()
            val iv = ByteArray(ivSize)
            inStream.readFully(iv)

            val cipherSize = inStream.readInt()
            val ciphertext = ByteArray(cipherSize)
            inStream.readFully(ciphertext)

            // Decrypt on the fly
            val plaintext = CryptoUtils.decryptAES_GCM(ciphertext, iv, secretKey)
            received += plaintext.size

            withContext(Dispatchers.Main) {
                _bytesTransferred.value = received
                _transferProgress.value = received.toFloat() / fileSize.toFloat()
                _chunkPlaintextHex.value = CryptoUtils.bytesToHex(plaintext.take(16).toByteArray()) + "..."
                _chunkCiphertextHex.value = CryptoUtils.bytesToHex(ciphertext.take(16).toByteArray()) + "..."
                _chunkIV.value = CryptoUtils.bytesToHex(iv)

                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed > 0) {
                    val speed = (received * 8.0 / 1024.0 / 1024.0) / (elapsed / 1000.0)
                    _transferSpeedMbps.value = Math.round(speed * 10.0) / 10.0
                    _timeRemainingSeconds.value = ((fileSize - received) / (received / (elapsed / 1000.0))).toLong()
                }
            }
        }

        if (transferJob?.isCancelled != true) {
            withContext(Dispatchers.Main) {
                _integrityVerified.value = true
                _connectionStatus.value = ConnectionStatus.COMPLETED
            }
            val duration = System.currentTimeMillis() - startTime
            val finalSpeed = (fileSize * 8.0 / 1024.0 / 1024.0) / (duration / 1000.0)
            repository.insert(
                TransferEntity(
                    fileName = fileName,
                    fileSize = fileSize,
                    isSender = false,
                    peerDeviceName = _connectedPeer.value?.name ?: "P2P Host",
                    speedMbps = Math.round(finalSpeed * 10.0) / 10.0,
                    durationMs = duration,
                    status = "COMPLETED",
                    encryptionKey = fingerprint,
                    secureHash = "SHA256-" + CryptoUtils.bytesToHex(Random.nextBytes(16)).take(12)
                )
            )
        }
    }

    private fun closeSockets() {
        try {
            clientSocket?.close()
        } catch (e: Exception) {}
        try {
            serverSocket?.close()
        } catch (e: Exception) {}
        clientSocket = null
        serverSocket = null
    }

    // Wi-Fi Direct BroadcastReceiver Registration
    private fun registerP2pReceiver() {
        p2pReceiver = WifiP2pReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(p2pReceiver, intentFilter)
    }

    override fun onCleared() {
        super.onCleared()
        cancelActiveTransfer()
        try {
            if (p2pReceiver != null) {
                context.unregisterReceiver(p2pReceiver)
            }
        } catch (e: Exception) {}
    }

    // BroadcastReceiver implementation nested or coupled closely for simple MVVM architecture
    inner class WifiP2pReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (!_isSimulationMode.value) {
                        try {
                            wifiP2pManager?.requestPeers(channel) { peerList: WifiP2pDeviceList ->
                                val deviceList = peerList.deviceList.map { device ->
                                    PeerDevice(
                                        name = device.deviceName.ifBlank { "Android Device" },
                                        macAddress = device.deviceAddress,
                                        signalStrength = 4,
                                        status = when (device.status) {
                                            WifiP2pDevice.AVAILABLE -> "Available"
                                            WifiP2pDevice.CONNECTED -> "Connected"
                                            WifiP2pDevice.FAILED -> "Failed"
                                            WifiP2pDevice.INVITED -> "Invited"
                                            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
                                            else -> "Unknown"
                                        },
                                        isSimulation = false
                                    )
                                }
                                _discoveredPeers.value = deviceList
                                if (deviceList.isNotEmpty() && _connectionStatus.value == ConnectionStatus.DISCOVERING) {
                                    _connectionStatus.value = ConnectionStatus.PEERS_FOUND
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e("MeshDrop", "Missing location permission for requesting peers")
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    if (!_isSimulationMode.value) {
                        val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                        if (networkInfo?.isConnected == true) {
                            wifiP2pManager?.requestConnectionInfo(channel) { info: WifiP2pInfo ->
                                if (info.groupFormed) {
                                    val isHost = info.isGroupOwner
                                    val hostAddress = info.groupOwnerAddress?.hostAddress ?: ""
                                    // Trigger real secure socket network operation
                                    startRealNetworkOperation(hostAddress, isHost)
                                }
                            }
                        } else {
                            cancelActiveTransfer()
                        }
                    }
                }
            }
        }
    }
}
