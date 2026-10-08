package top.niunaijun.blackboxa.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.core.system.notification.NotificationIdentity
import top.niunaijun.blackbox.core.system.notification.NotificationRecord

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

    @Test
    fun sameHostIdWithDifferentTagsCreatesDistinctNotifications() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("tag-test", "Tag test", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        // One host ID represents the same package + virtual user + guest numeric ID.
        val hostId = 4242
        manager.notify(
            "child-a",
            hostId,
            Notification.Builder(context, channel.id).setSmallIcon(android.R.drawable.ic_dialog_info).build()
        )
        manager.notify(
            "child-b",
            hostId,
            Notification.Builder(context, channel.id).setSmallIcon(android.R.drawable.ic_dialog_info).build()
        )

        val identities = manager.activeNotifications.map { it.tag to it.id }.toSet()
        assertTrue(identities.contains("child-a" to hostId))
        assertTrue(identities.contains("child-b" to hostId))
    }

    @Test
    fun summaryAndChildWithSameIdBothSurviveWhenTagsDiffer() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("group-test", "Group test", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val hostId = 5151
        val child = Notification.Builder(context, channel.id)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setGroup("messages")
            .build()
        val summary = Notification.Builder(context, channel.id)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setGroup("messages")
            .setGroupSummary(true)
            .build()

        manager.notify("child", hostId, child)
        manager.notify("summary", hostId, summary)

        val active = manager.activeNotifications.associateBy { it.tag }
        assertTrue(active.containsKey("child"))
        assertTrue(active.containsKey("summary"))
        assertFalse(active.getValue("child").notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
        assertTrue(active.getValue("summary").notification.flags and Notification.FLAG_GROUP_SUMMARY != 0)
    }

    @Test
    fun instanceDeleteTrackingCancelsAllTagIdPairs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel("delete-test", "Delete test", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)

        val hostId = 6262
        val record = NotificationRecord()
        record.mIds.add(NotificationRecord.NotificationKey("child", hostId))
        record.mIds.add(NotificationRecord.NotificationKey("summary", hostId))

        for (key in record.mIds) {
            manager.notify(
                key.tag,
                key.id,
                Notification.Builder(context, channel.id).setSmallIcon(android.R.drawable.ic_dialog_info).build()
            )
        }
        assertEquals(2, manager.activeNotifications.size)

        for (key in record.mIds) {
            manager.cancel(key.tag, key.id)
        }
        assertEquals(0, manager.activeNotifications.size)
    }
}
