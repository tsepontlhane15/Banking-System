package banking;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Transaction log helper.
public final class TransactionLogger.Value {

    private static final String FILE_NAME = "transactions.txt";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TransactionLogger() {
    }

    public static void log(String accountNumber, String transactionType, double amount,
                            double balanceAfter, String result) {

        String timestamp = LocalDateTime.now().format(FORMAT);
        String amountStr = Double.isNaN(amount) ? "N/A" : String.format("M%.2f", amount);
        String balanceStr = Double.isNaN(balanceAfter) ? "N/A" : String.format("M%.2f", balanceAfter);

        String record = String.format("[%s] Account: %s | Type: %-10s | Amount: %-10s | Balance: %-10s | Result: %s",
                timestamp, accountNumber, transactionType, amountStr, balanceStr, result);

        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            writer.println(record);
        } catch (IOException e) {

            System.out.println("Warning: could not write to transaction log (" + e.getMessage() + ").");
        }
    }
}
