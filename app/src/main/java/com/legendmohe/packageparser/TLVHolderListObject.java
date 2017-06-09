package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

import java.util.List;

/**
 * Created by legendmohe on 2017/5/16.
 */
@ParsePacket({
        "tlvObject",
        "a:4",
        "b:1"
})
public class TLVHolderListObject extends TLVHeaderObject {
    public TLVObject tlvObject;
    public List<Integer> a;
    public List<TLVObject> b;
}
