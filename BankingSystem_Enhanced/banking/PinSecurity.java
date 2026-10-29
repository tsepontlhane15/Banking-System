package banking;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

// PIN and password security.
public final class PinSecurity.Value {

    public static final int PIN_LENGTH = 4;
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PinSecurity() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] secret, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(secret, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available on this JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean matches(char[] secret, byte[] salt, byte[] expectedHash) {
        if (secret == null || salt == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(hash(secret, salt), expectedHash);
    }

    public static char[] generateTemporaryPin() {
        char[] pin;
        do {
            pin = new char[PIN_LENGTH];
            for (int i = 0; i < PIN_LENGTH; i++) {
                pin[i] = (char) ('0' + RANDOM.nextInt(10));
            }
        } while (validatePin(pin) != null);
        return pin;
    }

    public static String validatePin(char[] pin) {
        if (pin == null || pin.length != PIN_LENGTH) {
            return "PIN must be exactly " + PIN_LENGTH + " digits.";
        }
        for (char c : pin) {
            if (c < '0' || c > '9') {
                return "PIN must contain digits only.";
            }
        }
        boolean allSame = true, ascending = true, descending = true;
        for (int i = 1; i < pin.length; i++) {
            if (pin[i] != pin[0]) allSame = false;
            if (pin[i] != pin[i - 1] + 1) ascending = false;
            if (pin[i] != pin[i - 1] - 1) descending = false;
        }
        if (allSame || ascending || descending) {
            return "PIN is too easy to guess (e.g. 1111, 1234, 4321). Choose another.";
        }
        return null;
    }

    public static String validateAdminPassword(char[] password) {
        if (password == null || password.length < 8) {
            return "Password must be at least 8 characters long.";
        }
        boolean letter = false, digit = false;
        for (char c : password) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) {
            return "Password must contain at least one letter and one digit.";
        }
        return null;
    }

    public static void wipe(char[] secret) {
        if (secret != null) {
            Arrays.fill(secret, '\0');
        }
    }
}
