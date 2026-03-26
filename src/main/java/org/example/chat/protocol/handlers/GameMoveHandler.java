package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

/**
 * Design choice:
 * Protocol-layer handler responsible for processing GAME_MOVE frames.
 *
 * This class belongs to the transport/protocol layer and NOT the game domain.
 *
 * Responsibilities:
 * - Decode incoming frame payload
 * - Extract move string
 * - Delegate to GameManager
 *
 * It does NOT:
 * - Validate move correctness
 * - Resolve game rules
 * - Manage sessions
 *
 * Those responsibilities belong to:
 * - GameSession (state + resolution)
 * - Game (rule logic)
 *
 * This keeps networking strictly separated from business logic (SRP).
 *
 * Follows the Command Handler pattern at the protocol level.
 */
public class GameMoveHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.GAME_MOVE;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        DataInputStream in = ctx.client().readStream(ctx.frame());

        String move = in.readUTF();

        ctx.client()
                .getServer()
                .getGameManager()
                .submitMove(ctx.client(), move);
    }
}