package top.niunaijun.blackbox.app.dispatcher

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class JobServiceCompatTest {

    private open class EngineHolder {
        @JvmField
        val embeddedEngine = FakeEngine()
    }

    private class DerivedEngineHolder : EngineHolder()

    private class UnsupportedHolder

    private class StaticOnlyHolder {
        companion object {
            @JvmField
            val STATIC_ENGINE = FakeEngine()
        }
    }

    private class FakeEngine

    @Test
    fun findsCompatibleEngineStoredOnSuperclass() {
        val target = DerivedEngineHolder()

        val found = JobServiceCompat.findAssignableFieldValue(
            target,
            FakeEngine::class.java
        )

        assertSame(target.embeddedEngine, found)
    }

    @Test
    fun unsupportedTargetFailsClosed() {
        val found = JobServiceCompat.findAssignableFieldValue(
            UnsupportedHolder(),
            FakeEngine::class.java
        )

        assertNull(found)
    }

    @Test
    fun staticFieldsAreNotTreatedAsPerServiceEngines() {
        val found = JobServiceCompat.findAssignableFieldValue(
            StaticOnlyHolder(),
            FakeEngine::class.java
        )

        assertNull(found)
    }
}
