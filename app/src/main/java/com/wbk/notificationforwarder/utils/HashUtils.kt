package com.wbk.notificationforwarder.utils

import java.security.MessageDigest

object HashUtils {
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        // PERBAIKAN: Tambahkan 'it.toInt() and 0xff' agar byte negatif tidak merusak hash
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    // Helper untuk membuat Bearer Token
    fun createSignature(apiId: String, apiKey: String): String {
        return "Bearer " + md5(apiId + apiKey)
    }
}