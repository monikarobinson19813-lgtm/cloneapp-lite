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
        packageName in setOf("com.whatsapp", "com.whatsapp.w4b") &&
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

    fun instanceLabel(userId: Int, savedRemark: String?): String =
        savedRemark?.takeIf { it.isNotBlank() } ?: "User $userId"
}
