package org.example.chat.Client.input;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemConsoleInputSource implements CommandInputSource {

    private final BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    @Override
    public String readLine() throws Exception {
        return reader.readLine();
    }
}
