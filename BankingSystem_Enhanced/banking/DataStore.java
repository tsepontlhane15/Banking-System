package banking;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

// Read and write saved bank data.
// Data persistence helper.
class DataStore.Value {

    private final Path customersFile;
    private final Path accountsFile;
    private final Path adminFile;
    private final Path folder;

    DataStore(String folderName) {
        this.folder = Paths.get(folderName);
        this.customersFile = folder.resolve("customers.txt");
        this.accountsFile = folder.resolve("accounts.txt");
        this.adminFile = folder.resolve("admin.txt");
    }

    boolean exists() {
        return Files.exists(customersFile) || Files.exists(accountsFile);
    }

    // Save all records to disk.
    void save(Bank bank) {
        try {
            Files.createDirectories(folder);
            StringBuilder cs = new StringBuilder();
            for (Customer c : bank.allCustomers()) {
                cs.append(String.join("|", c.getCustomerId(), c.getFullName(), c.getPhone(), c.getAddress(),
                        enc(c.getPinSalt()), enc(c.getPinHash()), String.valueOf(c.getFailedAttempts()),
                        String.valueOf(c.isLocked()), String.valueOf(c.isMustChangePin()))).append('\n');
            }
            writeAtomically(customersFile, cs.toString());

            StringBuilder as = new StringBuilder();
            for (BankAccount a : bank.allAccounts()) {
                String type = (a instanceof SavingsAccount) ? "SAVINGS" : "CURRENT";
                as.append(String.join("|", type, a.getAccountNumber(), nz(a.getOwnerCustomerId()),
                        a.getAccountHolderName(), Double.toString(a.getBalance()),
                        a.getDateOpened().toString(), a.getStatus())).append('\n');
            }
            writeAtomically(accountsFile, as.toString());

            AdminCredential ad = bank.getAdmin();
            if (ad != null) {
                writeAtomically(adminFile, String.join("|", ad.username, enc(ad.salt), enc(ad.hash),
                        String.valueOf(ad.failedAttempts), String.valueOf(ad.lockedUntilMillis)) + "\n");
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save data (" + e.getMessage() + ").");
        }
    }

    private void writeAtomically(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // Load all records from disk.
    void load(Bank bank) {
        try {
            if (Files.exists(customersFile)) {
                for (String line : Files.readAllLines(customersFile, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    String[] f = line.split("\\|", -1);
                    if (f.length < 9) { warn("customers.txt", line); continue; }
                    Customer c = new Customer(f[0], f[1], f[2], f[3]);
                    c.restoreCredentials(dec(f[4]), dec(f[5]), Boolean.parseBoolean(f[8]),
                            Integer.parseInt(f[6]), Boolean.parseBoolean(f[7]));
                    bank.addCustomer(c);
                }
            }
            if (Files.exists(accountsFile)) {
                for (String line : Files.readAllLines(accountsFile, StandardCharsets.UTF_8)) {
                    if (line.isBlank()) continue;
                    String[] f = line.split("\\|", -1);
                    if (f.length < 7) { warn("accounts.txt", line); continue; }
                    LocalDateTime opened = LocalDateTime.parse(f[5]);
                    BankAccount a = f[0].equals("SAVINGS")
                            ? new SavingsAccount(f[1], f[3], 0.0, opened)
                            : new CurrentAccount(f[1], f[3], 0.0, opened);
                    a.restoreState(Double.parseDouble(f[4]), f[6].equals("FROZEN"), f[6].equals("CLOSED"));
                    a.setOwnerCustomerId(f[2].isEmpty() ? null : f[2]);
                    bank.addAccount(a);
                }
            }
            if (Files.exists(adminFile)) {
                List<String> lines = Files.readAllLines(adminFile, StandardCharsets.UTF_8);
                if (!lines.isEmpty()) {
                    String[] f = lines.get(0).split("\\|", -1);
                    if (f.length >= 5) {
                        bank.setAdmin(new AdminCredential(f[0], dec(f[1]), dec(f[2]),
                                Integer.parseInt(f[3]), Long.parseLong(f[4])));
                    }
                }
            }
        } catch (IOException | RuntimeException | InvalidTransactionException e) {
            System.err.println("Warning: problem reading saved data (" + e.getMessage() + ").");
        }
    }

    private static void warn(String file, String line) {
        System.err.println("Warning: skipped malformed record in " + file + ".");
    }

    private static String enc(byte[] b) { return b == null ? "" : Base64.getEncoder().encodeToString(b); }
    private static byte[] dec(String s) { return s.isEmpty() ? null : Base64.getDecoder().decode(s); }
    private static String nz(String s) { return s == null ? "" : s; }
}
