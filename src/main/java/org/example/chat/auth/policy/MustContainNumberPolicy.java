package org.example.chat.auth.policy;

public class MustContainNumberPolicy implements PasswordPolicy {
    @Override
    public void validate(String password) throws PasswordPolicyException {
        if (!password.matches(".*\\d.*")) {
            throw new PasswordPolicyException("Password must contain at least one number.");
        }
    }
}
