package banking;

import java.util.List;

// Transaction service.
public class TransactionService.Value {

    private final Bank bank;

    public TransactionService(Bank bank) {
        this.bank = bank;
    }

    private void requireSession(CustomerSession s) {
        if (s == null || !s.isActive()) {
            throw new SecurityException("You must be logged in to perform this action.");
        }
    }

    private void requireOwner(CustomerSession s, BankAccount a) {
        requireSession(s);
        if (a == null || !s.getCustomerId().equals(a.getOwnerCustomerId())) {
            AuditLogger.log("CUSTOMER:" + s.getCustomerId(), "ACCESS ACCOUNT",
                    a == null ? "-" : a.getAccountNumber(), "FAILED - access denied (not the owner)");
            throw new SecurityException("Access denied: that account does not belong to you.");
        }
    }


    public List<BankAccount> getOwnAccounts(CustomerSession s) {
        requireSession(s);
        return bank.getAccountsOf(s.getCustomerId());
    }

    public BankAccount findOwnAccount(CustomerSession s, String accountNumber) {
        requireSession(s);
        BankAccount a = bank.findAccount(accountNumber);
        if (a == null) return null;
        if (!s.getCustomerId().equals(a.getOwnerCustomerId())) {
            AuditLogger.log("CUSTOMER:" + s.getCustomerId(), "ACCESS ACCOUNT", a.getAccountNumber(),
                    "FAILED - access denied (not the owner)");
            return null;
        }
        return a;
    }

    public BankAccount findTransferDestination(CustomerSession s, String accountNumber) {
        requireSession(s);
        return bank.findAccount(accountNumber);
    }


    public Receipt deposit(CustomerSession s, BankAccount account, double amount) throws InvalidTransactionException {
        requireOwner(s, account);
        try {
            account.deposit(amount);
            Receipt r = new Receipt("DEPOSIT", amount, account.getAccountNumber(), null, account.getBalance(),
                    Double.NaN, String.format("Your deposit of M%,.2f was completed successfully.", amount));
            TransactionLogger.log(account.getAccountNumber(), "DEPOSIT", amount, account.getBalance(),
                    "SUCCESS | Ref: " + r.getReference());
            bank.save();
            return r;
        } catch (InvalidTransactionException e) {
            TransactionLogger.log(account.getAccountNumber(), "DEPOSIT", amount, account.getBalance(), "FAILED - " + e.getMessage());
            throw e;
        }
    }

    public Receipt withdraw(CustomerSession s, BankAccount account, double amount)
            throws InvalidTransactionException, InsufficientFundsException {
        requireOwner(s, account);
        try {
            account.withdraw(amount);
            Receipt r = new Receipt("WITHDRAWAL", amount, account.getAccountNumber(), null, account.getBalance(),
                    Double.NaN, String.format("Your withdrawal of M%,.2f was completed successfully.", amount));
            TransactionLogger.log(account.getAccountNumber(), "WITHDRAW", amount, account.getBalance(),
                    "SUCCESS | Ref: " + r.getReference());
            bank.save();
            return r;
        } catch (InvalidTransactionException | InsufficientFundsException e) {
            TransactionLogger.log(account.getAccountNumber(), "WITHDRAW", amount, account.getBalance(), "FAILED - " + e.getMessage());
            throw e;
        }
    }

    public Receipt transfer(CustomerSession s, BankAccount from, BankAccount to, double amount)
            throws InvalidTransactionException, InsufficientFundsException {
        requireOwner(s, from);
        try {
            from.transferTo(to, amount);

            boolean ownDestination = s.getCustomerId().equals(to.getOwnerCustomerId());
            Receipt r = new Receipt("TRANSFER", amount, from.getAccountNumber(), to.getAccountNumber(),
                    from.getBalance(), ownDestination ? to.getBalance() : Double.NaN,
                    String.format("Your transfer of M%,.2f to account %s was completed successfully.",
                            amount, to.getAccountNumber()));

            TransactionLogger.log(from.getAccountNumber(), "TRANSFER OUT", amount, from.getBalance(),
                    "SUCCESS -> " + to.getAccountNumber() + " | Ref: " + r.getReference());
            TransactionLogger.log(to.getAccountNumber(), "TRANSFER IN", amount, to.getBalance(),
                    "SUCCESS <- " + from.getAccountNumber() + " | Ref: " + r.getReference());
            bank.save();
            return r;
        } catch (InvalidTransactionException | InsufficientFundsException e) {
            TransactionLogger.log(from.getAccountNumber(), "TRANSFER OUT", amount, from.getBalance(), "FAILED - " + e.getMessage());
            throw e;
        }
    }

    public double calculateInterest(CustomerSession s, BankAccount account) throws InvalidTransactionException {
        requireOwner(s, account);
        if (!account.isOperational()) {
            String reason = "Interest not applied: account is " + account.getStatus().toLowerCase() + ".";
            TransactionLogger.log(account.getAccountNumber(), "INTEREST", Double.NaN, account.getBalance(), "FAILED - " + reason);
            throw new InvalidTransactionException(reason);
        }
        double interest = account.calculateInterest();
        TransactionLogger.log(account.getAccountNumber(), "INTEREST", interest, account.getBalance(), "SUCCESS");
        bank.save();
        return interest;
    }

    public double requestLoan(CustomerSession s, Loanable loanable, BankAccount account, double amount)
            throws InvalidLoanRequestException {
        requireOwner(s, account);
        try {
            double disbursed = loanable.requestLoan(amount);
            TransactionLogger.log(account.getAccountNumber(), "LOAN", amount, account.getBalance(), "SUCCESS");
            bank.save();
            return disbursed;
        } catch (InvalidLoanRequestException e) {
            TransactionLogger.log(account.getAccountNumber(), "LOAN", amount, account.getBalance(), "FAILED - " + e.getMessage());
            throw e;
        }
    }
}
