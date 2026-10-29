package banking;

// Customer record.
public class Customer.Value {

    private final String customerId;
    private String fullName;
    private String phone;
    private String address;

    private byte[] pinSalt;
    private byte[] pinHash;
    private boolean mustChangePin;
    private int failedAttempts;
    private boolean locked;

    public Customer(String customerId, String fullName, String phone, String address) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    public String getCustomerId() { return customerId; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public boolean hasPin() { return pinHash != null; }
    public boolean isMustChangePin() { return mustChangePin; }
    public int getFailedAttempts() { return failedAttempts; }
    public boolean isLocked() { return locked; }

    public String getPinStatus() {
        if (locked) return "LOCKED";
        if (!hasPin()) return "NOT ISSUED";
        return mustChangePin ? "TEMPORARY" : "SET";
    }

    boolean verifyPin(char[] pin) {
        return hasPin() && PinSecurity.matches(pin, pinSalt, pinHash);
    }

    void setPin(char[] pin, boolean temporary) {
        this.pinSalt = PinSecurity.newSalt();
        this.pinHash = PinSecurity.hash(pin, pinSalt);
        this.mustChangePin = temporary;
    }

    void restoreCredentials(byte[] salt, byte[] hash, boolean mustChange, int failed, boolean locked) {
        this.pinSalt = salt;
        this.pinHash = hash;
        this.mustChangePin = mustChange;
        this.failedAttempts = failed;
        this.locked = locked;
    }

    byte[] getPinSalt() { return pinSalt; }
    byte[] getPinHash() { return pinHash; }

    void updateDetails(String fullName, String phone, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    int recordFailedAttempt() { return ++failedAttempts; }
    void resetFailedAttempts() { failedAttempts = 0; }
    void lock() { locked = true; }
    void unlock() { locked = false; failedAttempts = 0; }
}
