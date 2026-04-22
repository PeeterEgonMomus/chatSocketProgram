package org.example.chat.heartbeat;

import org.example.chat.ChatServer;
import org.example.chat.ClientHandler;
import org.example.chat.protocol.FrameType;
import org.example.chat.util.Logger;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically:
 * - Sends PING to all clients
 * - Disconnects inactive clients
 *
 * This is infrastructure-level logic.
 */
public class HeartbeatMonitor {

    private final ChatServer server;
    private final ScheduledExecutorService scheduler;

    private final long heartbeatIntervalMs;
    private final long timeoutMs;

    public HeartbeatMonitor(ChatServer server,
                            ScheduledExecutorService scheduler,
                            long heartbeatIntervalMs,
                            long timeoutMs) {
        this.server = server;
        this.scheduler = scheduler;
        this.heartbeatIntervalMs = heartbeatIntervalMs;
        this.timeoutMs = timeoutMs;
    }

    public void start() {

        scheduler.scheduleAtFixedRate(() -> {

            long now = System.currentTimeMillis();

            for (ClientHandler client : server.getAllClients()) {

                long last = client.getLastHeartbeat();

                if (now - last > timeoutMs) {
                    Logger.info("Disconnecting inactive client: " + client);
                    try {
                        client.getSocket().close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    continue;
                }

                try {
                    client.sendEncrypted(FrameType.PING, new byte[0]);
                } catch (Exception e) {
                    Logger.error("Heartbeat failed for " + client, e);
                }
            }

        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
    }
}