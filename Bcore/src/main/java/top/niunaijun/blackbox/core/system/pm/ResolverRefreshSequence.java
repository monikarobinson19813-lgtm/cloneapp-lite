package top.niunaijun.blackbox.core.system.pm;

/**
 * Keeps package metadata reload and component-resolver replacement in one tested sequence.
 */
final class ResolverRefreshSequence {
    private ResolverRefreshSequence() {
    }

    static void run(Runnable reloadPackages, Runnable removePreviousComponents, Runnable addCurrentComponents) {
        reloadPackages.run();
        removePreviousComponents.run();
        addCurrentComponents.run();
    }
}
