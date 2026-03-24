package org.example.chat.auth.policy;

import java.util.ArrayList;
import java.util.List;

/**
 * Design choice:
 * This implements the Composite Pattern.
 *
 * Instead of hardcoding validation logic or chaining rules manually,
 * we treat a group of rules as a single rule.
 *
 * Key benefits:
 * - OCP: new rules can be added without modifying this class
 * - Flexible composition: policies can be built dynamically
 * - Better UX: aggregates all validation errors instead of failing fast
 */
public class CompositePasswordRule implements PasswordRule {

    private final List<PasswordRule> rules = new ArrayList<>();

    /**
     * Design choice:
     * Fluent API for readability when constructing policies.
     *
     * Example:
     * new CompositePasswordRule()
     *     .addRule(new MinLengthRule(8))
     *     .addRule(new MustContainNumberRule());
     */
    public CompositePasswordRule addRule(PasswordRule rule) {

        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        rules.add(rule);
        return this;
    }

    @Override
    public void validate(String password) throws PasswordPolicyException {

        List<String> errors = new ArrayList<>();

        for (PasswordRule rule : rules) {
            try {
                rule.validate(password);
            } catch (PasswordPolicyException e) {
                // Design choice:
                // Collect all errors instead of failing fast,
                // improving feedback quality for the user.
                errors.add(e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new PasswordPolicyException(String.join(", ", errors));
        }
    }
}