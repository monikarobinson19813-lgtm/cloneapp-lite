package top.niunaijun.blackbox.fake.delegate

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import black.android.content.BRBroadcastReceiver
import black.android.content.BRBroadcastReceiverPendingResultM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RegisteredReceiverBridgeRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun screenOnPreservesFrameworkPendingResult() {
        assertSystemBroadcastPreservesCompletion(Intent.ACTION_SCREEN_ON)
    }

    @Test
    fun screenOffPreservesFrameworkPendingResult() {
        assertSystemBroadcastPreservesCompletion(Intent.ACTION_SCREEN_OFF)
    }

    @Test
    fun userPresentPreservesFrameworkPendingResult() {
        assertSystemBroadcastPreservesCompletion(Intent.ACTION_USER_PRESENT)
    }

    @Test
    fun connectivityChangePreservesFrameworkPendingResult() {
        assertSystemBroadcastPreservesCompletion("android.net.conn.CONNECTIVITY_CHANGE")
    }

    @Test
    fun proxyBroadcastUnwrapsTargetWithoutChangingCompletionIdentity() {
        val target = RecordingReceiver()
        val bridge = RegisteredReceiverBridge(target)
        val token = Binder()
        val pending = newPendingResult(token)
        BRBroadcastReceiver.get(bridge).setPendingResult(pending)

        val realIntent = Intent("cloneapp.test.REAL").putExtra("payload", "guest")
        val shadowIntent = Intent("cloneapp.test.SHADOW")
        ProxyBroadcastRecord.saveStub(shadowIntent, realIntent, 0)

        bridge.onReceive(context, shadowIntent)

        assertEquals("cloneapp.test.REAL", target.seenIntent?.action)
        assertEquals("guest", target.seenIntent?.getStringExtra("payload"))
        assertSame(pending, target.seenPendingResult)
        assertSame(pending, BRBroadcastReceiver.get(bridge).getPendingResult())
    }

    @Test
    fun goAsyncKeepsFrameworkPendingResultWithGuestAndPreventsEarlyFinish() {
        val target = AsyncReceiver()
        val bridge = RegisteredReceiverBridge(target)
        val pending = newPendingResult(Binder())
        BRBroadcastReceiver.get(bridge).setPendingResult(pending)

        bridge.onReceive(context, Intent(Intent.ACTION_SCREEN_ON))

        assertSame(pending, target.seenPendingResult)
        assertSame(pending, target.asyncPendingResult)
        assertNull(BRBroadcastReceiver.get(bridge).getPendingResult())
    }

    private fun assertSystemBroadcastPreservesCompletion(action: String) {
        val target = RecordingReceiver()
        val bridge = RegisteredReceiverBridge(target)
        val pending = newPendingResult(Binder())
        BRBroadcastReceiver.get(bridge).setPendingResult(pending)

        bridge.onReceive(context, Intent(action))

        assertEquals(action, target.seenIntent?.action)
        assertSame(pending, target.seenPendingResult)
        assertSame(pending, BRBroadcastReceiver.get(bridge).getPendingResult())
    }

    private fun newPendingResult(token: IBinder): BroadcastReceiver.PendingResult {
        return BRBroadcastReceiverPendingResultM.get()._new(
            0,
            null,
            null,
            0,
            false,
            false,
            token,
            0,
            0
        )
    }

    private class RecordingReceiver : BroadcastReceiver() {
        var seenIntent: Intent? = null
        var seenPendingResult: PendingResult? = null

        override fun onReceive(context: Context?, intent: Intent?) {
            seenIntent = intent
            seenPendingResult = BRBroadcastReceiver.get(this).getPendingResult()
        }
    }

    private class AsyncReceiver : BroadcastReceiver() {
        var seenPendingResult: PendingResult? = null
        var asyncPendingResult: PendingResult? = null

        override fun onReceive(context: Context?, intent: Intent?) {
            seenPendingResult = BRBroadcastReceiver.get(this).getPendingResult()
            asyncPendingResult = goAsync()
        }
    }
}
