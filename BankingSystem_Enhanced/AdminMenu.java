import banking.AdminService;
import banking.AdminSession;
import banking.AdministrationException;
import banking.AuthService;
import banking.Bank;
import banking.ConsoleIO;
import banking.Customer;
import banking.PinSecurity;

import java.util.List;

// Admin menu.
public final class AdminMenu.Value {

    private AdminMenu() {
    }

    // Start the admin login flow.
    static boolean run(AuthService auth, Bank bank) {
        System.out.println("\n--- Administrator Login ---");
        if (!auth.isAdminConfigured() && !firstTimeSetup(auth)) {
            return false;
        }
        String username = ConsoleIO.readLine("Administrator username: ");
        char[] password = ConsoleIO.readSecret("Administrator password: ");
        AuthService.AdminLoginResult result = auth.loginAdmin(username, password);
        PinSecurity.wipe(password);
        System.out.println(result.message);
        if (!result.success) {
            return false;
        }
        menu(auth, new AdminService(bank), result.session);
        return true;
    }

    // Create the first admin account if needed.
    private static boolean firstTimeSetup(AuthService auth) {
        System.out.println("No administrator account exists yet. Create it now (first-time setup).");
        System.out.println("Password: at least 8 characters, with at least one letter and one digit.");
        String username = ConsoleIO.readLine("Choose administrator username: ");
        char[] first = ConsoleIO.readSecret("Choose password: ");
        char[] second = ConsoleIO.readSecret("Confirm password: ");
        String error = java.util.Arrays.equals(first, second)
                ? auth.setUpAdmin(username, first) : "The two passwords do not match.";
        PinSecurity.wipe(first);
        PinSecurity.wipe(second);
        if (error != null) {
            System.out.println("Setup failed: " + error);
            return false;
        }
        System.out.println("Administrator account created. Please log in with it now.");
        return true;
    }

    // Show the admin menu and handle choices.
    private static void menu(AuthService auth, AdminService admin, AdminSession session) {
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("------------- ADMINISTRATOR MENU -------------");
            System.out.println("1. Register new customer");
            System.out.println("2. Create Savings / Current account for a customer");
            System.out.println("3. View all customer accounts");
            System.out.println("4. Search for a customer account");
            System.out.println("5. Update customer information");
            System.out.println("6. Reset a customer's PIN");
            System.out.println("7. Freeze / unfreeze an account");
            System.out.println("8. Close an account");
            System.out.println("9. Log out (return to welcome menu)");
            System.out.println("10. Change administrator password");
            System.out.print("Enter your choice (1-10): ");
            int choice = ConsoleIO.readMenuChoice();
            try {
                switch (choice) {
                    case 1 -> register(admin, session);
                    case 2 -> createAccount(admin, session);
                    case 3 -> printRows(admin.listAll(session));
                    case 4 -> printRows(admin.search(session, ConsoleIO.readLine("Enter account number, customer ID or name: ")));
                    case 5 -> update(admin, session);
                    case 6 -> resetPin(admin, session);
                    case 7 -> freeze(admin, session);
                    case 8 -> {
                        String no = ConsoleIO.readLine("Account number to close: ");
                        admin.closeAccount(session, no);
                        System.out.println("Account " + no.toUpperCase() + " has been closed.");
                    }
                    case 9 -> {
                        auth.logout(session);
                        System.out.println("Administrator logged out.");
                        running = false;
                    }
                    case 10 -> changePassword(auth, session);
                    default -> System.out.println("Invalid menu choice. Please enter a number between 1 and 10.");
                }
            } catch (AdministrationException e) {
                System.out.println("Action failed: " + e.getMessage());
            } catch (ConsoleIO.InputClosedException e) {
                throw e;
            } catch (Exception e) {
                System.out.println("Unexpected error: " + e.getMessage());
            }
        }
    }

    // Register a new customer.
    private static void register(AdminService admin, AdminSession s) throws AdministrationException {
        String name = ConsoleIO.readLine("Full name: ");
        String phone = ConsoleIO.readLine("Phone number: ");
        String address = ConsoleIO.readLine("Address (optional): ");
        Customer c = admin.registerCustomer(s, name, phone, address);
        System.out.println("Customer registered. Customer ID: " + c.getCustomerId());
        System.out.println("Next: create an account for this customer (option 2); a one-time temporary PIN is issued with the first account.");
    }

    // Create a savings or current account.
    private static void createAccount(AdminService admin, AdminSession s) throws AdministrationException {
        String id = ConsoleIO.readLine("Customer ID: ");
        String type = ConsoleIO.readLine("Account type (1 = Savings, 2 = Current): ");
        if (!type.equals("1") && !type.equals("2")) {
            System.out.println("Invalid account type. Enter 1 or 2.");
            return;
        }
        AdminService.AccountCreation created = admin.createAccount(s, id, type.equals("1"));
        System.out.println("Created " + created.account().getAccountType() + " " + created.account().getAccountNumber()
                + " for " + created.account().getAccountHolderName() + " (opening balance M0.00).");
        showTemporaryPin(created.tempPin());
    }

    private static void resetPin(AdminService admin, AdminSession s) throws AdministrationException {
        String id = ConsoleIO.readLine("Customer ID: ");
        showTemporaryPin(admin.resetPin(s, id));
        System.out.println("The account (if locked) has been unlocked. The old PIN was not and cannot be displayed.");
    }

    // Show the one-time temporary PIN.
    private static void showTemporaryPin(char[] tempPin) {
        if (tempPin == null) return;
        System.out.println("ONE-TIME TEMPORARY PIN: " + new String(tempPin));
        System.out.println("Give it to the customer privately. It is not stored and must be changed at first login.");
        PinSecurity.wipe(tempPin);
    }

    private static void update(AdminService admin, AdminSession s) throws AdministrationException {
        String id = ConsoleIO.readLine("Customer ID: ");
        System.out.println("Leave a field blank to keep its current value.");
        String name = ConsoleIO.readLine("New full name: ");
        String phone = ConsoleIO.readLine("New phone number: ");
        String address = ConsoleIO.readLine("New address: ");
        admin.updateCustomer(s, id, name, phone, address);
        System.out.println("Customer information updated.");
    }

    private static void freeze(AdminService admin, AdminSession s) throws AdministrationException {
        String no = ConsoleIO.readLine("Account number: ");
        String action = ConsoleIO.readLine("1 = Freeze, 2 = Unfreeze: ");
        if (action.equals("1")) {
            admin.setFrozen(s, no, true);
            System.out.println("Account " + no.toUpperCase() + " is now FROZEN.");
        } else if (action.equals("2")) {
            admin.setFrozen(s, no, false);
            System.out.println("Account " + no.toUpperCase() + " is now ACTIVE.");
        } else {
            System.out.println("Invalid choice. Enter 1 or 2.");
        }
    }

    private static void changePassword(AuthService auth, AdminSession s) {
        char[] current = ConsoleIO.readSecret("Current password: ");
        char[] first = ConsoleIO.readSecret("New password: ");
        char[] second = ConsoleIO.readSecret("Confirm new password: ");
        String error = java.util.Arrays.equals(first, second)
                ? auth.changeAdminPassword(s, current, first) : "The two passwords do not match.";
        PinSecurity.wipe(current);
        PinSecurity.wipe(first);
        PinSecurity.wipe(second);
        System.out.println(error == null ? "Administrator password changed." : error);
    }

    // Print customer/account search results.
    private static void printRows(List<AdminService.AccountSummary> rows) {
        if (rows.isEmpty()) {
            System.out.println("No matching customers or accounts found.");
            return;
        }
        System.out.println();
        System.out.printf("%-6s %-20s %-14s %-8s %-16s %-8s %-10s%n",
                "Cust", "Name", "Phone", "Account", "Type", "Status", "PIN");
        for (AdminService.AccountSummary r : rows) {
            System.out.printf("%-6s %-20s %-14s %-8s %-16s %-8s %-10s%n", r.customerId(), r.customerName(),
                    r.phone().isEmpty() ? "-" : r.phone(), r.accountNumber(), r.accountType(), r.status(), r.pinStatus());
        }
    }
}
