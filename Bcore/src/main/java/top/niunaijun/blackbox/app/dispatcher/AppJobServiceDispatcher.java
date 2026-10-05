package top.niunaijun.blackbox.app.dispatcher;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.res.Configuration;

import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.JobRecord;
import top.niunaijun.blackbox.utils.Slog;

public class AppJobServiceDispatcher {
    public static final String TAG = "AppJobServiceDispatcher";
    private static final AppJobServiceDispatcher sServiceDispatcher = new AppJobServiceDispatcher();
    private final Map<Integer, JobRecord> mJobRecords = new HashMap<>();
    private final Map<Integer, Service> mScheduledServices = new HashMap<>();

    public static AppJobServiceDispatcher get() { return sServiceDispatcher; }

    public boolean onStartJob(JobParameters params) {
        try {
            Service service = getScheduledService(params.getJobId());
            if (service == null) return false;
            Slog.i(TAG, "CLF9_BISECT_BUILD4_JOB_COMPAT jobId=" + params.getJobId()
                    + " service=" + service.getClass().getName());
            Boolean result = JobServiceCompat.onStartJob(service, params);
            if (result == null) {
                Slog.w(TAG, "Scheduled service has no JobService-compatible engine: " + service.getClass().getName());
                return false;
            }
            if (!(service instanceof JobService)) {
                Slog.i(TAG, "CLF9_JOB_COMPAT start via embedded JobServiceEngine: " + service.getClass().getName());
            }
            return result;
        } catch (Throwable t) {
            Slog.e(TAG, "Unable to start scheduled service", t);
            return false;
        }
    }

    public boolean onStopJob(JobParameters params) {
        Service service = getScheduledService(params.getJobId());
        if (service == null) return false;
        boolean reschedule = false;
        try {
            Boolean result = JobServiceCompat.onStopJob(service, params);
            reschedule = result != null && result;
        } catch (Throwable t) {
            Slog.e(TAG, "Unable to stop scheduled service", t);
        } finally {
            try { service.onDestroy(); } catch (Throwable ignored) {}
            synchronized (mJobRecords) {
                mJobRecords.remove(params.getJobId());
                mScheduledServices.remove(params.getJobId());
            }
        }
        return reschedule;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        for (Service service : mScheduledServices.values()) service.onConfigurationChanged(newConfig);
    }

    public void onDestroy() {
    }

    public void onLowMemory() {
        for (Service service : mScheduledServices.values()) service.onLowMemory();
    }

    public void onTrimMemory(int level) {
        for (Service service : mScheduledServices.values()) service.onTrimMemory(level);
    }

    Service getScheduledService(int jobId) {
        synchronized (mJobRecords) {
            Service cached = mScheduledServices.get(jobId);
            if (cached != null) return cached;
            try {
                JobRecord record = BlackBoxCore.getBJobManager().queryJobRecord(BlackBoxCore.getAppProcessName(), jobId);
                if (record == null || record.mServiceInfo == null) return null;
                Service service = BlackBoxCore.currentActivityThread().createScheduledService(record.mServiceInfo);
                if (service == null) return null;
                if (service instanceof JobService) record.mJobService = (JobService) service;
                mJobRecords.put(jobId, record);
                mScheduledServices.put(jobId, service);
                return service;
            } catch (Throwable t) {
                Slog.e(TAG, "Unable to resolve scheduled service for job " + jobId, t);
                return null;
            }
        }
    }
}
