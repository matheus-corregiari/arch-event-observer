@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("OPT_IN_USAGE")

package br.com.arch.toolkit.compose

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.text.BasicText
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
    fun `custom contentTransform animation renders content`() = animationScenario(
        result = DataResult("Custom", null, DataResultStatus.SUCCESS),
        animationConfig = {
            contentTransform = fadeIn() togetherWith fadeOut()
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
}
