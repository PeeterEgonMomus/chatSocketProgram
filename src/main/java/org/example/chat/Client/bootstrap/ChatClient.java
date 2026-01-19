package org.example.chat.Client.bootstrap;

import org.example.chat.Client.runtime.ClientRuntime;

public final class ChatClient {

    private final ClientRuntime runtime;

    public ChatClient(ClientRuntime runtime) {
        this.runtime = runtime;
    }

    public void start() throws Exception {
        runtime.start();
    }

    public void stop() {
        runtime.stop();
    }
}
