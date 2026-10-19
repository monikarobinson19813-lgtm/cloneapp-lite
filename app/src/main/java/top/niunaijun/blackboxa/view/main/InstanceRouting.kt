package top.niunaijun.blackboxa.view.main

/**
 * Pure routing helpers for instance-sensitive UI actions.
 *
 * Keeping this logic Android-free lets CI prove that delayed activity results and page
 * changes cannot silently retarget an install to the currently visible instance.
 */
internal object InstanceRouting {
    fun installTarget(returnedUserId: Int, attachedUserIds: Collection<Int>): Int? =
        returnedUserId.takeIf { it in attachedUserIds }

    fun userForPage(position: Int, userIds: List<Int>): Int? =
        userIds.getOrNull(position)

    // Never reuse a deleted user's ID while older proxy tasks might still exist.
    fun nextVirtualUserId(existingIds: Collection<Int>): Int? =
        existingIds.maxOrNull()?.let { if (it == Int.MAX_VALUE) null else it + 1 } ?: 0

    fun mayDeleteInstance(
        userId: Int,
        existingIds: Collection<Int>,
        packageName: String,
        isInstalled: Boolean
    ): Boolean = userId >= 0 &&
        userId in existingIds &&
        packageName == "com.whatsapp" &&
        isInstalled

    fun deletionComplete(
        targetId: Int,
        beforeIds: Collection<Int>,
        afterIds: Collection<Int>,
        internalUserDirExists: Boolean,
        externalUserDirExists: Boolean
    ): Boolean = targetId in beforeIds &&
        afterIds.toSet() == (beforeIds.toSet() - targetId) &&
        !internalUserDirExists && !externalUserDirExists

    /**
     * Post-wipe forensic snapshot. deleteUser() itself has no rollback and its
     * file deletions can fail silently; never reduce a partial wipe to "failed".
     */
    fun deletionFailureDetails(
        targetId: Int,
        beforeIds: Collection<Int>,
        afterIds: Collection<Int>,
        internalRootExists: Boolean,
        externalRootExists: Boolean,
        packageStillInstalled: Boolean
    ): String? {
        val issues = mutableListOf<String>()
        if (targetId !in beforeIds) issues.add("target user was absent before wipe")
        if (targetId in afterIds) issues.add("user still registered")
        if (afterIds.toSet() != beforeIds.toSet() - targetId) {
            issues.add("surviving user IDs differ from the pre-wipe snapshot")
        }
        val residualRoots = mutableListOf<String>()
        if (internalRootExists) residualRoots.add("internal data/user/$targetId")
        if (externalRootExists) residualRoots.add("external storage/emulated/$targetId")
        if (residualRoots.isNotEmpty()) {
            val prefix = if (targetId !in afterIds) "user removed but data remains" else "data remains"
            issues.add("$prefix: ${residualRoots.joinToString(", ")}")
        }
        if (packageStillInstalled) issues.add("WhatsApp package still installed for User $targetId")
        return issues.takeIf { it.isNotEmpty() }?.joinToString("; ")
    }

    fun instanceLabel(userId: Int, savedRemark: String?): String =
        savedRemark?.takeIf { it.isNotBlank() } ?: "User $userId"
}
