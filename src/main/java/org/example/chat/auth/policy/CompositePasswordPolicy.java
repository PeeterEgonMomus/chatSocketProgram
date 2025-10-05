package org.example.chat.auth.policy;

import java.util.ArrayList;
import java.util.List;

public class CompositePasswordPolicy implements PasswordPolicy {
    private final List<PasswordPolicy> policies = new ArrayList<>();

    public CompositePasswordPolicy addPolicy(PasswordPolicy policy) {
        policies.add(policy);
        return this;
    }

    @Override
    public void validate(String password) throws PasswordPolicyException {
        for (PasswordPolicy policy : policies) {
            policy.validate(password);
        }
    }
}
