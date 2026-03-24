package org.example.chat.auth.policy;

/**
 * Design choice:
 * Rules remain stateless and composable.
 *
 * This enables flexible policy construction at runtime
 * without introducing conditional logic in the validator.
 */
public class MustContainNumberRule implements PasswordRule {

    @Override
    public void validate(String password) throws PasswordPolicyException {

        if (password == null || !password.matches(".*\\d.*")) {
            throw new PasswordPolicyException(
                    "Password must contain at least one number."
            );
        }
    }
}