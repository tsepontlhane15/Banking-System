# LIBATA BANKING SYSTEM - security enhancement notes

## Compile and run
```bash
javac -d out banking/*.java BankingSystemMain.java AdminMenu.java
java -cp out BankingSystemMain
```
Run from a real terminal so PIN/password entry is hidden (`System.console().readPassword()`).
In an IDE console the program warns that input will be visible and continues.
Data is saved in `./data/` (customers.txt, accounts.txt, admin.txt); logs are `transactions.txt` and `audit.log`.

## First-time setup
1. Start the program -> 1 (Use Banking System) -> 2 (Administrator login). On the very first run you are asked to
   create the administrator username and password (min 8 chars, letter + digit). There is no built-in password.
   Change it later with admin menu option 10.
2. The original demo accounts (SA001, SA002, CA001, CA002, CA003) are migrated to customers C001-C005 with NO PIN.
   Use admin option 6 (Reset PIN) for each to issue a unique one-time temporary PIN. The customer must choose a new
   4-digit PIN at first login. Option 2 also issues a temporary PIN with a customer's first new account.
3. A customer is locked after 3 consecutive wrong PINs; the administrator unlocks by resetting the PIN.
   The administrator login locks for 5 minutes after 3 failures.

## Rules worth knowing
- Administrators cannot deposit/withdraw/transfer/request loans, set balances, or see PINs (only salted PBKDF2 hashes are stored).
- New accounts open with M0.00; customers fund them themselves.
- An account can be closed only if it is not frozen and its balance is exactly M0.00.
- Frozen or closed accounts reject deposits, withdrawals, transfers (in or out), interest and loans.

---

(Original README below)

# BIOP2110 — Console Banking System

A Java console application demonstrating object-oriented programming
(abstraction, encapsulation, inheritance, polymorphism, interfaces, and
exception handling) through a simple bank account management system.

## How to run

```bash
javac banking/*.java BankingSystemMain.java
java BankingSystemMain
```

This compiles everything and starts the menu. A `transactions.txt` file is
created (and appended to) automatically in the working directory the first
time you deposit, withdraw, transfer, calculate interest, or request a loan.

The app starts with four demo accounts already loaded so you can try every
menu option immediately:

| Account No | Holder            | Type    | Opening balance |
|------------|--------------------|---------|------------------|
| SA001      | Palesa Mokoena     | Savings | M1,500.00        |
| SA002      | Thabo Nkosi        | Savings | M300.00          |
| CA001      | Lerato Sefali      | Current | M500.00          |
| CA002      | Retsepile Mahao    | Current | M0.00            |

## Project structure

```
BankingSystem/
├── BankingSystemMain.java          <- separate main class, outside the package
└── banking/
    ├── BankAccount.java            <- abstract superclass
    ├── SavingsAccount.java         <- extends BankAccount, implements Loanable
    ├── CurrentAccount.java         <- extends BankAccount, implements Loanable
    ├── Loanable.java               <- interface for loan requests
    ├── TransactionLogger.java      <- final utility class, writes transactions.txt
    ├── InvalidTransactionException.java
    ├── InsufficientFundsException.java
    └── InvalidLoanRequestException.java
```

## Where each requirement is demonstrated

- **Abstraction** — `BankAccount` is `abstract` with abstract `deposit()`,
  `withdraw()`, and `calculateInterest()` methods that every subclass must
  implement its own way.
- **Encapsulation** — all fields in `BankAccount` are `private`; state is
  only reachable through the constructor, getters, a controlled
  `setAccountHolderName()`, and `protected` `credit()`/`debit()` helpers
  that subclasses use after validating an amount.
- **Inheritance** — `SavingsAccount` and `CurrentAccount` both `extend
  BankAccount`.
- **Polymorphism** — `BankingSystemMain` stores accounts as
  `List<BankAccount>`, and calls like `account.deposit(...)` or
  `account.displayAccountInfo()` run whichever subclass's overridden
  version applies, resolved at runtime.
- **Method overriding** — `deposit()`, `withdraw()`, `calculateInterest()`,
  `getAccountType()`, and `displayAccountInfo()` behave differently in
  `SavingsAccount` (minimum balance, 5% interest) vs `CurrentAccount`
  (overdraft limit, no interest).
- **Interface** — `Loanable` declares `requestLoan()`; both account types
  implement it with their own loan-limit rule (balance-based for savings,
  a flat cap for current accounts).
- **`final` class** — `TransactionLogger` is declared `final`: it's a
  stateless utility class with only static behaviour, so there's no reason
  for it to ever be subclassed.
- **Exception handling** — three custom checked exceptions
  (`InvalidTransactionException`, `InsufficientFundsException`,
  `InvalidLoanRequestException`) cover invalid amounts, business-rule
  violations, and bad loan requests. `BankingSystemMain` also catches
  `InputMismatchException`/`NumberFormatException` so bad menu or amount
  input never crashes the program.
- **File handling** — `TransactionLogger.log(...)` appends every attempted
  transaction (successful or failed) to `transactions.txt` with a
  timestamp, account number, type, amount, resulting balance, and result.
- **Package structure** — all banking classes live in the `banking`
  package; `BankingSystemMain` is a separate top-level class that imports
  from it.

## Notes on the business rules implemented

- Opening balance must be non-negative (enforced in the `BankAccount`
  constructor).
- Deposits/withdrawals must be greater than zero.
- `SavingsAccount` withdrawals may never take the balance below M100.00.
- `CurrentAccount` withdrawals may use up to M2,000.00 of overdraft.
- `SavingsAccount.calculateInterest()` credits 5% of the current balance.
- `CurrentAccount.calculateInterest()` always returns 0 (no interest).
- Loan limit for `SavingsAccount` = 3 × current balance.
- Loan limit for `CurrentAccount` = a flat M15,000.00.
  (These specific multipliers/caps aren't pinned down by the assignment
  brief, so they're reasonable placeholder rules — adjust the constants
  at the top of `SavingsAccount`/`CurrentAccount` if your lecturer wants
  different numbers.)
- Transfers reuse each account's own `withdraw()`/`deposit()`, so the
  source and destination accounts' individual rules are both respected.

## AI-use disclosure (edit before submitting)

Per the assignment's integrity rules, attach a short statement such as:

> "I used AI to help design the class structure and initial implementation
> of this banking system. I reviewed, tested, and can explain every part
> of the code, including the exception-handling and business-rule logic."

Fill in your group names in the assignment cover page, and be ready to
explain/extend the code live if asked, per the verification measures in
the brief.
