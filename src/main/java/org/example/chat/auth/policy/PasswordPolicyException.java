package org.example.chat.auth.policy;

/**
 * Design choice:
 * We use a domain-specific exception instead of generic exceptions
 * so validation failures are explicit and composable.
 *
 * This allows higher-level components (e.g. registration flow)
 * to handle password issues separately from other errors.
 */
public class PasswordPolicyException extends Exception {

    public PasswordPolicyException(String message) {
        super(message);
    }

}