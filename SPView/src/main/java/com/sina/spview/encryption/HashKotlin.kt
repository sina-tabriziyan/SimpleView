/**
 * Created by ST on 5/3/2025.
 * Author: Sina Tabriziyan
 * @sina.tabriziyan@gmail.com
 */
package com.sina.spview.encryption

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object HashKotlin {

    // SHA-1
    fun sha1(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val bytes = md.digest(input.toByteArray())
            bytes.toHex()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    // SHA-256
    fun sha256(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.toHex()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    // MD5
    fun md5(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val bytes = md.digest(input.toByteArray())
            bytes.toHex()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            null
        }
    }

    // Extension function to convert byte array to hexadecimal string
    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}
