package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

/**
 * Created by legendmohe on 2017/5/16.
 */

@ParsePacket({
        "type:1",
        "length:2",
        "-value:this.length"
})
public class TLVHeaderObject {
    public byte type;
    public short length;
    public byte[] value;
}
