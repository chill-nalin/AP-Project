package edu.univ.erp.auth;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// A utility class that wraps jBCrypt to handle password hashing and verification.
public class PasswordHasher {

    private static final Logger log = LoggerFactory.getLogger(PasswordHasher.class);

    // Hashes a plain-text password using jBCrypt.
    public static String hash(String password) {
        log.debug("Hashing new password.");
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Checks if a plain-text password matches a stored hash.
    public static boolean checkPassword(String plainPassword, String storedHash) {
        log.debug("Checking password against stored hash.");
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid hash format provided for check.", e);
            return false;
        }
    }
}