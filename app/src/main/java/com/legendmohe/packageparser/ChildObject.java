package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

/**
 * Created by legendmohe on 16/8/25.
 */
@ParsePacket({
        "[this.len > 2]seq:2",
        "mac:6",
        "data:this.len-6",
        "check:1",
        "tail:1"
})
public class ChildObject extends ParentObject {
    public short seq;
    public byte[] mac;
    public byte[] data;
    public byte check;
    public byte tail;
}
