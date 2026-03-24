package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.*;
import org.example.chat.games.commands.GameAction;
import org.example.chat.games.commands.GameActionRegistry;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

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