package top.niunaijun.blackboxa.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.core.system.notification.NotificationIdentity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class NotificationIdentityRobolectricTest {

    @Test
    fun channelsAndLabelsAreDistinctPerVirtualUser() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val user0 = NotificationChannel(
            NotificationIdentity.channelId("messages", 0),
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        NotificationIdentity.applyChannelName(user0, 0)

        val user1 = NotificationChannel(
            NotificationIdentity.channelId("messages", 1),
            "Messages",
            NotificationManager.IMPORTANCE_HIGH
        )
        NotificationIdentity.applyChannelName(user1, 1)

        manager.createNotificationChannel(user0)
        manager.createNotificationChannel(user1)

        assertNotEquals(user0.id, user1.id)
        assertEquals("Messages · User 0", user0.name.toString())
        assertEquals("Messages · User 1", user1.name.toString())
    }

    @Test
    fun notificationSubtextKeepsExistingTextAndAddsInstanceOnce() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val notification = Notification.Builder(context, "messages")
            .setContentTitle("WhatsApp")
            .setSubText("Family")
            .build()

        NotificationIdentity.applyNotificationSubText(notification, 2)
        NotificationIdentity.applyNotificationSubText(notification, 2)

        assertEquals(
            "Family · User 2",
            notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT).toString()
        )
    }
}
