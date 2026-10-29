package banking;

// Loan contract.
public interface Loanable.Value {

    double requestLoan(double amount) throws InvalidLoanRequestException;

    double getLoanLimit();
}
