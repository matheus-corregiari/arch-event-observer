@file:Suppress("Filename")

package br.com.arch.toolkit.util

/** Returns this pair only when both values are non-null. */
fun <T, R> Pair<T?, R?>.onlyWithValues(): Pair<T, R>? =
    runCatching { requireNotNull(first) to requireNotNull(second) }.getOrNull()
