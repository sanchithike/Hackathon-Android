package com.roposo.core.kotlinExtensions

/**
 * @author muddassir on 1/5/18.
 */
public fun Collection<Any>?.isNullOrEmpty(): Boolean = this == null || this.isEmpty()

public inline fun <T> doOn(vararg elements: T, block: T.() -> Unit) {
    elements.forEach(block)
}

public fun <T> Collection<T>.joinToStringForJava(separator: CharSequence = ", ", prefix: CharSequence = "", postfix: CharSequence = ""): String {
    return joinToString(separator, prefix, postfix)
}
