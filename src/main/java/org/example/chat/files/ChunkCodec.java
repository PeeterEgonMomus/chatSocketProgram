package org.example.chat.files;

import java.io.*;

public final class ChunkCodec {

    private ChunkCodec() {}

    public static byte[] encode(String transferId, int index, byte[] data, int len)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(transferId);
        out.writeInt(index);
        out.writeInt(len);
        out.write(data, 0, len);

        return baos.toByteArray();
    }
}
