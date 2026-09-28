@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("OPT_IN_USAGE")

package br.com.arch.toolkit.compose

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
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
    fun `default animation renders content`() = animationScenario(
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
    fun `same animation key updates payload content without keeping stale UI`() = withGraphicsReady {
        var result by mutableStateOf(DataResult("First", null, DataResultStatus.SUCCESS))

        runComposeUiTest {
            setContent {
                DataResultContent(
                    result = result,
                    transitionSpec = { fadeIn() togetherWith fadeOut() }
                ) {
                    OnData { data ->
                        BasicText(data, modifier = Modifier.testTag("payload"))
                    }
                }
            }

            onNodeWithTag("payload").assertTextEquals("First")
            runOnIdle {
                result = DataResult("Second", null, DataResultStatus.SUCCESS)
            }
            waitForIdle()

            onNodeWithTag("payload").assertTextEquals("Second")
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
                    OnEmpty {
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
