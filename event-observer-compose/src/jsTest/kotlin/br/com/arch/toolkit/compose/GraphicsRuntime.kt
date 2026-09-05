package br.com.arch.toolkit.compose

import kotlinx.coroutines.test.TestResult
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko
import kotlin.js.unsafeCast

@OptIn(InternalSkikoApi::class)
internal actual fun withGraphicsReady(block: () -> TestResult): TestResult =
    // JavaScript flattens the Promise returned by the test callback.
    awaitSkiko.then { block() }.unsafeCast<TestResult>()
