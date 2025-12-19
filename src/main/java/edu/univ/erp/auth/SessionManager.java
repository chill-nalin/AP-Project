package edu.univ.erp.auth;

import edu.univ.erp.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicReference;


public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private static SessionManager instance;

    private final AtomicReference<User> currentUser = new AtomicReference<>();

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startSession(User user) {
        currentUser.set(user);
        log.info("Session started for user: {} (Role: {})", user.username(), user.role());
    }

    public void endSession() {
        User user = currentUser.getAndSet(null);
        if (user != null) {
            log.info("Session ended for user: {}", user.username());
        }
    }

    public User getCurrentUser() {
        return currentUser.get();
    }

    public boolean isUserLoggedIn() {
        return currentUser.get() != null;
    }
}