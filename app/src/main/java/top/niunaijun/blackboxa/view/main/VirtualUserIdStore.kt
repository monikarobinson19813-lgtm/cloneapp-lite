package top.niunaijun.blackboxa.view.main

import android.content.Context

/**
 * Durable, app-private high-water allocation. IDs are committed synchronously
 * *before* virtual installation so a crash, deletion or reboot cannot reuse one.
 *
 * Stored in the host app's SharedPreferences, independent of the virtual user's
 * directories erased by deleteUser(). Never derive the next ID from live users alone.
 */
internal object VirtualUserIdStore {
    private const val PREFS_NAME = "cloneapp_instance_id_allocator"
    private const val HIGHEST_EVER_KEY = "highest_ever_user_id"

    @Synchronized
    fun nextId(context: Context, existingIds: Collection<Int>): Int? {
        val mark = migratedMark(context, existingIds)
        return InstanceRouting.nextVirtualUserId(mark)
    }

    @Synchronized
    fun reserve(context: Context, requestedId: Int, existingIds: Collection<Int>): Boolean {
        val mark = migratedMark(context, existingIds)
        if (InstanceRouting.nextVirtualUserId(mark) != requestedId) return false
        return persistMark(context, requestedId)
    }

    private fun migratedMark(context: Context, existingIds: Collection<Int>): Int {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val old = prefs.getInt(HIGHEST_EVER_KEY, -1)
        val migrated = InstanceRouting.seedHighWaterMark(old, existingIds)
        if (!prefs.contains(HIGHEST_EVER_KEY) || old != migrated) {
            check(prefs.edit().putInt(HIGHEST_EVER_KEY, migrated).commit()) {
                "Could not persist virtual user ID high-water mark"
            }
        }
        return migrated
    }

    private fun persistMark(context: Context, allocated: Int): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        check(prefs.edit().putInt(HIGHEST_EVER_KEY, allocated).commit()) {
            "Could not reserve virtual user ID $allocated"
        }
        return true
    }
}
