package br.com.arch.toolkit.statebenchmark

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Debug
import android.os.Parcel
import android.os.Trace
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.arch.toolkit.eventObserver.state.ViewModelState
import br.com.arch.toolkit.result.DataResult
import br.com.arch.toolkit.statebenchmark.baseline.ViewModelState as BaselineState
import br.com.arch.toolkit.util.dataResultLoading
import br.com.arch.toolkit.util.dataResultSuccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Both implementations use the same UI, dependencies, repository fixtures and device process. */
class BenchmarkModel(handle: SavedStateHandle, baseline: Boolean, private val size: Int) : ViewModel() {
    private val serializer = ListSerializer(Int.serializer())
    private val current = if (baseline) null else ViewModelState.Result("rows", serializer, handle, viewModelScope)
    private val previous = if (baseline) BaselineState.Result("rows", serializer, handle, viewModelScope) else null
    val results: StateFlow<DataResult<List<Int>>> = current?.flow() ?: previous!!.flow()
    var sequence = 0
        private set

    fun update(merge: Boolean = false): Job {
        val count = ++sequence
        val source = suspend {
            flow {
                emit(dataResultLoading<List<Int>>())
                delay(10)
                emit(dataResultSuccess(List(if (merge) (size / 10).coerceAtLeast(1) else size) { it + count }))
            }
        }
        val reducer: suspend (List<Int>?, List<Int>) -> List<Int> = { old, next ->
            if (merge) (old.orEmpty() + next).takeLast(size) else next
        }
        return current?.loadReducing(reducer, source) ?: previous!!.loadReducing(reducer, source)
    }
}

class BenchmarkActivity : ComponentActivity() {
    private lateinit var model: BenchmarkModel
    private lateinit var stateLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        Trace.beginSection("StateRestore")
        try {
            super.onCreate(savedInstanceState)
            Log.i("StateBenchmark", "restoredActivity=${savedInstanceState != null}")
            val factory = viewModelFactory {
                initializer {
                    BenchmarkModel(createSavedStateHandle(), intent.getBooleanExtra("baseline", false), intent.getIntExtra("size", 100))
                }
            }
            model = ViewModelProvider.create(this, factory, defaultViewModelCreationExtras)[BenchmarkModel::class]
            // Initialize the restored holder inside the measured section.
            model.results.value
        } finally {
            Trace.endSection()
        }
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ViewCompat.setOnApplyWindowInsetsListener(container) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        stateLabel = TextView(this).apply { contentDescription = "StateStatus" }
        container.addView(stateLabel)
        fun button(label: String, action: () -> Unit) {
            container.addView(Button(this).apply { text = label; contentDescription = label; setOnClickListener { action() } })
        }
        fun update(merge: Boolean) {
            val allocated = Debug.getRuntimeStat("art.gc.bytes-allocated").toLongOrNull() ?: 0L
            lifecycleScope.launch {
                model.update(merge).join()
                val total = Debug.getRuntimeStat("art.gc.bytes-allocated").toLongOrNull() ?: 0L
                Log.i("StateBenchmark", "allocatedBytes=${total - allocated}; rows=${model.results.value.data?.size}")
            }
        }
        button("Refresh") { update(false) }
        button("Merge") { update(true) }
        button("Rotate") {
            requestedOrientation = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        val list = ListView(this).apply { contentDescription = "Rows" }
        container.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(container)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.results.collect { result ->
                    Trace.beginSection("StateRender")
                    try {
                        list.adapter = ArrayAdapter(this@BenchmarkActivity, android.R.layout.simple_list_item_1, result.data.orEmpty())
                        stateLabel.text = "${result.status}; rows=${result.data?.size ?: 0}; sequence=${model.sequence}"
                    } finally {
                        Trace.endSection()
                    }
                }
            }
        }
        if (model.results.value.data == null) update(false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Trace.beginSection("StateSave")
        try {
            super.onSaveInstanceState(outState)
            val parcel = Parcel.obtain()
            try {
                parcel.writeBundle(outState)
                Log.i("StateBenchmark", "savedBundleBytes=${parcel.dataSize()}")
            } finally {
                parcel.recycle()
            }
        } finally {
            Trace.endSection()
        }
    }
}
