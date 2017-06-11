package com.legendmohe.packageparser;

import java.nio.ByteBuffer;
import java.util.ArrayList;

/*

        "tlvObject",
        "2a:4",
        "1b",
        "c:4"

 */
public class TLVHolderObjectPacketParser2 {
    public static final TLVHolderListObject parse(byte[] src) throws Exception {
        TLVHolderListObject srcObject = new TLVHolderListObject();
        parse(src, srcObject);
        return srcObject;
    }

    public static final int parse(byte[] bytes, TLVHolderListObject src) throws Exception {
        int wrapStartPos = 0;
        wrapStartPos = TLVHeaderObjectPacketParser.parse(bytes, src);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, wrapStartPos, bytes.length - wrapStartPos);
        byte[] tlvObjectBytes = new byte[byteBuffer.remaining()];
        byteBuffer.slice().get(tlvObjectBytes);
        TLVObject tlvObjectObject = TLVObjectPacketParser.parse(tlvObjectBytes);
        src.tlvObject = tlvObjectObject;
        byteBuffer.position(byteBuffer.position() + TLVObjectPacketParser.parseLen(tlvObjectObject));
        src.a = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            src.a.add(byteBuffer.getInt());
        }
        src.b = new ArrayList<>();
        for (int i = 0; i < 1; i++) {
            byte[] bBytes = new byte[byteBuffer.remaining()];
            byteBuffer.slice().get(bBytes);
            TLVObject bObject = TLVObjectPacketParser.parse(bBytes);
            src.b.add(bObject);
            byteBuffer.position(byteBuffer.position() + TLVObjectPacketParser.parseLen(bObject));
        }
        src.c = byteBuffer.getInt();
        return byteBuffer.position();
    }

    public static final byte[] toBytes(TLVHolderListObject src) throws Exception {
        if (src == null) return null;
        int bufferLen = TLVHolderListObjectPacketParser.parseLen(src);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferLen);
        byte[] parentBytes = TLVHeaderObjectPacketParser.toBytes(src);
        if (parentBytes != null) {
            byteBuffer.put(parentBytes);
        }
        if (src.tlvObject != null) {
            byteBuffer.put(TLVObjectPacketParser.toBytes(src.tlvObject));
        } else {
            byteBuffer.put(new byte[TLVObjectPacketParser.parseLen(src.tlvObject)]);
        }

        for (int i = 0; i < 2 && byteBuffer.hasRemaining(); i++) {
            byteBuffer.putInt(src.a.get(i));
        }

        for (int i = 0; i < 1; i++) {
            byteBuffer.put(TLVObjectPacketParser.toBytes(src.b.get(i)));
        }

        byteBuffer.putInt(src.c);

        return byteBuffer.array();
    }

    public static final int parseLen(TLVHolderListObject src) {
        if (src == null) return 0;
        int bufferLen = 0;
        for (int i = 0; i < 1; i++) {
            bufferLen += TLVObjectPacketParser.parseLen(src.b.get(i));
        }
        bufferLen += TLVObjectPacketParser.parseLen(src.tlvObject) + (4) * 2 + (4);
        bufferLen += TLVHeaderObjectPacketParser.parseLen(src);
        return bufferLen;
    }
}