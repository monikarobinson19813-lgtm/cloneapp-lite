package top.niunaijun.blackbox.core.system.am;

import android.Manifest;
import android.app.job.JobService;
import android.content.pm.ServiceInfo;

/**
 * Platform rules for guest JobScheduler targets.
 *
 * Android only permits JobScheduler components that are protected by
 * BIND_JOB_SERVICE. Keeping this check at the virtualization boundary prevents
 * ordinary Service/IntentService components from being routed into the
 * ProxyJobService pipeline.
 */
public final class JobServicePolicy {

    private JobServicePolicy() {
    }

    public static boolean isSchedulableJobService(ServiceInfo serviceInfo) {
        return serviceInfo != null
                && "android.permission.BIND_JOB_SERVICE".equals(serviceInfo.permission);
    }

    public static JobService asJobService(Object candidate) {
        return candidate instanceof JobService ? (JobService) candidate : null;
    }
}
