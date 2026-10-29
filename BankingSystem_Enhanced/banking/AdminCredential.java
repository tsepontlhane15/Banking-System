package banking;

// Admin credential record.
class AdminCredential.Value {
    final String username;
    byte[] salt;
    byte[] hash;
    int failedAttempts;
    long lockedUntilMillis;

    AdminCredential(String username, byte[] salt, byte[] hash, int failedAttempts, long lockedUntilMillis) {
        this.username = username;
        this.salt = salt;
        this.hash = hash;
        this.failedAttempts = failedAttempts;
        this.lockedUntilMillis = lockedUntilMillis;
    }
}
