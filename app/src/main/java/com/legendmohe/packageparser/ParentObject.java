package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

/**
 * Created by legendmohe on 16/8/25.
 */
@ParsePacket(
        "header:1|cmd:2|len:2"
)
public class ParentObject {
    public byte header;
    public short cmd;
    public short len;
}
