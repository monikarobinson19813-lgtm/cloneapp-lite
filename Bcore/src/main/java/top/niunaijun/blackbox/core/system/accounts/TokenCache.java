package top.niunaijun.blackbox.core.system.accounts;

import android.accounts.Account;

import java.util.Objects;


public class TokenCache {
    public int userId;
    public Account account;
    public long expiryEpochMillis;
    public String authToken;
    public String authTokenType;
    public String packageName;

    public TokenCache(int userId,Account account,
                      String callerPkg,
                      String tokenType,
                      String token,
                      long expiryMillis) {
        this.userId = userId;
        this.account = account;
        this.expiryEpochMillis = expiryMillis;
        this.authToken = token;
        this.authTokenType = tokenType;
        this.packageName = callerPkg;
    }

    public TokenCache(int userId, Account account, String authTokenType, String packageName) {
        this.userId = userId;
        this.account = account;
        this.authToken = authToken;
        this.authTokenType = authTokenType;
        this.packageName = packageName;
    }

    /** Enforce the virtual-user boundary when retrieving cached tokens. */
    public boolean matchesRead(int requestedUserId, Account requestedAccount,
                               String requestedTokenType, String requestedPackage) {
        return userId == requestedUserId
                && Objects.equals(account, requestedAccount)
                && Objects.equals(authTokenType, requestedTokenType)
                && Objects.equals(packageName, requestedPackage);
    }

    /** Invalidation must never remove another virtual user's cached token. */
    public boolean matchesInvalidation(int requestedUserId, String requestedAccountType,
                                       String requestedToken) {
        return userId == requestedUserId
                && account != null
                && Objects.equals(account.type, requestedAccountType)
                && Objects.equals(authToken, requestedToken);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenCache)) return false;
        TokenCache that = (TokenCache) o;
        return userId == that.userId &&
                expiryEpochMillis == that.expiryEpochMillis &&
                Objects.equals(account, that.account) &&
                Objects.equals(authToken, that.authToken) &&
                Objects.equals(authTokenType, that.authTokenType) &&
                Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, account, expiryEpochMillis, authToken, authTokenType, packageName);
    }
}
