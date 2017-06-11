package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

import java.util.List;

/**
 * Created by legendmohe on 2017/5/16.
 */
@ParsePacket({
        "tlvObject",
        "c:4",
        "2a:4",
        "[this.c > 0x02]*b",
})
public class TLVHolderListObject extends TLVHeaderObject {
    public TLVObject tlvObject;
    public List<Integer> a;
    public int c;
    public List<TLVObject> b;
}
