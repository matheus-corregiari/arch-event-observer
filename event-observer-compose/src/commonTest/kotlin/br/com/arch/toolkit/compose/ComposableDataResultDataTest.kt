@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("OPT_IN_USAGE")

package br.com.arch.toolkit.compose

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.test.PlatformTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlin.test.Test

class ComposableDataResultDataTest : PlatformTest() {
    init {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    private sealed interface ScreenState {
        val title: String

        data class Ready(override val title: String) : ScreenState
        data object Empty : ScreenState {
            override val title = "Empty"
        }
    }

    @Test
    fun `typed data callbacks receive partial data from an error result`() = scenario(
        result = DataResult<ScreenState>(
            ScreenState.Ready("Home"),
            IllegalStateException("offline"),
            DataResultStatus.ERROR
        ),
        config = {
            OnData(ScreenState.Ready::class) { data ->
                BasicText(data.copy(title = "Cached").title, Modifier.testTag("typedData"))
            }
            OnData(ScreenState::class) { data, status ->
                BasicText("${data.title}: $status", Modifier.testTag("typedStatus"))
            }
            OnData(ScreenState.Ready::class) { data, status, error ->
                BasicText(
                    "${data.title}: $status: ${error?.message}",
                    Modifier.testTag("typedError")
                )
            }
        },
        assert = {
            onNodeWithTag("typedData").assertTextEquals("Cached")
            onNodeWithTag("typedStatus").assertTextEquals("Home: ERROR")
            onNodeWithTag("typedError").assertTextEquals("Home: ERROR: offline")
        }
    )

    @Test
    fun `typed data passes a null throwable with loading data`() = scenario(
        result = DataResult<ScreenState>(ScreenState.Ready("Home"), null, DataResultStatus.LOADING),
        config = {
            OnData(ScreenState.Ready::class) { data, status, error ->
                BasicText("${data.title}: $status: $error", Modifier.testTag("typedLoading"))
            }
        },
        assert = {
            onNodeWithTag("typedLoading").assertTextEquals("Home: LOADING: null")
        }
    )

    @Test
    fun `typed data skips a different sealed variant`() = assertReadyIsHidden(
        DataResult(ScreenState.Empty, null, DataResultStatus.SUCCESS)
    )

    @Test
    fun `typed data skips an absent value`() = assertReadyIsHidden(
        DataResult(null, null, DataResultStatus.SUCCESS)
    )

    @Test
    fun `typed data skips the none status even with a matching value`() = assertReadyIsHidden(
        DataResult(ScreenState.Ready("Home"), null, DataResultStatus.NONE)
    )

    private fun assertReadyIsHidden(result: DataResult<ScreenState>) = scenario(
        result = result,
        config = {
            OnData(ScreenState.Ready::class) { data ->
                BasicText(data.title, Modifier.testTag("ready"))
            }
        },
        assert = {
            onNodeWithTag("ready").assertDoesNotExist()
        }
    )
}
