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

    fun instanceLabel(userId: Int, savedRemark: String?): String =
        savedRemark?.takeIf { it.isNotBlank() } ?: "User $userId"
}
