@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("OPT_IN_USAGE")

package br.com.arch.toolkit.compose

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.test.PlatformTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlin.test.Test

class ComposableDataResultAnimationTest : PlatformTest() {
    init {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `default rendering works without animation`() = animationScenario(
        result = DataResult("Data", null, DataResultStatus.SUCCESS),
        config = {
            OnData { data ->
                BasicText("$data", modifier = Modifier.testTag("data"))
            }
        },
        assert = {
            onNodeWithTag("data").assertIsDisplayed()
        }
    )

    @Test
    fun `custom transitionSpec animation renders content`() = animationScenario(
        result = DataResult("Custom", null, DataResultStatus.SUCCESS),
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        config = {
            OnData { data ->
                BasicText("$data", modifier = Modifier.testTag("customData"))
            }
        },
        assert = {
            onNodeWithTag("customData").assertIsDisplayed()
        }
    )

    @Test
    fun `same key preserves remembered state while updating payload`() = withGraphicsReady {
        var result by mutableStateOf(DataResult("First", null, DataResultStatus.SUCCESS))
        var contentInstances = 0

        runComposeUiTest {
            setContent {
                DataResultContent(
                    result = result,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) {
                    OnData { data ->
                        val instance = remember { ++contentInstances }
                        BasicText("$data/$instance", modifier = Modifier.testTag("payload"))
                    }
                }
            }

            onNodeWithTag("payload").assertTextEquals("First/1")
            runOnIdle {
                result = DataResult("Second", null, DataResultStatus.SUCCESS)
            }
            waitForIdle()

            onNodeWithTag("payload").assertTextEquals("Second/1")
        }
    }

    @Test
    fun `collection shape change swaps visible structural content`() = withGraphicsReady {
        var result by mutableStateOf(
            DataResult<Collection<String>>(emptyList(), null, DataResultStatus.SUCCESS)
        )

        runComposeUiTest {
            setContent {
                DataResultContent(
                    result = result,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) {
                    OnEmpty { ->
                        BasicText("empty", modifier = Modifier.testTag("empty"))
                    }
                    OnSingle<String> {
                        BasicText(it, modifier = Modifier.testTag("single"))
                    }
                }
            }

            onNodeWithTag("empty").assertIsDisplayed()
            onNodeWithTag("single").assertDoesNotExist()

            runOnIdle {
                result = DataResult(listOf("One"), null, DataResultStatus.SUCCESS)
            }
            waitForIdle()

            onNodeWithTag("single").assertTextEquals("One")
            onNodeWithTag("empty").assertDoesNotExist()
        }
    }
}
