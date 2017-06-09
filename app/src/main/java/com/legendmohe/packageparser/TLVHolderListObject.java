package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

import java.util.List;

/**
 * Created by legendmohe on 2017/5/16.
 */
@ParsePacket({
        "tlvObject",
        "2a:4",
        "1b",
        "c:4"
})
public class TLVHolderListObject extends TLVHeaderObject {
    public TLVObject tlvObject;
    public List<Integer> a;
    public List<TLVObject> b;
    public int c;
}
