package org.example.chat.auth.policy;

/**
 * Design choice:
 * We define a minimal contract (validate) so rules remain interchangeable.
 *
 * This keeps the system open for extension (new rules)
 * without modifying existing validation logic (OCP).
 */
public interface PasswordRule {

    void validate(String password) throws PasswordPolicyException;

}