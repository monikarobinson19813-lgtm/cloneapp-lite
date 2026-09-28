package top.niunaijun.blackboxa

import android.Manifest
import android.app.Application
import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.content.pm.ServiceInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.core.system.am.JobServicePolicy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JobServicePolicyRobolectricTest {

    @Test
    fun acceptsServiceProtectedByBindJobService() {
        val info = ServiceInfo().apply {
            permission = Manifest.permission.BIND_JOB_SERVICE
        }

        assertTrue(JobServicePolicy.isSchedulableJobService(info))
    }

    @Test
    fun rejectsOrdinaryServiceFromJobSchedulerRouting() {
        val info = ServiceInfo().apply {
            permission = null
        }

        assertFalse(JobServicePolicy.isSchedulableJobService(info))
    }

    @Test
    fun plainServiceCannotBeCastIntoJobServicePipeline() {
        assertNull(JobServicePolicy.asJobService(PlainService()))
    }

    @Test
    fun realJobServicePassesRuntimeGuard() {
        val service = RealJobService()
        assertSame(service, JobServicePolicy.asJobService(service))
    }

    private class PlainService : Service() {
        override fun onBind(intent: Intent?) = null
    }

    private class RealJobService : JobService() {
        override fun onStartJob(params: JobParameters?) = false
        override fun onStopJob(params: JobParameters?) = false
    }
}
