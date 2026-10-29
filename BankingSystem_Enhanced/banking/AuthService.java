package banking;

// Authentication service.
public class AuthService.Value {

    public static final int MAX_FAILED_ATTEMPTS = 3;
    public static final long ADMIN_LOCK_MILLIS = 5 * 60 * 1000L;

    public enum Status { SUCCESS, INVALID_CREDENTIALS, LOCKED, PIN_NOT_ISSUED, ACCOUNT_CLOSED }

    public static final class LoginResult {
        public final Status status;
        public final CustomerSession session;
        public final String message;

        LoginResult(Status status, CustomerSession session, String message) {
            this.status = status;
            this.session = session;
            this.message = message;
        }
    }

    public static final class AdminLoginResult {
        public final boolean success;
        public final AdminSession session;
        public final String message;

        AdminLoginResult(boolean success, AdminSession session, String message) {
            this.success = success;
            this.session = session;
            this.message = message;
        }
    }

    private final Bank bank;

    public AuthService(Bank bank) {
        this.bank = bank;
    }


    public LoginResult loginCustomer(String accountNumber, char[] pin) {
        BankAccount account = bank.findAccount(accountNumber);
        Customer customer = (account == null) ? null : bank.findCustomer(account.getOwnerCustomerId());

        if (customer == null) {
            PinSecurity.hash(pin == null ? new char[0] : pin, PinSecurity.newSalt());
            AuditLogger.log("ANONYMOUS", "CUSTOMER LOGIN", "account " + accountNumber, "FAILED - unknown account");
            return new LoginResult(Status.INVALID_CREDENTIALS, null, "Incorrect account number or PIN.");
        }
        String actor = "CUSTOMER:" + customer.getCustomerId();

        if (customer.isLocked()) {
            AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(), "FAILED - locked");
            return new LoginResult(Status.LOCKED, null,
                    "This account is LOCKED after " + MAX_FAILED_ATTEMPTS
                            + " failed PIN attempts. Please contact the bank administrator.");
        }
        if (!customer.hasPin()) {
            AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(), "FAILED - no PIN issued");
            return new LoginResult(Status.PIN_NOT_ISSUED, null,
                    "No PIN has been issued for this account yet. Please contact the bank administrator.");
        }

        if (!customer.verifyPin(pin)) {
            int failures = customer.recordFailedAttempt();
            if (failures >= MAX_FAILED_ATTEMPTS) {
                customer.lock();
                bank.save();
                AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(), "FAILED - account LOCKED");
                return new LoginResult(Status.LOCKED, null,
                        "Incorrect PIN. Too many failed attempts: this account is now LOCKED. "
                                + "Please contact the bank administrator.");
            }
            bank.save();
            AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(),
                    "FAILED - incorrect PIN (" + failures + "/" + MAX_FAILED_ATTEMPTS + ")");
            return new LoginResult(Status.INVALID_CREDENTIALS, null,
                    "Incorrect account number or PIN. Attempts remaining: " + (MAX_FAILED_ATTEMPTS - failures) + ".");
        }

        if (account.isClosed()) {
            AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(), "FAILED - account closed");
            return new LoginResult(Status.ACCOUNT_CLOSED, null, "This account has been closed.");
        }

        customer.resetFailedAttempts();
        bank.save();
        AuditLogger.log(actor, "CUSTOMER LOGIN", account.getAccountNumber(), "SUCCESS");
        return new LoginResult(Status.SUCCESS, new CustomerSession(customer.getCustomerId()), "Login successful.");
    }

    public void logout(CustomerSession session) {
        if (session != null && session.isActive()) {
            session.end();
            AuditLogger.log("CUSTOMER:" + session.getCustomerId(), "LOGOUT", "-", "SUCCESS");
        }
    }

    public boolean mustChangePin(CustomerSession session) {
        Customer c = bank.findCustomer(session.getCustomerId());
        return c != null && c.isMustChangePin();
    }

    public String setNewPin(CustomerSession session, char[] newPin) {
        Customer c = bank.findCustomer(session.getCustomerId());
        if (c == null || !session.isActive()) return "Not logged in.";
        String problem = PinSecurity.validatePin(newPin);
        if (problem != null) return problem;
        if (c.verifyPin(newPin)) return "The new PIN must be different from your current PIN.";
        c.setPin(newPin, false);
        bank.save();
        AuditLogger.log("CUSTOMER:" + c.getCustomerId(), "PIN CHANGE", c.getCustomerId(), "SUCCESS");
        return null;
    }

    public String changePin(CustomerSession session, char[] currentPin, char[] newPin) {
        Customer c = bank.findCustomer(session.getCustomerId());
        if (c == null || !session.isActive()) return "Not logged in.";
        if (!c.verifyPin(currentPin)) {
            int failures = c.recordFailedAttempt();
            if (failures >= MAX_FAILED_ATTEMPTS) {
                c.lock();
                session.end();
                bank.save();
                AuditLogger.log("CUSTOMER:" + c.getCustomerId(), "PIN CHANGE", c.getCustomerId(), "FAILED - account LOCKED");
                return "Incorrect current PIN. Too many failed attempts: account LOCKED and you have been logged out.";
            }
            bank.save();
            AuditLogger.log("CUSTOMER:" + c.getCustomerId(), "PIN CHANGE", c.getCustomerId(), "FAILED - wrong current PIN");
            return "Incorrect current PIN. Attempts remaining: " + (MAX_FAILED_ATTEMPTS - failures) + ".";
        }
        c.resetFailedAttempts();
        return setNewPin(session, newPin);
    }


    public boolean isAdminConfigured() {
        return bank.getAdmin() != null;
    }

    public String setUpAdmin(String username, char[] password) {
        if (isAdminConfigured()) return "An administrator is already configured.";
        if (username == null || !username.matches("[A-Za-z0-9_]{3,20}")) {
            return "Username must be 3-20 letters, digits or underscores.";
        }
        String problem = PinSecurity.validateAdminPassword(password);
        if (problem != null) return problem;
        byte[] salt = PinSecurity.newSalt();
        bank.setAdmin(new AdminCredential(username, salt, PinSecurity.hash(password, salt), 0, 0));
        bank.save();
        AuditLogger.log("SYSTEM", "ADMIN SETUP", username, "SUCCESS");
        return null;
    }

    public AdminLoginResult loginAdmin(String username, char[] password) {
        AdminCredential admin = bank.getAdmin();
        if (admin == null) {
            return new AdminLoginResult(false, null, "No administrator has been configured.");
        }
        long now = System.currentTimeMillis();
        if (admin.lockedUntilMillis > now) {
            long minutes = (admin.lockedUntilMillis - now + 59_999) / 60_000;
            AuditLogger.log("ANONYMOUS", "ADMIN LOGIN", "-", "FAILED - temporarily locked");
            return new AdminLoginResult(false, null,
                    "Administrator login is temporarily locked. Try again in about " + minutes + " minute(s).");
        }
        boolean userOk = admin.username.equals(username);
        boolean passOk = PinSecurity.matches(password, admin.salt, admin.hash);
        if (userOk && passOk) {
            admin.failedAttempts = 0;
            admin.lockedUntilMillis = 0;
            bank.save();
            AuditLogger.log("ADMIN:" + admin.username, "ADMIN LOGIN", "-", "SUCCESS");
            return new AdminLoginResult(true, new AdminSession(admin.username), "Administrator login successful.");
        }
        admin.failedAttempts++;
        String outcome = "FAILED - incorrect credentials (" + admin.failedAttempts + "/" + MAX_FAILED_ATTEMPTS + ")";
        String message = "Incorrect administrator username or password.";
        if (admin.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            admin.lockedUntilMillis = now + ADMIN_LOCK_MILLIS;
            admin.failedAttempts = 0;
            outcome = "FAILED - administrator login LOCKED for 5 minutes";
            message = "Too many failed attempts: administrator login is locked for 5 minutes.";
        }
        bank.save();
        AuditLogger.log("ANONYMOUS", "ADMIN LOGIN", "-", outcome);
        return new AdminLoginResult(false, null, message);
    }

    public void logout(AdminSession session) {
        if (session != null && session.isActive()) {
            session.end();
            AuditLogger.log("ADMIN:" + session.getUsername(), "ADMIN LOGOUT", "-", "SUCCESS");
        }
    }

    public String changeAdminPassword(AdminSession session, char[] current, char[] newPassword) {
        AdminCredential admin = bank.getAdmin();
        if (session == null || !session.isActive() || admin == null) return "Not logged in as administrator.";
        if (!PinSecurity.matches(current, admin.salt, admin.hash)) {
            AuditLogger.log("ADMIN:" + admin.username, "CHANGE ADMIN PASSWORD", "-", "FAILED - wrong current password");
            return "Current password is incorrect.";
        }
        String problem = PinSecurity.validateAdminPassword(newPassword);
        if (problem != null) return problem;
        admin.salt = PinSecurity.newSalt();
        admin.hash = PinSecurity.hash(newPassword, admin.salt);
        bank.save();
        AuditLogger.log("ADMIN:" + admin.username, "CHANGE ADMIN PASSWORD", "-", "SUCCESS");
        return null;
    }
}
