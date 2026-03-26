package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.GameService;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

/**
 * Design choice:
 * Handles GAME_DECLINE frames from clients.
 *
 * Thin protocol adapter that:
 * - Extracts inviter username
 * - Delegates decline logic to GameService
 *
 * This class does not:
 * - Check invite validity
 * - Modify GameManager state directly
 *
 * All business rules are centralized inside GameService and GameManager.
 *
 * This enforces clean architectural boundaries
 * and prevents duplicated validation logic.
 */
public class GameDeclineHandler implements FrameHandler {

    private final GameService gameService;

    public GameDeclineHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public FrameType type() {
        return FrameType.GAME_DECLINE;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        var client = ctx.client();
        var in = client.readStream(ctx.frame());

        String inviter = in.readUTF();

        gameService.decline(client, inviter);
    }
}