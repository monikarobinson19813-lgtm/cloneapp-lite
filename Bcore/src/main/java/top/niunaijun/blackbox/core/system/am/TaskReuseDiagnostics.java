package top.niunaijun.blackbox.core.system.am;

import java.util.Collection;

/**
 * Pure helper used by CLF-6 diagnostics.
 *
 * It detects the case where ActivityStack's currently selected matching task is
 * no longer live while a later task for the same virtual user/task affinity is
 * still live. The helper is diagnostic only and does not change task selection.
 */
public final class TaskReuseDiagnostics {

    private TaskReuseDiagnostics() {
    }

    public static final class TaskState {
        public final int id;
        public final int userId;
        public final String taskAffinity;
        public final boolean hasLiveActivity;

        public TaskState(int id, int userId, String taskAffinity, boolean hasLiveActivity) {
            this.id = id;
            this.userId = userId;
            this.taskAffinity = taskAffinity;
            this.hasLiveActivity = hasLiveActivity;
        }
    }

    public static int findMaskedLiveTaskId(
            int userId,
            String taskAffinity,
            int selectedTaskId,
            boolean selectedNeedsNewTask,
            Collection<TaskState> states) {
        if (!selectedNeedsNewTask || states == null || taskAffinity == null) {
            return -1;
        }

        for (TaskState state : states) {
            if (state == null || state.id == selectedTaskId || !state.hasLiveActivity) {
                continue;
            }
            if (state.userId == userId && taskAffinity.equals(state.taskAffinity)) {
                return state.id;
            }
        }
        return -1;
    }
}
