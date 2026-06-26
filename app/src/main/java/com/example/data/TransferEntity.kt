package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_history")
data class TransferEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val fileSize: Long,
    val isSender: Boolean,
    val peerDeviceName: String,
    val speedMbps: Double,
    val durationMs: Long,
    val status: String, // "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val encryptionKey: String, // ECDH Key Thumbprint
    val secureHash: String // SHA-256 for integrity check
)
