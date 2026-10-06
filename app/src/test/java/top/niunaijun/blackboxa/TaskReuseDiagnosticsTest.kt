package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Test
import top.niunaijun.blackbox.core.system.am.TaskReuseDiagnostics

class TaskReuseDiagnosticsTest {

    @Test
    fun detectsDeadSelectedTaskMaskingLaterLiveSameInstanceTask() {
        val states = listOf(
            TaskReuseDiagnostics.TaskState(100, 1, "com.whatsapp", false),
            TaskReuseDiagnostics.TaskState(101, 1, "com.whatsapp", true)
        )

        assertEquals(
            101,
            TaskReuseDiagnostics.findMaskedLiveTaskId(
                1,
                "com.whatsapp",
                100,
                true,
                states
            )
        )
    }

    @Test
    fun ignoresOtherUsersAndAffinities() {
        val states = listOf(
            TaskReuseDiagnostics.TaskState(200, 0, "com.whatsapp", true),
            TaskReuseDiagnostics.TaskState(201, 1, "other.affinity", true)
        )

        assertEquals(
            -1,
            TaskReuseDiagnostics.findMaskedLiveTaskId(
                1,
                "com.whatsapp",
                199,
                true,
                states
            )
        )
    }

    @Test
    fun doesNotFlagWhenSelectedTaskIsAlreadyLive() {
        val states = listOf(
            TaskReuseDiagnostics.TaskState(300, 1, "com.whatsapp", true),
            TaskReuseDiagnostics.TaskState(301, 1, "com.whatsapp", true)
        )

        assertEquals(
            -1,
            TaskReuseDiagnostics.findMaskedLiveTaskId(
                1,
                "com.whatsapp",
                300,
                false,
                states
            )
        )
    }
}
