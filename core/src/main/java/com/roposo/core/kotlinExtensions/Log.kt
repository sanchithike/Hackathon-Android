package com.roposo.core.kotlinExtensions

import android.util.Log
import com.roposo.core.BuildConfig

/**
 * @author Harsh on 1/29/18.
 */

interface ImprovedLog {
    interface NoLog
    interface LineNumberLog
    interface FunLog : LineNumberLog
}

@JvmOverloads
fun Any.logD(message: String, tag: String = getTag()) {
    if (canLog()) Log.d(tag, message)
}

@JvmOverloads
fun Any.logE(message: String, tag: String = getTag()) {
    if (canLog()) Log.e(tag, message)
}

@JvmOverloads
fun Any.logI(message: String, tag: String = getTag()) {
    if (canLog()) Log.i(tag, message)
}

@JvmOverloads
fun Any.logV(message: String, tag: String = getTag()) {
    if (canLog()) Log.v(tag, message)
}

@JvmOverloads
fun Any.logW(message: String, tag: String = getTag()) {
    if (canLog()) Log.w(tag, message)
}

@JvmOverloads
fun Any.logWtf(message: String, tag: String = getTag()) {
    if (canLog()) Log.wtf(tag, message)
}

private fun Any.getTag(): String {
    if (this !is ImprovedLog.LineNumberLog) {
        return this::class.java.simpleName
    }

    val stackTraceElement = Thread.currentThread().stackTrace.getOrNull(5)
    return if (stackTraceElement != null) {
        "(${stackTraceElement.fileName}:${stackTraceElement.lineNumber})" +
                if (this is ImprovedLog.FunLog) " ${stackTraceElement.methodName} ()" else ""
    } else {
        this::class.java.simpleName
    }
}

private fun Any.canLog() =
    BuildConfig.DEBUG && (this !is ImprovedLog.NoLog)

fun logE(tag: String, vararg args: String) {
    if (!BuildConfig.DEBUG) return
    Log.e(tag, args.joinToString())
}

fun log(tag: String, vararg args: String) {
    if (!BuildConfig.DEBUG) return
    Log.d(tag, args.joinToString())
}

fun asKeyValue(key: String, value: Any?): String = "{$key : $value}"