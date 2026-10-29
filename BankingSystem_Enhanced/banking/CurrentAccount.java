package banking;

// Current account logic.
public class
CurrentAccount.Value extends BankAccount implements Loanable {

    public static final double OVERDRAFT_LIMIT = 2000.00;
    private static final double LOAN_LIMIT = 15000.00;

    public CurrentAccount(String accountNumber, String accountHolderName, double openingBalance)
            throws InvalidTransactionException {
        super(accountNumber, accountHolderName, openingBalance);
    }

    CurrentAccount(String accountNumber, String accountHolderName, double openingBalance,
            java.time.LocalDateTime dateOpened) throws InvalidTransactionException {
        super(accountNumber, accountHolderName, openingBalance, dateOpened);
    }

    @Override
    public void deposit(double amount) throws InvalidTransactionException {
        ensureOperational();
        if (amount <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero.");
        }
        credit(amount);
    }

    @Override
    public void withdraw(double amount) throws InvalidTransactionException, InsufficientFundsException {
        ensureOperational();
        if (amount <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero.");
        }

        if (getBalance() - amount < -OVERDRAFT_LIMIT) {
            throw new InsufficientFundsException(
                    "Withdrawal denied: exceeds overdraft limit of M"
                            + String.format("%.2f", OVERDRAFT_LIMIT) + ".");
        }
        debit(amount);
    }

    @Override
    public double calculateInterest() {

        return 0.0;
    }

    @Override
    public double getLoanLimit() {
        return LOAN_LIMIT;
    }

    @Override
    public double requestLoan(double amount) throws InvalidLoanRequestException {
        if (!isOperational()) {
            throw new InvalidLoanRequestException("Loan denied: account is " + getStatus().toLowerCase() + ".");
        }
        if (amount <= 0) {
            throw new InvalidLoanRequestException("Loan amount must be greater than zero.");
        }
        if (amount > LOAN_LIMIT) {
            throw new InvalidLoanRequestException(
                    "Loan denied: requested M" + String.format("%.2f", amount)
                            + " exceeds this CurrentAccount's loan limit of M"
                            + String.format("%.2f", LOAN_LIMIT) + ".");
        }
        credit(amount);
        return amount;
    }

    @Override
    public String getAccountType() {
        return "Current Account";
    }

    @Override
    public void displayAccountInfo() {
        super.displayAccountInfo();
        System.out.printf("   -> Overdraft limit: M%,.2f | Interest: none%n", OVERDRAFT_LIMIT);
    }
}
