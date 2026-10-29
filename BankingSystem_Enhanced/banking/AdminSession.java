package banking;

// Admin session.
public final class AdminSession.Value {

    private final String username;
    private boolean active = true;

    AdminSession(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }
    public boolean isActive() { return active; }
    void end() { active = false; }
}
