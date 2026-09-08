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
    private class PlainModel(handle: SavedStateHandle, scope: CoroutineScope) : ViewModel(scope) {
        val users by handle.saveState<List<Profile>>(default = emptyList())
        val count = users.select { it?.size ?: 0 }
        val screen by handle.saveState<Screen>()
        val names = screen.select { it?.users.orEmpty().map(Profile::name) }
        val total = screen.select { it?.counts?.get("total") ?: 0 }

        fun connect(source: Flow<List<Profile>>) = users.bind(source)
        fun replace(users: List<Profile>) = this.users.set(users)
        fun connectNames(source: Flow<List<String>>) = screen.bindMapped(source) { names ->
            Screen(names.map { Profile(it) }, mapOf("total" to names.size))
        }
    }

    @Test
    fun plainReadmeExampleSupportsDirectValuesAndRepositoryFlows() = runTest {
        val model = PlainModel(SavedStateHandle(), backgroundScope)
        assertEquals(emptyList(), model.users.get())
        model.replace(listOf(Profile("Ada")))
        assertEquals(1, model.count.value)
        model.connect(flowOf(listOf(Profile("Lin"), Profile("Grace")))).join()
        assertEquals(2, model.count.value)
        assertEquals(listOf("Lin", "Grace"), model.users.get()?.map(Profile::name))
    }

    @Test
    fun oneMappedResponseRestoresMultipleDerivedStates() = runTest {
        val handle = SavedStateHandle()
        val model = PlainModel(handle, backgroundScope)
        model.connectNames(flowOf(listOf("Ada", "Lin"))).join()
        assertEquals(listOf("Ada", "Lin"), model.names.value)
        assertEquals(2, model.total.value)

        val restoredHandle = SavedStateHandle(mapOf("screen" to handle.get<String>("screen")))
        val restored = PlainModel(restoredHandle, backgroundScope)
        assertEquals(model.names.value, restored.names.value)
        assertEquals(model.total.value, restored.total.value)
    }

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
