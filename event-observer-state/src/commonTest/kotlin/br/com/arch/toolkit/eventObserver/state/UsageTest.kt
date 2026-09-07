package br.com.arch.toolkit.eventObserver.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Compilable repository/ViewModel examples for the documented public API. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UsageTest {
    private class UsersModel(handle: SavedStateHandle, scope: CoroutineScope) : ViewModel(scope) {
        val users by handle.saveResponseState<List<Profile>>()
        val filter by handle.saveState<String>(default = "")
        val count = users.select { it?.size ?: 0 }

        fun refresh(repository: (String) -> Flow<DataResult<List<Profile>>>) = users.load {
            repository(filter.get().orEmpty())
        }

        fun changeFilter(value: String, repository: (String) -> Flow<DataResult<List<Profile>>>) {
            filter.set(value)
            refresh(repository)
        }
    }

    @Test
    fun refreshFiltersAndDisposalUseTheSamePublicApi() = runTest {
        val scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher(testScheduler))
        val model = UsersModel(SavedStateHandle(), scope)
        val store = ViewModelStore().apply { put("users", model) }
        try {
            val requests = mutableListOf<String>()
            val repository: (String) -> Flow<DataResult<List<Profile>>> = { filter ->
                requests += filter
                flowOf(dataResultSuccess(listOf(Profile(filter))))
            }
            model.refresh(repository).join()
            model.refresh(repository).join()
            model.changeFilter("active", repository)
            runCurrent()
            assertEquals(listOf("", "", "active"), requests)
            assertEquals(Profile("active"), model.users.get()!!.single())
            assertEquals(1, model.count.value)
            val job = model.users.load { flow { awaitCancellation() } }
            store.clear()
            assertTrue(job.isCancelled)
        } finally {
            store.clear()
        }
    }
}
