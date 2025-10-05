package org.example.chat.auth;

import java.util.Optional;

public interface UserStore {
    boolean addUser(User user); // returns false if user already exists
    Optional<User> getUser(String username);
}
