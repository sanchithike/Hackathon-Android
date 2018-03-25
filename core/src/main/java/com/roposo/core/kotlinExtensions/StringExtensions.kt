package com.roposo.core.kotlinExtensions

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * @author muddassir on 1/5/18.
 */

public fun String.getSha1Hash():String {
    var id: String
    try {
        val digest = MessageDigest.getInstance("SHA-1").digest(toByteArray())
        val builder = StringBuilder()
        digest.forEach {
            builder.append(String.format("%02x", it))
        }
        id = builder.toString()
    } catch (e: NoSuchAlgorithmException) {
        id = UUID.randomUUID().toString()
        e.printStackTrace()
    }
    return id;
}
