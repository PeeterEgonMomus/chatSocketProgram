package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.*;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

/**
 * Design choice:
 * Handles GAME_INVITE frames at the protocol layer.
 *
 * This class acts as a thin adapter between:
 * - Binary protocol frames
 * - Application service layer (GameService)
 *
 * Responsibilities:
 * - Decode opponent username
 * - Decode game name
 * - Delegate invite logic to GameService
 *
 * It intentionally contains no validation or domain logic.
 * All validation and error handling occurs in GameService.
 *
 * This maintains strict layering:
 *
 * Transport Layer  →  Service Layer  →  Domain Layer
 *
 * Prevents protocol code from leaking business logic.
 */
public class GameInviteHandler implements FrameHandler {

    private final GameService gameService;

    public GameInviteHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public FrameType type() {
        return FrameType.GAME_INVITE;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        var client = ctx.client();
        var frame = ctx.frame();

        var in = client.readStream(frame);

        String opponent = in.readUTF();
        String gameName = in.readUTF();

        gameService.invite(client, opponent, gameName);
    }
}