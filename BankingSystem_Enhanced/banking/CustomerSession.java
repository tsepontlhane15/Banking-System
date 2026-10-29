package banking;

// Customer session.
public final class CustomerSession.Value {

    private final String customerId;
    private boolean active = true;

    CustomerSession(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() { return customerId; }
    public boolean isActive() { return active; }
    void end() { active = false; }
}
