package br.com.arch.toolkit.statebenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.TraceSectionMetric
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(androidx.benchmark.macro.ExperimentalMetricApi::class)
@RunWith(Parameterized::class)
class StateBench(private val baseline: Boolean, private val size: Int) {
    @get:Rule
    val benchmark = MacrobenchmarkRule()

    @Test
    fun frames() = benchmark.measureRepeated(
        packageName = PACKAGE,
        metrics = listOf(
            FrameTimingMetric(),
            TraceSectionMetric("StateSave", TraceSectionMetric.Mode.Sum),
            TraceSectionMetric("StateRestore", TraceSectionMetric.Mode.Sum)
        ),
        compilationMode = CompilationMode.Full(),
        iterations = InstrumentationRegistry.getArguments().getString("stateBenchmarkIterations")?.toInt() ?: 10,
        setupBlock = {
            pressHome()
            device.executeShellCommand("am force-stop $PACKAGE")
            startActivityAndWait(Intent().apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(PACKAGE, "$PACKAGE.BenchmarkActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("baseline", baseline)
                putExtra("size", size)
            })
            check(device.wait(Until.hasObject(By.desc("StateStatus").textStartsWith("SUCCESS; rows=$size")), 10_000))
        }
    ) {
        for (action in listOf("Refresh", "Merge")) {
            val oldStatus = device.findObject(By.desc("StateStatus")).text
            device.findObject(By.desc(action)).click()
            check(device.wait(Until.findObject(By.desc("StateStatus")), 5_000) != null)
            val nextSequence = oldStatus.substringAfter("sequence=").toInt() + 1
            check(device.wait(Until.hasObject(By.desc("StateStatus").text("SUCCESS; rows=$size; sequence=$nextSequence")), 10_000))
            device.findObject(By.desc("Rows")).fling(androidx.test.uiautomator.Direction.DOWN)
        }
        device.findObject(By.desc("Rotate")).click()
        device.waitForIdle()
        check(device.wait(Until.hasObject(By.desc("StateStatus").textStartsWith("SUCCESS; rows=$size")), 10_000))
        pressHome()
        check(device.wait(Until.gone(By.pkg(PACKAGE)), 10_000))
        // am kill is safe only after Android has moved the process to the background.
        val deadline = android.os.SystemClock.uptimeMillis() + 10_000
        do {
            device.executeShellCommand("am kill $PACKAGE")
            if (device.executeShellCommand("pidof $PACKAGE").isBlank()) break
            Thread.sleep(100)
        } while (android.os.SystemClock.uptimeMillis() < deadline)
        check(device.executeShellCommand("pidof $PACKAGE").isBlank()) { "Background process was not killed" }
        // Resume the existing task, preserving its saved Activity state.
        device.executeShellCommand("am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10200000 -n $PACKAGE/.BenchmarkActivity")
        check(device.wait(Until.hasObject(By.desc("StateStatus").text("SUCCESS; rows=$size; sequence=0")), 10_000)) {
            "Unexpected restored status: ${device.findObject(By.desc("StateStatus"))?.text}"
        }
    }

    companion object {
        private const val PACKAGE = "br.com.arch.toolkit.statebenchmark"

        @JvmStatic
        @Parameterized.Parameters(name = "old={0},n={1}")
        fun parameters(): List<Array<Any>> {
            val smoke = InstrumentationRegistry.getArguments().getString("stateBenchmarkSmoke") == "true"
            val sizes = if (smoke) listOf(100) else listOf(100, 1_000, 10_000)
            return listOf(false, true).flatMap { baseline ->
                sizes.map { size -> arrayOf(baseline, size) }
            }
        }
    }
}
