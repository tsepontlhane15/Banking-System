import banking.AuthService;
import banking.Bank;
import banking.BankAccount;
import banking.ConsoleIO;
import banking.CustomerSession;
import banking.Customer;
import banking.InsufficientFundsException;
import banking.InvalidLoanRequestException;
import banking.InvalidTransactionException;
import banking.Loanable;
import banking.PinSecurity;
import banking.Receipt;
import banking.TransactionService;

import java.util.List;

// Main app entry point.
public class BankingSystemMain {

    private static Bank bank;
    private static AuthService auth;
    private static TransactionService transactions;

    public static void main(String[] args) {
        bank = Bank.loadOrCreate("data");
        auth = new AuthService(bank);
        transactions = new TransactionService(bank);

        try {
            boolean running = true;
            while (running) {
                printWelcome();
                int choice = ConsoleIO.readMenuChoice();
                switch (choice) {
                    case 1 -> loginMenu();
                    case 2 -> {
                        System.out.println("Thank you for using LIBATA BANKING SYSTEM. Goodbye!");
                        running = false;
                    }
                    default -> System.out.println("Invalid menu choice. Please enter 1 or 2.");
                }
            }
        } catch (ConsoleIO.InputClosedException e) {
            System.out.println();
            System.out.println("Input closed. Exiting LIBATA BANKING SYSTEM. Goodbye!");
        }
    }

    // Show the welcome screen.
    private static void printWelcome() {
        System.out.println();
        System.out.println("=======================================");
        System.out.println("   WELCOME TO LIBATA BANKING SYSTEM");
        System.out.println("=======================================");
        System.out.println("1. Use Banking System");
        System.out.println("2. Exit");
        System.out.print("Enter your choice (1-2): ");
    }

    // Handle login choices.
    private static void loginMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("---------------- LOGIN ----------------");
            System.out.println("1. Customer login");
            System.out.println("2. Administrator login");
            System.out.println("3. Back to welcome menu");
            System.out.print("Enter your choice (1-3): ");
            switch (ConsoleIO.readMenuChoice()) {
                case 1 -> {
                    if (customerLogin()) back = true;
                }
                case 2 -> {
                    if (AdminMenu.run(auth, bank)) back = true;
                }
                case 3 -> back = true;
                default -> System.out.println("Invalid menu choice. Please enter a number between 1 and 3.");
            }
        }
    }

    private static boolean customerLogin() {
        System.out.println("\n--- Customer Login ---");
        String accountNumber = ConsoleIO.readLine("Account number: ");
        char[] pin = ConsoleIO.readSecret("PIN: ");
        AuthService.LoginResult result = auth.loginCustomer(accountNumber, pin);
        PinSecurity.wipe(pin);
        System.out.println(result.message);
        if (result.status != AuthService.Status.SUCCESS) {
            return false;
        }
        CustomerSession session = result.session;
        if (auth.mustChangePin(session) && !forcePinChange(session)) {
            auth.logout(session);
            System.out.println("You have been logged out.");
            return true;
        }
        customerMenu(session);
        return true;
    }

    private static boolean forcePinChange(CustomerSession session) {
        System.out.println("\nYour PIN is a temporary PIN issued by the bank. You must choose a new " + PinSecurity.PIN_LENGTH
                + "-digit PIN before continuing. (Press Enter on a blank PIN to cancel.)");
        while (true) {
            char[] first = ConsoleIO.readSecret("New PIN: ");
            if (first.length == 0) return false;
            char[] second = ConsoleIO.readSecret("Confirm new PIN: ");
            String error = java.util.Arrays.equals(first, second)
                    ? auth.setNewPin(session, first) : "The two PINs do not match.";
            PinSecurity.wipe(first);
            PinSecurity.wipe(second);
            if (error == null) {
                System.out.println("PIN changed successfully.");
                return true;
            }
            System.out.println(error);
        }
    }

    // Run the logged-in customer menu.
    private static void customerMenu(CustomerSession session) {
        Customer customer = bank.findCustomer(session.getCustomerId());
        boolean running = true;
        while (running) {
            printMenu(customer);
            int choice = ConsoleIO.readMenuChoice();

            try {
                switch (choice) {
                    case 1 -> displayAllAccounts(session);
                    case 2 -> depositMoney(session);
                    case 3 -> withdrawMoney(session);
                    case 4 -> transferMoney(session);
                    case 5 -> calculateInterest(session);
                    case 6 -> requestLoan(session);
                    case 7 -> displayAccountDetails(session);
                    case 8 -> {
                        auth.logout(session);
                        System.out.println("You have been logged out securely. Thank you for banking with us!");
                        running = false;
                    }
                    case 9 -> {
                        changePin(session);
                        if (!session.isActive()) running = false;
                    }
                    default -> System.out.println("Invalid menu choice. Please enter a number between 1 and 9.");
                }
            } catch (InvalidTransactionException | InsufficientFundsException | InvalidLoanRequestException e) {

                System.out.println("Transaction failed: " + e.getMessage());
            } catch (ConsoleIO.InputClosedException e) {
                throw e;
            } catch (SecurityException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {

                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    private static void printMenu(Customer customer) {
        System.out.println();
        System.out.println("----------------- MENU -----------------");
        System.out.println("Logged in as: " + customer.getFullName() + " (" + customer.getCustomerId() + ")");
        System.out.println("1. Display all accounts");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Transfer money");
        System.out.println("5. Calculate interest");
        System.out.println("6. Request bank loan");
        System.out.println("7. Display account details");
        System.out.println("8. Log out");
        System.out.println("9. Change PIN");
        System.out.print("Enter your choice (1-9): ");
    }

    private static BankAccount promptForOwnAccount(CustomerSession session, String prompt) {
        String accNo = ConsoleIO.readLine(prompt);
        BankAccount account = transactions.findOwnAccount(session, accNo);
        if (account == null) {
            System.out.println("You do not have an account with number: " + accNo);
        }
        return account;
    }

    private static void printAccount(BankAccount account) {
        account.displayAccountInfo();
        if (!account.isOperational()) {
            System.out.println("   -> Status: " + account.getStatus() + " (transactions are not permitted)");
        }
    }

    private static void displayAllAccounts(CustomerSession session) {
        List<BankAccount> own = transactions.getOwnAccounts(session);
        if (own.isEmpty()) {
            System.out.println("You have no accounts in the system.");
            return;
        }
        System.out.println("\n--- Your Accounts ---");

        for (BankAccount account : own) {
            printAccount(account);
        }
    }

    private static void depositMoney(CustomerSession session) throws InvalidTransactionException {
        BankAccount account = promptForOwnAccount(session, "Enter account number: ");
        if (account == null) return;

        double amount = ConsoleIO.readAmount("Enter deposit amount: M");
        Receipt receipt = transactions.deposit(session, account, amount);
        receipt.display();
    }

    private static void withdrawMoney(CustomerSession session) throws InvalidTransactionException, InsufficientFundsException {
        BankAccount account = promptForOwnAccount(session, "Enter account number: ");
        if (account == null) return;

        double amount = ConsoleIO.readAmount("Enter withdrawal amount: M");
        Receipt receipt = transactions.withdraw(session, account, amount);
        receipt.display();
    }

    private static void transferMoney(CustomerSession session) throws InvalidTransactionException, InsufficientFundsException {
        BankAccount from = promptForOwnAccount(session, "Enter source account number: ");
        if (from == null) return;
        String toNo = ConsoleIO.readLine("Enter destination account number: ");
        BankAccount to = transactions.findTransferDestination(session, toNo);
        if (to == null) {
            System.out.println("No account found with number: " + toNo);
            return;
        }

        double amount = ConsoleIO.readAmount("Enter transfer amount: M");
        Receipt receipt = transactions.transfer(session, from, to, amount);
        receipt.display();
    }

    private static void calculateInterest(CustomerSession session) throws InvalidTransactionException {
        BankAccount account = promptForOwnAccount(session, "Enter account number: ");
        if (account == null) return;

        double interest = transactions.calculateInterest(session, account);
        if (interest > 0) {
            System.out.printf("Interest of M%,.2f credited. New balance: M%,.2f%n", interest, account.getBalance());
        } else {
            System.out.println("This account type does not earn interest.");
        }
    }

    private static void requestLoan(CustomerSession session) throws InvalidLoanRequestException {
        BankAccount account = promptForOwnAccount(session, "Enter account number: ");
        if (account == null) return;

        if (!(account instanceof Loanable loanableAccount)) {
            System.out.println("This account type does not support loans.");
            return;
        }

        double amount = ConsoleIO.readAmount("Enter loan amount requested: M");
        double disbursed = transactions.requestLoan(session, loanableAccount, account, amount);
        System.out.printf("Loan approved for M%,.2f. New balance: M%,.2f%n", disbursed, account.getBalance());
    }

    private static void displayAccountDetails(CustomerSession session) {
        BankAccount account = promptForOwnAccount(session, "Enter account number: ");
        if (account == null) return;

        System.out.println("\n--- Account Details ---");
        printAccount(account);
        System.out.println("Account holder: " + account.getAccountHolderName());
        System.out.println("Date opened: " + account.getDateOpened());
    }

    private static void changePin(CustomerSession session) {
        System.out.println("\n--- Change PIN ---");
        char[] current = ConsoleIO.readSecret("Current PIN: ");
        char[] first = ConsoleIO.readSecret("New PIN: ");
        char[] second = ConsoleIO.readSecret("Confirm new PIN: ");
        String error = java.util.Arrays.equals(first, second)
                ? auth.changePin(session, current, first) : "The two new PINs do not match.";
        PinSecurity.wipe(current);
        PinSecurity.wipe(first);
        PinSecurity.wipe(second);
        System.out.println(error == null ? "PIN changed successfully." : error);
        if (!session.isActive()) {
            System.out.println("You have been logged out.");
        }
    }
}
