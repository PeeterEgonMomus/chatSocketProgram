package org.example.chat.files;

import java.io.*;


/**
 * Design choice:
 * Codec for serializing/deserializing FileDescriptor.
 *
 * Keeps:
 * - Wire format separate
 * - Descriptor class immutable and clean
 *
 * Follows the same transport separation principle:
 *
 * Domain Object  ≠  Transport Format
 */
public final class FileDescriptorCodec {

    private FileDescriptorCodec() {}

    public static byte[] encode(FileDescriptor d) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        out.writeUTF(d.getId());
        out.writeUTF(d.getFilename());
        out.writeLong(d.getSize());
        out.writeUTF(d.getChecksumBase64());

        return baos.toByteArray();
    }

    public static FileDescriptor decode(byte[] payload) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));

        return new FileDescriptor(
                in.readUTF(),
                in.readUTF(),
                in.readLong(),
                in.readUTF()
        );
    }
}
