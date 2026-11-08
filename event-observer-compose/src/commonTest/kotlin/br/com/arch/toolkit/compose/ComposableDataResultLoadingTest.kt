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
import br.com.arch.toolkit.result.EventDataStatus
import br.com.arch.toolkit.test.PlatformTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlin.test.Test

class ComposableDataResultLoadingTest : PlatformTest() {
    init {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `loading renders original and matching data only for show observers`() =
        assertLoadingData(DataResultStatus.LOADING, visible = "show")

    @Test
    fun `success renders original and matching data only for hide observers`() =
        assertLoadingData(DataResultStatus.SUCCESS, visible = "hide")

    @Test
    fun `error renders original and matching data only for hide observers`() =
        assertLoadingData(DataResultStatus.ERROR, visible = "hide")

    @Test
    fun `none hides all loading data observers`() =
        assertLoadingData(DataResultStatus.NONE, visible = null)

    @Test
    fun `loading without data hides data observers`() =
        assertLoadingData(DataResultStatus.LOADING, visible = null, data = null)

    @Test
    fun `completed result without data hides data observers`() =
        assertLoadingData(DataResultStatus.SUCCESS, visible = null, data = null)

    @Suppress("LongMethod")
    private fun assertLoadingData(
        status: DataResultStatus,
        visible: String?,
        data: String? = "cached"
    ) = scenario(
        result = DataResult<Any>(data, null, status),
        config = {
            OnShowLoading { value -> BasicText(value.toString(), Modifier.testTag("showRaw")) }
            OnShowLoading(String::class) { value ->
                BasicText(value.uppercase(), Modifier.testTag("showTyped"))
            }
            OnShowLoading(Int::class) { _ -> BasicText("wrong", Modifier.testTag("showWrong")) }
            OnShowLoading(EventDataStatus.WithoutData) { value ->
                BasicText(value.toString(), Modifier.testTag("showFiltered"))
            }
            OnHideLoading { value -> BasicText(value.toString(), Modifier.testTag("hideRaw")) }
            OnHideLoading(String::class) { value ->
                BasicText(value.uppercase(), Modifier.testTag("hideTyped"))
            }
            OnHideLoading(Int::class) { _ -> BasicText("wrong", Modifier.testTag("hideWrong")) }
            OnHideLoading(String::class, EventDataStatus.WithoutData) { value ->
                BasicText(value, Modifier.testTag("hideFiltered"))
            }
        },
        assert = {
            for (observer in listOf("show", "hide")) {
                if (observer == visible) {
                    onNodeWithTag("${observer}Raw").assertTextEquals("cached")
                    onNodeWithTag("${observer}Typed").assertTextEquals("CACHED")
                } else {
                    onNodeWithTag("${observer}Raw").assertDoesNotExist()
                    onNodeWithTag("${observer}Typed").assertDoesNotExist()
                }
                onNodeWithTag("${observer}Wrong").assertDoesNotExist()
                onNodeWithTag("${observer}Filtered").assertDoesNotExist()
            }
        }
    )

    @Test
    fun `1 - String - LOADING - null`() = scenario(
        result = DataResult("Hello Compose", null, DataResultStatus.LOADING),
        config = stringConfig,
        assert = {
            // Data
            onNodeWithTag("dataTag1").assertTextEquals("Hello Compose")
            onNodeWithTag("dataTag2").assertTextEquals("Hello Compose - LOADING")
            onNodeWithTag("dataTag3").assertTextEquals("Hello Compose - LOADING - null")
            // Show Loading
            onNodeWithTag("showLoadingTag1").assertTextEquals("ShowLoading 1")
            onNodeWithTag("showLoadingTag2").assertTextEquals("ShowLoading 2")
            onNodeWithTag("showLoadingTag3").assertDoesNotExist()
            // Hide Loading
            onNodeWithTag("hideLoadingTag1").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag2").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag3").assertDoesNotExist()
            // Error Without Throwable
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("errorTag2").assertDoesNotExist()
            onNodeWithTag("errorTag3").assertDoesNotExist()
            // Error With Throwable
            onNodeWithTag("errorTag4").assertDoesNotExist()
            onNodeWithTag("errorTag5").assertDoesNotExist()
            onNodeWithTag("errorTag6").assertDoesNotExist()
            // Success
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("successTag2").assertDoesNotExist()
            onNodeWithTag("successTag3").assertDoesNotExist()
            // Empty
            onNodeWithTag("emptyTag").assertDoesNotExist()
            // NotEmpty
            onNodeWithTag("notEmptyTag").assertDoesNotExist()
            // Single
            onNodeWithTag("singleTag").assertDoesNotExist()
            // Many
            onNodeWithTag("manyTag").assertDoesNotExist()
            // Result
            onNodeWithTag("resultTag").assertTextEquals("Hello Compose")
            // Status
            onNodeWithTag("statusTag").assertTextEquals("LOADING")
            // None
            onNodeWithTag("noneTag").assertDoesNotExist()
        }
    )

    @Test
    fun `2 - null - LOADING - Throwable`() = scenario(
        result = DataResult(null, RuntimeException("fail"), DataResultStatus.LOADING),
        config = stringConfig,
        assert = {
            // Data
            onNodeWithTag("dataTag1").assertDoesNotExist()
            onNodeWithTag("dataTag2").assertDoesNotExist()
            onNodeWithTag("dataTag3").assertDoesNotExist()
            // Show Loading
            onNodeWithTag("showLoadingTag1").assertTextEquals("ShowLoading 1")
            onNodeWithTag("showLoadingTag2").assertDoesNotExist()
            onNodeWithTag("showLoadingTag3").assertTextEquals("ShowLoading 3")
            // Hide Loading
            onNodeWithTag("hideLoadingTag1").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag2").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag3").assertDoesNotExist()
            // Error Without Throwable
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("errorTag2").assertDoesNotExist()
            onNodeWithTag("errorTag3").assertDoesNotExist()
            // Error With Throwable
            onNodeWithTag("errorTag4").assertDoesNotExist()
            onNodeWithTag("errorTag5").assertDoesNotExist()
            onNodeWithTag("errorTag6").assertDoesNotExist()
            // Success
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("successTag2").assertDoesNotExist()
            onNodeWithTag("successTag3").assertDoesNotExist()
            // Empty
            onNodeWithTag("emptyTag").assertDoesNotExist()
            // NotEmpty
            onNodeWithTag("notEmptyTag").assertDoesNotExist()
            // Single
            onNodeWithTag("singleTag").assertDoesNotExist()
            // Many
            onNodeWithTag("manyTag").assertDoesNotExist()
            // Result
            onNodeWithTag("resultTag").assertTextEquals("null")
            // Status
            onNodeWithTag("statusTag").assertTextEquals("LOADING")
            // None
            onNodeWithTag("noneTag").assertDoesNotExist()
        }
    )

    @Test
    fun `3 - String - LOADING - Throwable`() = scenario(
        result = DataResult("Hello Compose", RuntimeException("fail"), DataResultStatus.LOADING),
        config = stringConfig,
        assert = {
            // Data
            onNodeWithTag("dataTag1").assertTextEquals("Hello Compose")
            onNodeWithTag("dataTag2").assertTextEquals("Hello Compose - LOADING")
            onNodeWithTag("dataTag3").assertTextEquals("Hello Compose - LOADING - fail")
            // Show Loading
            onNodeWithTag("showLoadingTag1").assertTextEquals("ShowLoading 1")
            onNodeWithTag("showLoadingTag2").assertTextEquals("ShowLoading 2")
            onNodeWithTag("showLoadingTag3").assertDoesNotExist()
            // Hide Loading
            onNodeWithTag("hideLoadingTag1").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag2").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag3").assertDoesNotExist()
            // Error Without Throwable
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("errorTag2").assertDoesNotExist()
            onNodeWithTag("errorTag3").assertDoesNotExist()
            // Error With Throwable
            onNodeWithTag("errorTag4").assertDoesNotExist()
            onNodeWithTag("errorTag5").assertDoesNotExist()
            onNodeWithTag("errorTag6").assertDoesNotExist()
            // Success
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("successTag2").assertDoesNotExist()
            onNodeWithTag("successTag3").assertDoesNotExist()
            // Empty
            onNodeWithTag("emptyTag").assertDoesNotExist()
            // NotEmpty
            onNodeWithTag("notEmptyTag").assertDoesNotExist()
            // Single
            onNodeWithTag("singleTag").assertDoesNotExist()
            // Many
            onNodeWithTag("manyTag").assertDoesNotExist()
            // Result
            onNodeWithTag("resultTag").assertTextEquals("Hello Compose")
            // Status
            onNodeWithTag("statusTag").assertTextEquals("LOADING")
            // None
            onNodeWithTag("noneTag").assertDoesNotExist()
        }
    )

    @Test
    fun `4 - null - LOADING - null`() = scenario(
        result = DataResult(null, null, DataResultStatus.LOADING),
        config = stringConfig,
        assert = {
            // Data
            onNodeWithTag("dataTag1").assertDoesNotExist()
            onNodeWithTag("dataTag2").assertDoesNotExist()
            onNodeWithTag("dataTag3").assertDoesNotExist()
            // Show Loading
            onNodeWithTag("showLoadingTag1").assertTextEquals("ShowLoading 1")
            onNodeWithTag("showLoadingTag2").assertDoesNotExist()
            onNodeWithTag("showLoadingTag3").assertTextEquals("ShowLoading 3")
            // Hide Loading
            onNodeWithTag("hideLoadingTag1").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag2").assertDoesNotExist()
            onNodeWithTag("hideLoadingTag3").assertDoesNotExist()
            // Error Without Throwable
            onNodeWithTag("errorTag1").assertDoesNotExist()
            onNodeWithTag("errorTag2").assertDoesNotExist()
            onNodeWithTag("errorTag3").assertDoesNotExist()
            // Error With Throwable
            onNodeWithTag("errorTag4").assertDoesNotExist()
            onNodeWithTag("errorTag5").assertDoesNotExist()
            onNodeWithTag("errorTag6").assertDoesNotExist()
            // Success
            onNodeWithTag("successTag1").assertDoesNotExist()
            onNodeWithTag("successTag2").assertDoesNotExist()
            onNodeWithTag("successTag3").assertDoesNotExist()
            // Empty
            onNodeWithTag("emptyTag").assertDoesNotExist()
            // NotEmpty
            onNodeWithTag("notEmptyTag").assertDoesNotExist()
            // Single
            onNodeWithTag("singleTag").assertDoesNotExist()
            // Many
            onNodeWithTag("manyTag").assertDoesNotExist()
            // Result
            onNodeWithTag("resultTag").assertTextEquals("null")
            // Status
            onNodeWithTag("statusTag").assertTextEquals("LOADING")
            // None
            onNodeWithTag("noneTag").assertDoesNotExist()
        }
    )
}
