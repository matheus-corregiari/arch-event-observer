@file:Suppress("Filename")

package br.com.arch.toolkit.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.result.EventDataStatus

@OptIn(ExperimentalTestApi::class)
fun <T> scenario(
    result: DataResult<T>,
    config: ObserveComposableWrapper<T>.() -> Unit,
    assert: ComposeUiTest.() -> Unit
) = runComposeUiTest {
    setContent {
        Column {
            result.composable
                .animation { enabled = false }
                .outsideComposable { /* See ObserveWrapper Tests */ }
                .Unwrap(owner = null, config = config)
        }
    }
    runOnIdle { assert.invoke(this) }
    waitForIdle()
}

val stringConfig: ObserveComposableWrapper<String>.() -> Unit = { createConfig<String, String>() }
val iterableConfig: ObserveComposableWrapper<Collection<String>>.() -> Unit =
    { createConfig<Collection<String>, String>() }
val mapConfig: ObserveComposableWrapper<Map<String, String>>.() -> Unit =
    { createConfig<Map<String, String>, Pair<String, String>>() }

@Suppress("LongMethod")
private fun <T, R> ObserveComposableWrapper<T>.createConfig() {
    // Data
    OnData { data ->
        BasicText("$data", modifier = Modifier.testTag("dataTag1"))
    }
    OnData { data, status ->
        BasicText("$data - $status", modifier = Modifier.testTag("dataTag2"))
    }
    OnData { data, status, error ->
        BasicText("$data - $status - ${error?.message}", modifier = Modifier.testTag("dataTag3"))
    }
    // ShowLoading
    OnShowLoading {
        BasicText("ShowLoading 1", modifier = Modifier.testTag("showLoadingTag1"))
    }
    OnShowLoading(EventDataStatus.WithData) {
        BasicText("ShowLoading 2", modifier = Modifier.testTag("showLoadingTag2"))
    }
    OnShowLoading(EventDataStatus.WithoutData) {
        BasicText("ShowLoading 3", modifier = Modifier.testTag("showLoadingTag3"))
    }
    // HideLoading
    OnHideLoading {
        BasicText("HideLoading 1", modifier = Modifier.testTag("hideLoadingTag1"))
    }
    OnHideLoading(EventDataStatus.WithData) {
        BasicText("HideLoading 2", modifier = Modifier.testTag("hideLoadingTag2"))
    }
    OnHideLoading(EventDataStatus.WithoutData) {
        BasicText("HideLoading 3", modifier = Modifier.testTag("hideLoadingTag3"))
    }
    // Error Without Throwable
    OnError { ->
        BasicText("Error 1", modifier = Modifier.testTag("errorTag1"))
    }
    OnError(EventDataStatus.WithData) { ->
        BasicText("Error 2", modifier = Modifier.testTag("errorTag2"))
    }
    OnError(EventDataStatus.WithoutData) { ->
        BasicText("Error 3", modifier = Modifier.testTag("errorTag3"))
    }
    // Error With Throwable
    OnError { throwable ->
        BasicText("Error 4 ${throwable.message}", modifier = Modifier.testTag("errorTag4"))
    }
    OnError(EventDataStatus.WithData) { throwable ->
        BasicText("Error 5 ${throwable.message}", modifier = Modifier.testTag("errorTag5"))
    }
    OnError(EventDataStatus.WithoutData) { throwable ->
        BasicText("Error 6 ${throwable.message}", modifier = Modifier.testTag("errorTag6"))
    }
    // Success
    OnSuccess {
        BasicText("Success 1", modifier = Modifier.testTag("successTag1"))
    }
    OnSuccess(EventDataStatus.WithData) {
        BasicText("Success 2", modifier = Modifier.testTag("successTag2"))
    }
    OnSuccess(EventDataStatus.WithoutData) {
        BasicText("Success 3", modifier = Modifier.testTag("successTag3"))
    }
    // Empty
    OnEmpty { ->
        BasicText("Empty", modifier = Modifier.testTag("emptyTag"))
    }
    // NotEmpty
    OnNotEmpty { data ->
        BasicText("$data", modifier = Modifier.testTag("notEmptyTag"))
    }
    // Single
    OnSingle<R> { data ->
        BasicText("$data", modifier = Modifier.testTag("singleTag"))
    }
    // Many
    OnMany { data ->
        BasicText("$data", modifier = Modifier.testTag("manyTag"))
    }
    // Result
    OnResult { data ->
        BasicText("$data", modifier = Modifier.testTag("resultTag"))
    }
    // Status
    OnStatus { status ->
        BasicText("$status", modifier = Modifier.testTag("statusTag"))
    }
    // None
    OnNone {
        BasicText("none", modifier = Modifier.testTag("noneTag"))
    }
}
