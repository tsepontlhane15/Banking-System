package banking;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Audit log helper.
public final class AuditLogger.Value {

    private static final String FILE_NAME = "audit.log";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuditLogger() {
    }

    public static void log(String actor, String action, String target, String outcome) {
        String record = String.format("[%s] Actor: %s | Action: %s | Target: %s | Outcome: %s",
                LocalDateTime.now().format(FORMAT), clean(actor), clean(action), clean(target), clean(outcome));
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            writer.println(record);
        } catch (IOException e) {
            System.out.println("Warning: could not write to audit log (" + e.getMessage() + ").");
        }
    }

    private static String clean(String s) {
        return s == null ? "N/A" : s.replace('\r', ' ').replace('\n', ' ');
    }
}
