package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

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