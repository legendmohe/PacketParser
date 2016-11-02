package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

/**
 * Created by legendmohe on 16/8/25.
 */
@ParsePacket(
        "header:1|cmd:2|len:2|[this.len > 2]seq:2|mac:6|data:this.len-6|check:1|tail:(this.len > 0 ? 1 : 0)"
)
public class TargetObject {
    public byte header;
    public short cmd;
    public short len;
    public short seq;
    public byte[] mac;
    public byte[] data;
    public byte check;
    public byte tail;
}
