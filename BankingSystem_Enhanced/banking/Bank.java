package banking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;

// Bank data container.
public class Bank.Value {

    private final Map<String, Customer> customers = new LinkedHashMap<>();
    private final List<BankAccount> accounts = new ArrayList<>();
    private AdminCredential admin;
    private final DataStore store;

    private Bank(DataStore store) {
        this.store = store;
    }

    public static Bank loadOrCreate(String dataFolder) {
        Bank bank = new Bank(new DataStore(dataFolder));
        if (!bank.store.exists()) {
            bank.seedDemoAccounts();
            bank.save();
        } else {
            bank.store.load(bank);
        }
        return bank;
    }

    private void seedDemoAccounts() {
        try {
            seed("Andrias Nchabeng", new SavingsAccount("SA001", "Andrias Nchabeng", 2000.00));
            seed("Malefu Motlalehi", new SavingsAccount("SA002", "Malefu Motlalehi", 300.00));
            seed("Moseli Motopi", new CurrentAccount("CA001", "Moseli Motopi", 500.00));
            seed("Tshepo Nthlane", new CurrentAccount("CA002", "Tshepo Nthlane", 0.00));
            seed("Mahlape Ntoko", new CurrentAccount("CA003", "Mahlape Ntoko", 6000));
        } catch (InvalidTransactionException e) {
            System.out.println("Failed to set up demo accounts: " + e.getMessage());
        }
    }

    private void seed(String name, BankAccount account) {
        Customer customer = new Customer(nextCustomerId(), name, "", "");
        customers.put(customer.getCustomerId(), customer);
        account.setOwnerCustomerId(customer.getCustomerId());
        accounts.add(account);
    }


    public BankAccount findAccount(String accountNumber) {
        if (accountNumber == null) return null;
        for (BankAccount account : accounts) {
            if (account.getAccountNumber().equalsIgnoreCase(accountNumber.trim())) {
                return account;
            }
        }
        return null;
    }

    public Customer findCustomer(String customerId) {
        if (customerId == null) return null;
        for (Customer c : customers.values()) {
            if (c.getCustomerId().equalsIgnoreCase(customerId.trim())) return c;
        }
        return null;
    }

    public List<BankAccount> getAccountsOf(String customerId) {
        List<BankAccount> result = new ArrayList<>();
        for (BankAccount account : accounts) {
            if (customerId != null && customerId.equals(account.getOwnerCustomerId())) {
                result.add(account);
            }
        }
        return result;
    }

    List<BankAccount> allAccounts() { return Collections.unmodifiableList(accounts); }
    Collection<Customer> allCustomers() { return Collections.unmodifiableCollection(customers.values()); }


    void addCustomer(Customer customer) { customers.put(customer.getCustomerId(), customer); }
    void addAccount(BankAccount account) { accounts.add(account); }
    AdminCredential getAdmin() { return admin; }
    void setAdmin(AdminCredential admin) { this.admin = admin; }

    String nextCustomerId() {
        int max = 0;
        for (String id : customers.keySet()) {
            max = Math.max(max, numericPart(id));
        }
        return String.format("C%03d", max + 1);
    }

    String nextAccountNumber(String prefix) {
        int max = 0;
        for (BankAccount a : accounts) {
            if (a.getAccountNumber().toUpperCase().startsWith(prefix)) {
                max = Math.max(max, numericPart(a.getAccountNumber()));
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    private static int numericPart(String id) {
        try {
            return Integer.parseInt(id.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void save() {
        store.save(this);
    }
}
