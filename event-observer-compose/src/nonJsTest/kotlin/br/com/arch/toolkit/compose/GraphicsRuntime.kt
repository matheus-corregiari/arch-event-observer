package br.com.arch.toolkit.compose

import kotlinx.coroutines.test.TestResult

internal actual fun withGraphicsReady(block: () -> TestResult): TestResult = block()
