package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.GameService;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

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