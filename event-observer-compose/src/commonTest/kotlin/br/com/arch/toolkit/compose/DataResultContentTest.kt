@file:OptIn(ExperimentalTestApi::class)
@file:Suppress("OPT_IN_USAGE")

package br.com.arch.toolkit.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.DataResultStatus
import br.com.arch.toolkit.test.PlatformTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlin.test.Test

class DataResultContentTest : PlatformTest() {
    init {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    private fun <T> directContentScenario(
        result: DataResult<T>,
        config: DataResultContentScope<T>.() -> Unit,
        assert: ComposeUiTest.() -> Unit
    ) = withGraphicsReady {
        runComposeUiTest {
            setContent {
                Column {
                    DataResultContent(
                        result = result,
                        content = config
                    )
                }
            }
            runOnIdle { assert.invoke(this) }
            waitForIdle()
        }
    }

    private fun <T> extensionContentScenario(
        result: DataResult<T>,
        config: DataResultContentScope<T>.() -> Unit,
        assert: ComposeUiTest.() -> Unit
    ) = withGraphicsReady {
        runComposeUiTest {
            setContent {
                Column {
                    result.Content(
                        content = config
                    )
                }
            }
            runOnIdle { assert.invoke(this) }
            waitForIdle()
        }
    }

    @Test
    fun `DataResultContent renders SUCCESS state correctly`() = directContentScenario(
        result = DataResult("Hello Direct", null, DataResultStatus.SUCCESS),
        config = stringConfig,
        assert = {
            onNodeWithTag("dataTag1").assertTextEquals("Hello Direct")
            onNodeWithTag("successTag1").assertTextEquals("Success 1")
            onNodeWithTag("statusTag").assertTextEquals("SUCCESS")
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("showLoadingTag1").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResult extension Content renders SUCCESS state correctly`() = extensionContentScenario(
        result = DataResult("Hello Extension", null, DataResultStatus.SUCCESS),
        config = stringConfig,
        assert = {
            onNodeWithTag("dataTag1").assertTextEquals("Hello Extension")
            onNodeWithTag("successTag1").assertTextEquals("Success 1")
            onNodeWithTag("statusTag").assertTextEquals("SUCCESS")
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("showLoadingTag1").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResultContent renders LOADING state correctly`() = directContentScenario(
        result = DataResult(null, null, DataResultStatus.LOADING),
        config = stringConfig,
        assert = {
            onNodeWithTag("showLoadingTag1").assertTextEquals("ShowLoading 1")
            onNodeWithTag("statusTag").assertTextEquals("LOADING")
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("errorTag1").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResultContent renders ERROR state correctly`() = directContentScenario(
        result = DataResult(null, RuntimeException("Error occurred"), DataResultStatus.ERROR),
        config = stringConfig,
        assert = {
            onNodeWithTag("errorTag1").assertTextEquals("Error 1")
            onNodeWithTag("errorTag4").assertTextEquals("Error 4 Error occurred")
            onNodeWithTag("statusTag").assertTextEquals("ERROR")
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("showLoadingTag1").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResultContent renders collection EMPTY state correctly`() = directContentScenario(
        result = DataResult(emptyList<String>(), null, DataResultStatus.SUCCESS),
        config = iterableConfig,
        assert = {
            onNodeWithTag("emptyTag").assertTextEquals("Empty")
            onNodeWithTag("notEmptyTag").assertDoesNotExist()
            onNodeWithTag("singleTag").assertDoesNotExist()
            onNodeWithTag("manyTag").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResultContent renders collection SINGLE item correctly`() = directContentScenario(
        result = DataResult(listOf("Item 1"), null, DataResultStatus.SUCCESS),
        config = iterableConfig,
        assert = {
            onNodeWithTag("notEmptyTag").assertTextEquals("[Item 1]")
            onNodeWithTag("singleTag").assertTextEquals("Item 1")
            onNodeWithTag("manyTag").assertDoesNotExist()
            onNodeWithTag("emptyTag").assertDoesNotExist()
        }
    )

    @Test
    fun `DataResultContent renders collection MANY items correctly`() = directContentScenario(
        result = DataResult(listOf("Item 1", "Item 2"), null, DataResultStatus.SUCCESS),
        config = iterableConfig,
        assert = {
            onNodeWithTag("notEmptyTag").assertTextEquals("[Item 1, Item 2]")
            onNodeWithTag("manyTag").assertTextEquals("[Item 1, Item 2]")
            onNodeWithTag("singleTag").assertDoesNotExist()
            onNodeWithTag("emptyTag").assertDoesNotExist()
        }
    )

    @Test
    fun `Flow Content renders SUCCESS state correctly`() = withGraphicsReady {
        val flow = MutableStateFlow(DataResult("Hello Flow", null, DataResultStatus.SUCCESS))
        runComposeUiTest {
            setContent {
                Column {
                    flow.Content(owner = null, content = stringConfig)
                }
            }
            runOnIdle {
                onNodeWithTag("dataTag1").assertTextEquals("Hello Flow")
                onNodeWithTag("successTag1").assertTextEquals("Success 1")
            }
            waitForIdle()
        }
    }

    @Test
    fun `ComposableDataResult top level function renders SUCCESS state correctly`() =
        withGraphicsReady {
            val result = DataResult("Hello Function", null, DataResultStatus.SUCCESS)
            val flow = MutableStateFlow(result)
            runComposeUiTest {
                setContent {
                    Column {
                        ComposableDataResult(
                            flow = flow,
                            owner = null,
                            content = stringConfig
                        )
                    }
                }
                runOnIdle {
                    onNodeWithTag("dataTag1").assertTextEquals("Hello Function")
                    onNodeWithTag("successTag1").assertTextEquals("Success 1")
                }
                waitForIdle()
            }
        }

    @Test
    fun `visible observer keeps remembered state when an earlier observer disappears`() =
        withGraphicsReady {
            var result by mutableStateOf(DataResult("data", null, DataResultStatus.LOADING))
            var instances = 0
            val content: DataResultContentScope<String>.() -> Unit = {
                OnShowLoading { BasicText("loading", Modifier.testTag("loading")) }
                OnData { data ->
                    val instance = remember { ++instances }
                    BasicText("$data/$instance", Modifier.testTag("data"))
                }
            }
            runComposeUiTest {
                setContent { DataResultContent(result, content = content) }
                onNodeWithTag("loading").assertExists()
                onNodeWithTag("data").assertTextEquals("data/1")
                runOnIdle { result = DataResult("updated", null, DataResultStatus.SUCCESS) }
                waitForIdle()
                onNodeWithTag("loading").assertDoesNotExist()
                onNodeWithTag("data").assertTextEquals("updated/1")
            }
        }

    @Test
    fun `changed content lambda refreshes observer registrations`() = withGraphicsReady {
        var label by mutableStateOf("first")
        val result = DataResult("data", null, DataResultStatus.SUCCESS)
        runComposeUiTest {
            setContent {
                val capturedLabel = label
                DataResultContent(result) {
                    OnData { BasicText(capturedLabel, Modifier.testTag("label")) }
                }
            }
            onNodeWithTag("label").assertTextEquals("first")
            runOnIdle { label = "second" }
            waitForIdle()
            onNodeWithTag("label").assertTextEquals("second")
        }
    }
}
