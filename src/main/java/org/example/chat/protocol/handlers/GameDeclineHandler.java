package org.example.chat.protocol.handlers;

import org.example.chat.FrameHandler;
import org.example.chat.protocol.*;

import java.io.DataInputStream;

public class GameDeclineHandler implements FrameHandler {

    @Override
    public FrameType type() {
        return FrameType.GAME_DECLINE;
    }

    @Override
    public void handle(FrameContext ctx) throws Exception {

        var client = ctx.client();
        var frame = ctx.frame();

        DataInputStream in = client.readStream(frame);

        String inviter = in.readUTF();

        client.getServer()
                .getGameManager()
                .declineInvite(client, inviter);
    }
}