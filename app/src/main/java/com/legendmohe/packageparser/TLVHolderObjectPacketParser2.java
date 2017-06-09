package com.legendmohe.packageparser;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

public class TLVHolderObjectPacketParser2 {
    public static final TLVHeaderObject parse(byte[] src) {
        TLVHeaderObject srcObject = new TLVHeaderObject();
        parse(src, srcObject);
        return srcObject;
    }

    public static final int parse(byte[] bytes, TLVHeaderObject src) {
        int wrapStartPos = 0;
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, wrapStartPos, bytes.length - wrapStartPos);
        try {
            src.type = byteBuffer.get();
            src.length = byteBuffer.getShort();
        } catch (BufferUnderflowException ignore) {
        }
        ;
        return byteBuffer.position();
    }

    public static final byte[] toBytes(TLVHeaderObject src) {
        int bufferLen = TLVHeaderObjectPacketParser.parseLen(src);
        ByteBuffer byteBuffer = ByteBuffer.allocate(bufferLen);
        byteBuffer.put(src.type);
        byteBuffer.putShort(src.length);
        return byteBuffer.array();
    }

    public static final int parseLen(TLVHeaderObject src) {
        int bufferLen = 0;
        bufferLen += (1) + (2);
        return bufferLen;
    }
}