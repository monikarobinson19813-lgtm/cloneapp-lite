package top.niunaijun.blackbox.core.system.accounts

import android.accounts.Account
import android.app.Application
import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BUserAccountsIdentityTest {

    private val account = Account("disposable-test", "example.account")

    @Test
    fun newRecordsUseTheirActualVirtualUserId() {
        assertEquals(0, BUserAccounts(0).userId)
        assertEquals(1, BUserAccounts(1).userId)
        assertEquals(2, BUserAccounts(2).userId)
    }

    @Test
    fun sameAccountHasDistinctTokenCachePartitionsInDifferentUsers() {
        val user0 = BUserAccounts(0)
        val user1 = BUserAccounts(1)
        val caches = listOf(
            token(user0, "token-u0"),
            token(user1, "token-u1")
        )

        val forUser0 = caches.filter { it.matchesRead(user0.userId, account, "access", "guest.example") }
        val forUser1 = caches.filter { it.matchesRead(user1.userId, account, "access", "guest.example") }

        assertEquals(listOf("token-u0"), forUser0.map { it.authToken })
        assertEquals(listOf("token-u1"), forUser1.map { it.authToken })
        assertNotEquals(forUser0.single().userId, forUser1.single().userId)
    }

    @Test
    fun invalidatingUser1DoesNotRemoveUser0Token() {
        val caches = mutableListOf(
            token(BUserAccounts(0), "shared-token"),
            token(BUserAccounts(1), "shared-token")
        )
        caches.removeAll { it.matchesInvalidation(1, account.type, "shared-token") }

        assertEquals(1, caches.size)
        assertEquals(0, caches.single().userId)
        assertEquals("shared-token", caches.single().authToken)
    }

    @Test
    fun legacyUser2RecordIsNormalizedUsingMapKey() {
        val legacy = BUserAccounts() // Previously defaulted to userId 0.
        val users = hashMapOf(0 to BUserAccounts(0), 2 to legacy)
        assertTrue(BUserAccounts.normalizeLoadedUserIds(users))
        assertEquals(2, legacy.userId)
        assertSame(legacy, users[2])
        assertFalse(BUserAccounts.normalizeLoadedUserIds(users))
    }

    @Test
    fun migratedRecordsRetainPasswordsAndTokens() {
        val legacy = BUserAccounts()
        val stored = legacy.addAccount(account)
        stored.password = "synthetic-password"
        stored.authTokens["access"] = "synthetic-auth-token"
        stored.accountUserData["preference"] = "unchanged"
        val users = hashMapOf(3 to legacy)

        assertTrue(BUserAccounts.normalizeLoadedUserIds(users))
        assertEquals(3, legacy.userId)
        val migrated = users.getValue(3).getAccount(account)
        assertSame(stored, migrated)
        assertEquals("synthetic-password", migrated.password)
        assertEquals("synthetic-auth-token", migrated.authTokens["access"])
        assertEquals("unchanged", migrated.accountUserData["preference"])
    }

    @Test
    fun migrationPersistsCorrectUserIdsAndPreservesAccountContents() {
        val legacy = BUserAccounts()
        legacy.addAccount(account).apply {
            password = "synthetic-password"
            authTokens["access"] = "synthetic-token"
        }
        val users = hashMapOf(0 to BUserAccounts(0), 2 to legacy)

        val restored = roundTrip(users)
        assertEquals(0, restored.getValue(2).userId) // Legacy on-disk state.
        assertTrue(BUserAccounts.normalizeLoadedUserIds(restored))
        val persisted = roundTrip(restored)

        assertEquals(0, persisted.getValue(0).userId)
        assertEquals(2, persisted.getValue(2).userId)
        assertEquals("synthetic-password", persisted.getValue(2).getAccount(account).password)
        assertEquals("synthetic-token", persisted.getValue(2).getAccount(account).authTokens["access"])
        assertFalse(BUserAccounts.normalizeLoadedUserIds(persisted))
    }

    private fun token(user: BUserAccounts, value: String): TokenCache =
        TokenCache(user.userId, account, "guest.example", "access", value, Long.MAX_VALUE)

    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    private fun roundTrip(users: HashMap<Int, BUserAccounts>): HashMap<Int, BUserAccounts> {
        val parcel = Parcel.obtain()
        val bytes = try {
            parcel.writeMap(users)
            parcel.marshall()
        } finally {
            parcel.recycle()
        }
        val restoredParcel = Parcel.obtain()
        return try {
            restoredParcel.unmarshall(bytes, 0, bytes.size)
            restoredParcel.setDataPosition(0)
            restoredParcel.readHashMap(BUserAccounts::class.java.classLoader)
                as HashMap<Int, BUserAccounts>
        } finally {
            restoredParcel.recycle()
        }
    }
}
