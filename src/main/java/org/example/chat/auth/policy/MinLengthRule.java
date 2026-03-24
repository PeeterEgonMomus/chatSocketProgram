package org.example.chat.auth.policy;

/**
 * Design choice:
 * Each rule is isolated and single-purpose (SRP),
 * making it independently testable and reusable.
 *
 * Adding new rules never affects this class → strong OCP compliance.
 */
public class MinLengthRule implements PasswordRule {

    private final int minLength;

    public MinLengthRule(int minLength) {
        this.minLength = minLength;
    }

    @Override
    public void validate(String password) throws PasswordPolicyException {

        if (password == null || password.length() < minLength) {
            throw new PasswordPolicyException(
                    "Password must be at least " + minLength + " characters long."
            );
        }
    }
}