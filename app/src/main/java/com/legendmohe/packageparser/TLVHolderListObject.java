package com.legendmohe.packageparser;

import com.legendmohe.packetparser.annotation.ParsePacket;

import java.util.List;

/**
 * Created by legendmohe on 2017/5/16.
 */
@ParsePacket({
        "tlvObject", // 字段可以是任意以@ParsePacket标注的类
        "c:4",
        "2a:4", // 字段名前的数字表示该字段出现的次数，包装在List中
        "[this.c > 0x02]*b", // “[]”中括号内的为条件解析语句，括号内的表达式为true才会解析该字段。
        // 表达式内可通过this引用任意public方法。
        // “*b”表示b可出现0次或多次，包装在List中。注意“*”仅可用于最后一个元素
})
public class TLVHolderListObject extends TLVHeaderObject {
    public TLVObject tlvObject;
    public int c;
    public List<Integer> a;
    public List<TLVObject> b;
}
