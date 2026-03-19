package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.games.*;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

public class GameInviteHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.GAME_INVITE;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        var client = ctx.client();
        var frame = ctx.frame();

        DataInputStream in = client.readStream(frame);

        String opponent = in.readUTF();
        String gameName = in.readUTF();

        var server = client.getServer();
        var registry = server.getGameRegistry();

        if (!registry.exists(gameName)) {
            client.send("Unknown game: " + gameName);
            return;
        }

        var target =
                client.sessions()
                        .getSessionByUsername(opponent)
                        .map(s -> s.getChatHandler())
                        .orElse(null);

        if (target == null) {
            client.send("Player not found: " + opponent);
            return;
        }

        Game game = registry.get(gameName);

        server.getGameManager()
                .invite(game, client, target);
    }
}