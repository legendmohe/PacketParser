package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

/**
 * Created by legendmohe on 2017/5/16.
 */
@ParsePacket("tlvObject|a:1")
public class TLVHolderHeaderObject extends TLVHeaderObject {
    public TLVObject tlvObject;
    public byte a;
}
