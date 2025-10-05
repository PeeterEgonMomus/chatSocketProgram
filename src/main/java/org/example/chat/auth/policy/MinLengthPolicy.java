package org.example.chat.auth.policy;

public class MinLengthPolicy implements PasswordPolicy {
    private final int minLength;

    public MinLengthPolicy(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public void validate(String password) throws PasswordPolicyException {
        if (password == null || password.length() < minLength) {
            throw new PasswordPolicyException("Password must be at least " + minLength + " characters long.");
        }
    }
}
