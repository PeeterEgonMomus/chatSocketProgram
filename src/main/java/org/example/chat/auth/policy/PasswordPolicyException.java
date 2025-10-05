package org.example.chat.auth.policy;

public class PasswordPolicyException extends Exception {
    public PasswordPolicyException(String message) {
        super(message);
    }
}