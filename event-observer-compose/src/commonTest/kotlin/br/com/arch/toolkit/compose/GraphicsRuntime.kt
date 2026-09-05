package br.com.arch.toolkit.compose

import kotlinx.coroutines.test.TestResult

/** Runs a UI test after the platform graphics runtime is ready. */
internal expect fun withGraphicsReady(block: () -> TestResult): TestResult
