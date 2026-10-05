package top.niunaijun.blackboxa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.AppOpsReturnTypeGuard

class AppOpsReturnTypeGuardTest {
    @Test
    fun matchesAndroid14SyncNotedNoteOperationOnly() {
        assertTrue(
            AppOpsReturnTypeGuard.isSyncNotedNoteOperation(
                "noteOperation",
                "android.app.SyncNotedAppOp"
            )
        )
        assertFalse(
            AppOpsReturnTypeGuard.isSyncNotedNoteOperation(
                "noteOperation",
                "java.lang.Integer"
            )
        )
        assertFalse(
            AppOpsReturnTypeGuard.isSyncNotedNoteOperation(
                "checkOperation",
                "android.app.SyncNotedAppOp"
            )
        )
    }
}
