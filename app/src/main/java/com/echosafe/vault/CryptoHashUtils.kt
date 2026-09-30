package com.echosafe.vault

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

object CryptoHashUtils {

    fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun sha256(text: String): String {
        return sha256(text.trim().lowercase().toByteArray(Charsets.UTF_8))
    }

    fun sha256(file: File): String {
        if (!file.exists()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { inputStream: InputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun verifyIntegrity(file: File, expectedHash: String): Boolean {
        if (!file.exists()) return false
        val computedHash = sha256(file)
        return computedHash.equals(expectedHash, ignoreCase = true)
    }
}
