package com.wbk.notificationforwarder.utils

import java.security.MessageDigest

object HashUtils {
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Helper untuk membuat Bearer Token sesuai server kamu
    fun createSignature(apiId: String, apiKey: String): String {
        return "Bearer " + md5(apiId + apiKey)
    }
}