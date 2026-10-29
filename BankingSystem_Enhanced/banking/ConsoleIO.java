package banking;

import java.io.Console;
import java.util.NoSuchElementException;
import java.util.Scanner;

// Console input helper.
public final class ConsoleIO.Value {

    public static class InputClosedException extends RuntimeException {
        public InputClosedException() {
            super("Input stream closed.");
        }
    }

    private static final Scanner SCANNER = new Scanner(System.in);
    private static boolean fallbackWarningShown = false;

    private ConsoleIO() {
    }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return SCANNER.nextLine().trim();
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        }
    }

    public static int readMenuChoice() {
        try {
            String line = SCANNER.nextLine().trim();
            return Integer.parseInt(line);
        } catch (NoSuchElementException e) {
            throw new InputClosedException();
        } catch (NumberFormatException e) {
            System.out.println("Invalid input: please enter a number.");
            return -1;
        }
    }

    public static double readAmount(String prompt) {
        while (true) {
            String text = readLine(prompt);
            try {
                double value = Double.parseDouble(text);
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    throw new NumberFormatException();
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount: please enter a numeric value.");
            }
        }
    }

    public static char[] readSecret(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] secret = console.readPassword("%s", prompt);
            if (secret == null) {
                throw new InputClosedException();
            }
            return secret;
        }
        if (!fallbackWarningShown) {
            System.out.println();
            System.out.println("NOTICE: Hidden input is not supported in this environment (for example an IDE");
            System.out.println("        console or redirected input). What you type WILL BE VISIBLE.");
            System.out.println("        For hidden PIN entry, run the program from a normal terminal.");
            fallbackWarningShown = true;
        }
        return readLine(prompt).toCharArray();
    }
}
