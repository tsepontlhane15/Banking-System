package banking;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Receipt model.
public final class Receipt.Value {

    private static final DateTimeFormatter REF_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter SHOW_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> ISSUED = new HashSet<>();

    private final String reference;
    private final LocalDateTime timestamp;
    private final String type;
    private final double amount;
    private final String accountNumber;
    private final String otherAccountNumber;
    private final double balanceAfter;
    private final double otherBalanceAfter;
    private final String message;

    public Receipt(String type, double amount, String accountNumber, String otherAccountNumber,
                   double balanceAfter, double otherBalanceAfter, String message) {
        this.timestamp = LocalDateTime.now();
        this.reference = nextReference(timestamp);
        this.type = type;
        this.amount = amount;
        this.accountNumber = accountNumber;
        this.otherAccountNumber = otherAccountNumber;
        this.balanceAfter = balanceAfter;
        this.otherBalanceAfter = otherBalanceAfter;
        this.message = message;
    }

    private static synchronized String nextReference(LocalDateTime now) {
        String ref;
        do {
            ref = "LB" + now.format(REF_TIME) + "-"
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        } while (!ISSUED.add(ref));
        return ref;
    }

    public String getReference() {
        return reference;
    }

    public void display() {
        String line = "=".repeat(52);
        System.out.println();
        System.out.println(line);
        System.out.println("            LIBATA BANKING SYSTEM");
        System.out.println("           TRANSACTION RECEIPT");
        System.out.println(line);
        System.out.printf("Reference No   : %s%n", reference);
        System.out.printf("Date & Time    : %s%n", timestamp.format(SHOW_TIME));
        System.out.printf("Type           : %s%n", type);
        if (otherAccountNumber == null) {
            System.out.printf("Account        : %s%n", accountNumber);
        } else {
            System.out.printf("From Account   : %s%n", accountNumber);
            System.out.printf("To Account     : %s%n", otherAccountNumber);
        }
        System.out.printf("Amount         : M%,.2f%n", amount);
        System.out.printf("Balance After  : M%,.2f%s%n", balanceAfter,
                otherAccountNumber == null ? "" : " (" + accountNumber + ")");
        if (!Double.isNaN(otherBalanceAfter)) {
            System.out.printf("Recipient Bal. : M%,.2f (%s)%n", otherBalanceAfter, otherAccountNumber);
        }
        System.out.printf("Status         : SUCCESSFUL%n");
        System.out.printf("Message        : %s%n", message);
        System.out.println(line);
    }
}
