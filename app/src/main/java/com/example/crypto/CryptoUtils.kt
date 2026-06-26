package com.example.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    // Generate ephemeral EC KeyPair for Diffie-Hellman
    fun generateECDHKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        return kpg.generateKeyPair()
    }

    // Derive AES-256 SecretKey from private key and peer's public key bytes
    fun deriveSharedKey(privateKey: PrivateKey, peerPublicKeyBytes: ByteArray): SecretKeySpec {
        val kf = KeyFactory.getInstance("EC")
        val peerPublicKeySpec = X509EncodedKeySpec(peerPublicKeyBytes)
        val peerPublicKey = kf.generatePublic(peerPublicKeySpec)

        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(peerPublicKey, true)
        val sharedSecret = ka.generateSecret()

        // Hash shared secret with SHA-256 to produce a stable 256-bit symmetric key
        val md = MessageDigest.getInstance("SHA-256")
        val aesKeyBytes = md.digest(sharedSecret)
        return SecretKeySpec(aesKeyBytes, "AES")
    }

    // Encrypt data with AES-256-GCM
    fun encryptAES_GCM(data: ByteArray, secretKey: SecretKeySpec): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(data)
        return Pair(ciphertext, iv)
    }

    // Decrypt data with AES-256-GCM
    fun decryptAES_GCM(ciphertext: ByteArray, iv: ByteArray, secretKey: SecretKeySpec): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(ciphertext)
    }

    // Get SHA-256 Hash of a ByteArray for integrity verification
    fun sha256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(data)
        return bytesToHex(hashBytes)
    }

    // Convert ByteArray to hex string
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    // Formatted fingerprint (e.g. 1A:2B:3C...)
    fun getFingerprint(publicKeyBytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(publicKeyBytes)
        return hash.take(8).joinToString(":") { String.format("%02X", it) }
    }
}
