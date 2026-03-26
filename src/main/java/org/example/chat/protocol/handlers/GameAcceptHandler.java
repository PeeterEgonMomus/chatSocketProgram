package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.GameService;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;
import org.example.chat.protocol.*;

import java.io.DataInputStream;


/**
 * Design choice:
 * Handles GAME_ACCEPT frames.
 *
 * This class belongs strictly to the protocol handling layer.
 *
 * Responsibilities:
 * - Decode inviter username from frame
 * - Delegate accept logic to GameService
 *
 * It intentionally avoids:
 * - Direct interaction with GameManager
 * - Session creation logic
 *
 * This ensures:
 * - Protocol layer remains stateless
 * - Domain logic stays centralized
 *
 * Follows the Single Responsibility Principle.
 */
public class GameAcceptHandler implements FrameHandler {

    private final GameService gameService;

    public GameAcceptHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public FrameType type() {
        return FrameType.GAME_ACCEPT;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        var client = ctx.client();
        var in = client.readStream(ctx.frame());

        String inviter = in.readUTF();

        gameService.accept(client, inviter);
    }
}