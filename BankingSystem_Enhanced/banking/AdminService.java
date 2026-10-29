package banking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Admin business service.
public class AdminService.Value {

    public record AccountSummary(String customerId, String customerName, String phone, String accountNumber,
                                 String accountType, String status, String pinStatus, LocalDateTime dateOpened) { }

    public record AccountCreation(BankAccount account, char[] tempPin) { }

    private final Bank bank;

    public AdminService(Bank bank) {
        this.bank = bank;
    }

    private void requireAdmin(AdminSession session) {
        if (session == null || !session.isActive()) {
            throw new SecurityException("Administrator authentication required.");
        }
    }

    private String actor(AdminSession s) { return "ADMIN:" + s.getUsername(); }

    private AdministrationException fail(AdminSession s, String action, String target, String reason) {
        AuditLogger.log(actor(s), action, target, "FAILED - " + reason);
        return new AdministrationException(reason);
    }


    public Customer registerCustomer(AdminSession s, String name, String phone, String address)
            throws AdministrationException {
        requireAdmin(s);
        String problem = validateDetails(name, phone, address);
        if (problem != null) throw fail(s, "REGISTER CUSTOMER", name, problem);
        Customer c = new Customer(bank.nextCustomerId(), name.trim(), phone.trim(), address.trim());
        bank.addCustomer(c);
        bank.save();
        AuditLogger.log(actor(s), "REGISTER CUSTOMER", c.getCustomerId(), "SUCCESS");
        return c;
    }

    public AccountCreation createAccount(AdminSession s, String customerId, boolean savings)
            throws AdministrationException {
        requireAdmin(s);
        Customer c = bank.findCustomer(customerId);
        if (c == null) throw fail(s, "CREATE ACCOUNT", customerId, "customer not found");
        try {
            String number = bank.nextAccountNumber(savings ? "SA" : "CA");
            BankAccount account = savings
                    ? new SavingsAccount(number, c.getFullName(), 0.0)
                    : new CurrentAccount(number, c.getFullName(), 0.0);
            account.setOwnerCustomerId(c.getCustomerId());
            bank.addAccount(account);
            char[] temp = null;
            if (!c.hasPin()) {
                temp = PinSecurity.generateTemporaryPin();
                c.setPin(temp, true);
            }
            bank.save();
            AuditLogger.log(actor(s), "CREATE ACCOUNT", number + " for " + c.getCustomerId(),
                    "SUCCESS" + (temp != null ? " (temporary PIN issued)" : ""));
            return new AccountCreation(account, temp);
        } catch (InvalidTransactionException e) {
            throw fail(s, "CREATE ACCOUNT", customerId, e.getMessage());
        }
    }


    public List<AccountSummary> listAll(AdminSession s) {
        requireAdmin(s);
        List<AccountSummary> rows = new ArrayList<>();
        for (Customer c : bank.allCustomers()) {
            rows.addAll(summariesFor(c));
        }
        AuditLogger.log(actor(s), "VIEW ALL ACCOUNTS", "-", "SUCCESS");
        return rows;
    }

    public List<AccountSummary> search(AdminSession s, String query) {
        requireAdmin(s);
        List<AccountSummary> rows = new ArrayList<>();
        String q = query == null ? "" : query.trim();
        if (!q.isEmpty()) {
            for (Customer c : bank.allCustomers()) {
                boolean idMatch = c.getCustomerId().equalsIgnoreCase(q);
                boolean nameMatch = c.getFullName().toLowerCase().contains(q.toLowerCase());
                boolean accMatch = false;
                for (BankAccount a : bank.getAccountsOf(c.getCustomerId())) {
                    if (a.getAccountNumber().equalsIgnoreCase(q)) accMatch = true;
                }
                if (idMatch || nameMatch || accMatch) {
                    if (accMatch && !idMatch && !nameMatch) {
                        for (BankAccount a : bank.getAccountsOf(c.getCustomerId())) {
                            if (a.getAccountNumber().equalsIgnoreCase(q)) rows.add(summary(c, a));
                        }
                    } else {
                        rows.addAll(summariesFor(c));
                    }
                }
            }
        }
        AuditLogger.log(actor(s), "SEARCH", q, "SUCCESS (" + rows.size() + " result(s))");
        return rows;
    }

    private List<AccountSummary> summariesFor(Customer c) {
        List<AccountSummary> rows = new ArrayList<>();
        List<BankAccount> owned = bank.getAccountsOf(c.getCustomerId());
        if (owned.isEmpty()) {
            rows.add(new AccountSummary(c.getCustomerId(), c.getFullName(), c.getPhone(), "-", "-", "-",
                    c.getPinStatus(), null));
        }
        for (BankAccount a : owned) rows.add(summary(c, a));
        return rows;
    }

    private AccountSummary summary(Customer c, BankAccount a) {
        return new AccountSummary(c.getCustomerId(), c.getFullName(), c.getPhone(), a.getAccountNumber(),
                a.getAccountType(), a.getStatus(), c.getPinStatus(), a.getDateOpened());
    }

    public void updateCustomer(AdminSession s, String customerId, String name, String phone, String address)
            throws AdministrationException {
        requireAdmin(s);
        Customer c = bank.findCustomer(customerId);
        if (c == null) throw fail(s, "UPDATE CUSTOMER", customerId, "customer not found");
        String newName = name.isBlank() ? c.getFullName() : name.trim();
        String newPhone = phone.isBlank() ? c.getPhone() : phone.trim();
        String newAddress = address.isBlank() ? c.getAddress() : address.trim();
        String problem = validateDetails(newName, newPhone, newAddress);
        if (problem != null) throw fail(s, "UPDATE CUSTOMER", customerId, problem);
        c.updateDetails(newName, newPhone, newAddress);
        for (BankAccount a : bank.getAccountsOf(c.getCustomerId())) {
            a.setAccountHolderName(newName);
        }
        bank.save();
        AuditLogger.log(actor(s), "UPDATE CUSTOMER", c.getCustomerId(), "SUCCESS");
    }

    public char[] resetPin(AdminSession s, String customerId) throws AdministrationException {
        requireAdmin(s);
        Customer c = bank.findCustomer(customerId);
        if (c == null) throw fail(s, "RESET PIN", customerId, "customer not found");
        char[] temp = PinSecurity.generateTemporaryPin();
        c.setPin(temp, true);
        c.unlock();
        bank.save();
        AuditLogger.log(actor(s), "RESET PIN", c.getCustomerId(), "SUCCESS (customer must change PIN at next login)");
        return temp;
    }


    public void setFrozen(AdminSession s, String accountNumber, boolean freeze) throws AdministrationException {
        requireAdmin(s);
        String action = freeze ? "FREEZE ACCOUNT" : "UNFREEZE ACCOUNT";
        BankAccount a = bank.findAccount(accountNumber);
        if (a == null) throw fail(s, action, accountNumber, "account not found");
        if (a.isClosed()) throw fail(s, action, a.getAccountNumber(), "account is closed");
        if (freeze && a.isFrozen()) throw fail(s, action, a.getAccountNumber(), "account is already frozen");
        if (!freeze && !a.isFrozen()) throw fail(s, action, a.getAccountNumber(), "account is not frozen");
        a.setFrozen(freeze);
        bank.save();
        AuditLogger.log(actor(s), action, a.getAccountNumber(), "SUCCESS");
    }

    public void closeAccount(AdminSession s, String accountNumber) throws AdministrationException {
        requireAdmin(s);
        BankAccount a = bank.findAccount(accountNumber);
        if (a == null) throw fail(s, "CLOSE ACCOUNT", accountNumber, "account not found");
        String no = a.getAccountNumber();
        if (a.isClosed()) throw fail(s, "CLOSE ACCOUNT", no, "account is already closed");
        if (a.isFrozen()) throw fail(s, "CLOSE ACCOUNT", no, "account is frozen; unfreeze it before closing");
        if (Math.abs(a.getBalance()) >= 0.005) {
            throw fail(s, "CLOSE ACCOUNT", no, "balance must be exactly M0.00 before closure "
                    + "(the customer must clear the account first)");
        }
        a.markClosed();
        bank.save();
        AuditLogger.log(actor(s), "CLOSE ACCOUNT", no, "SUCCESS");
    }


    private String validateDetails(String name, String phone, String address) {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 60) {
            return "name must be 2-60 characters";
        }
        if (phone == null || !phone.trim().matches("\\+?[0-9 ]{7,15}")) {
            return "phone number must be 7-15 digits (spaces and a leading + allowed)";
        }
        if (address != null && address.trim().length() > 100) return "address is too long (max 100)";
        for (String field : new String[] {name, phone, address == null ? "" : address}) {
            if (field.contains("|") || field.matches("(?s).*\\p{Cntrl}.*")) {
                return "text may not contain '|' or control characters";
            }
        }
        return null;
    }
}
