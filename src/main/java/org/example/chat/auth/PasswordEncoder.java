package org.example.chat.auth;

public interface PasswordEncoder {

    String encode(String rawPassword);

    boolean verify(String rawPassword, String encodedPassword);

    boolean needsRehash(String encodedPassword);
}