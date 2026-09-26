package top.niunaijun.blackboxa

import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.ActivityResolveInfoGuard

class ActivityResolveInfoGuardTest {
    @Test
    fun rejectsNullAndIncompleteResolveInfo() {
        assertFalse(ActivityResolveInfoGuard.isUsable(null))
        assertFalse(ActivityResolveInfoGuard.isUsable(ResolveInfo()))

        val missingName = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "viewer.pkg"
            }
        }
        assertFalse(ActivityResolveInfoGuard.isUsable(missingName))
    }

    @Test
    fun acceptsCompleteActivityResolveInfo() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "viewer.pkg"
                name = "viewer.pkg.PdfActivity"
            }
        }
        assertTrue(ActivityResolveInfoGuard.isUsable(resolveInfo))
    }
}
