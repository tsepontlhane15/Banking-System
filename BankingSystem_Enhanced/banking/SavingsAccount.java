package banking;

// Savings account logic.
public class SavingsAccount.Value extends BankAccount implements Loanable {

    // Savings limits and account settings.
    public static final double MINIMUM_BALANCE = 100.00;
    public static final double INTEREST_RATE = 0.05;
    private static final double LOAN_LIMIT_MULTIPLIER = 3.0;

    // Create a new savings account.
    public SavingsAccount(String accountNumber, String accountHolderName, double openingBalance)
            throws InvalidTransactionException {
        super(accountNumber, accountHolderName, openingBalance);
    }

    // Restore a saved savings account.
    SavingsAccount(String accountNumber, String accountHolderName, double openingBalance,
            java.time.LocalDateTime dateOpened) throws InvalidTransactionException {
        super(accountNumber, accountHolderName, openingBalance, dateOpened);
    }

    // Deposit money when the amount is valid.
    @Override
    public void deposit(double amount) throws InvalidTransactionException {
        ensureOperational();
        if (amount <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero.");
        }
        credit(amount);
    }

    // Withdraw money without dropping below the minimum balance.
    @Override
    public void withdraw(double amount) throws InvalidTransactionException, InsufficientFundsException {
        ensureOperational();
        if (amount <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero.");
        }

        if (getBalance() - amount < MINIMUM_BALANCE) {
            throw new InsufficientFundsException(
                    "Withdrawal denied: SavingsAccount balance cannot fall below M"
                            + String.format("%.2f", MINIMUM_BALANCE) + ".");
        }
        debit(amount);
    }

    // Add interest for active savings accounts.
    @Override
    public double calculateInterest() {
        if (!isOperational()) {
            return 0.0;
        }
        double interest = getBalance() * INTEREST_RATE;
        credit(interest);
        return interest;
    }

    // Return the maximum loan amount for this account.
    @Override
    public double getLoanLimit() {
        return getBalance() * LOAN_LIMIT_MULTIPLIER;
    }

    // Apply the loan rules for this savings account.
    @Override
    public double requestLoan(double amount) throws InvalidLoanRequestException {
        if (!isOperational()) {
            throw new InvalidLoanRequestException("Loan denied: account is " + getStatus().toLowerCase() + ".");
        }
        if (amount <= 0) {
            throw new InvalidLoanRequestException("Loan amount must be greater than zero.");
        }
        if (amount > getLoanLimit()) {
            throw new InvalidLoanRequestException(
                    "Loan denied: requested M" + String.format("%.2f", amount)
                            + " exceeds this SavingsAccount's loan limit of M"
                            + String.format("%.2f", getLoanLimit()) + ".");
        }
        credit(amount);
        return amount;
    }

    // Return the account label shown to users.
    @Override
    public String getAccountType() {
        return "Savings Account";
    }

    // Print the savings details and rate.
    @Override
    public void displayAccountInfo() {
        super.displayAccountInfo();
        System.out.printf("   -> Minimum balance: M%,.2f | Interest rate: %.1f%%%n",
                MINIMUM_BALANCE, INTEREST_RATE * 100);
    }
}
