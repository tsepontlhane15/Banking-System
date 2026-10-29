package banking;

import java.time.LocalDateTime;

// Bank account base class.
public abstract class BankAccount.Value {

    private final String accountNumber;
    private String accountHolderName;
    private double balance;
    private final LocalDateTime dateOpened;

    private String ownerCustomerId;
    private boolean frozen;
    private boolean closed;

    protected BankAccount(String accountNumber, String accountHolderName, double openingBalance)
            throws InvalidTransactionException {
        this(accountNumber, accountHolderName, openingBalance, LocalDateTime.now());
    }

    protected BankAccount(String accountNumber, String accountHolderName, double openingBalance,
                          LocalDateTime dateOpened) throws InvalidTransactionException {

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new InvalidTransactionException("Account number cannot be empty.");
        }
        if (accountHolderName == null || accountHolderName.isBlank()) {
            throw new InvalidTransactionException("Account holder name cannot be empty.");
        }
        if (openingBalance < 0) {
            throw new InvalidTransactionException("Opening balance must be non-negative.");
        }

        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = openingBalance;
        this.dateOpened = dateOpened;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public double getBalance() {
        return balance;
    }

    public LocalDateTime getDateOpened() {
        return dateOpened;
    }

    public String getOwnerCustomerId() {
        return ownerCustomerId;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isOperational() {
        return !frozen && !closed;
    }

    public String getStatus() {
        return closed ? "CLOSED" : (frozen ? "FROZEN" : "ACTIVE");
    }

    void setOwnerCustomerId(String ownerCustomerId) {
        this.ownerCustomerId = ownerCustomerId;
    }

    void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    void markClosed() {
        this.closed = true;
    }

    void restoreState(double balance, boolean frozen, boolean closed) {
        this.balance = balance;
        this.frozen = frozen;
        this.closed = closed;
    }

    protected void ensureOperational() throws InvalidTransactionException {
        if (closed) {
            throw new InvalidTransactionException("Account " + accountNumber + " is closed.");
        }
        if (frozen) {
            throw new InvalidTransactionException("Account " + accountNumber
                    + " is frozen. Please contact the bank.");
        }
    }

    public void setAccountHolderName(String accountHolderName) {
        if (accountHolderName == null || accountHolderName.isBlank()) {
            throw new IllegalArgumentException("Account holder name cannot be empty.");
        }
        this.accountHolderName = accountHolderName;
    }

    protected void credit(double amount) {
        this.balance += amount;
    }

    protected void debit(double amount) {
        this.balance -= amount;
    }

    public abstract void deposit(double amount) throws InvalidTransactionException;

    public abstract void withdraw(double amount) throws InvalidTransactionException, InsufficientFundsException;

    public abstract double calculateInterest();

    public void transferTo(BankAccount destination, double amount)
            throws InvalidTransactionException, InsufficientFundsException {

        if (destination == null) {
            throw new InvalidTransactionException("Destination account does not exist.");
        }
        if (destination == this) {
            throw new InvalidTransactionException("Cannot transfer to the same account.");
        }

        this.ensureOperational();
        destination.ensureOperational();

        this.withdraw(amount);

        try {
            destination.deposit(amount);
        } catch (InvalidTransactionException e) {
            this.credit(amount);
            throw e;
        }
    }

    public void displayAccountInfo() {
        System.out.printf(
                "Account No: %-10s | Holder: %-20s | Type: %-15s | Balance: M%,.2f%n",
                accountNumber, accountHolderName, getAccountType(), balance);
    }

    public abstract String getAccountType();

    @Override
    public String toString() {
        return getAccountType() + " [" + accountNumber + "] " + accountHolderName
                + " - Balance: M" + String.format("%.2f", balance);
    }
}
