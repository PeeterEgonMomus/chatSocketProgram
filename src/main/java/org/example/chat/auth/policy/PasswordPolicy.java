package org.example.chat.auth.policy;

public interface PasswordPolicy {
    void validate(String password) throws PasswordPolicyException;
}
