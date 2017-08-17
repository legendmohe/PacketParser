这个工具通过自动生成解析类，实现了字节数组和对象之间的转换。

# 1. 0.40更新

版本0.40发布，更新内容如下所示：

1. 支持继承
2. 支持对象作为字段
3. 支持条件解析字段
4. 支持忽略字段
5. 支持对象List

使用0.40版本，你可以做到：

以TLV格式的数据为例，首先定义一个基类：

    @ParsePacket({
            "type:1",
            "length:2",
            "~value:this.length" // ~表示解析该字段但不会移动buffer的cursor
    })
    public class TLVHeaderObject {
        public byte type;
        public short length;
        public byte[] value;
    }

再定义一个普通的TLV类：

    @ParsePacket({
        "type:1",
        "length:2",
        "value:this.length"
    })
    public class TLVObject {
        public byte type;
        public short length;
        public byte[] value;
    }

对于任意一个TLV协议单元，可以定义一个类继承TLVHeaderObject：

    @ParsePacket({
            "tlvObject", // 字段可以是任意以@ParsePacket标注的类，无需指定数据长度
            "c:4",
            "(2)a:4", // 字段名前括号包裹部分的数字（可以为表达式）表示该字段出现的次数，包装在List中
            "[this.c > 0x02]*b", // “[]”中括号内的为条件解析语句，括号内的表达式为true才会解析该字段。
                                 // 表达式内可通过this引用任意public方法。
                                 // “*b”表示b可出现0次或多次，包装在List中。注意“*”仅可用于最后一个元素
    })
    public class TLVHolderListObject extends TLVHeaderObject /* 会先解析父类 */ {
        public TLVObject tlvObject;
        public int c;
        public List<Integer> a;
        public List<TLVObject> b;
    }

留意代码中注释标注出的新特性。

编译项目后，可以通过如下测试：

    byte[] bytes = hexToBytes("0a000fBB0003010101000000030000000100000002BB0003010100BB000101BB00020101");

    // 使用生成的TLVHolderListObjectPacketParser解析字节数组
    TLVHolderListObject tlvHolderListObject = TLVHolderListObjectPacketParser.parse(bytes);

    // 检查TLV头部，即继承TLVHeaderObject的部分
    assertEquals((byte) 0x0A, tlvHolderListObject.type);
    assertEquals(0x000f, tlvHolderListObject.length);

    // 检查对象作为字段
    assertEquals((byte) 0xBB, tlvHolderListObject.tlvObject.type);
    assertEquals(0x0003, tlvHolderListObject.tlvObject.length);
    assertArrayEquals(new byte[]{0x01, 0x01, 0x01}, tlvHolderListObject.tlvObject.value);

    // 检查普通字段
    assertEquals(0x3, tlvHolderListObject.c);

    // 检查列表字段
    assertEquals(Integer.valueOf(0x1), tlvHolderListObject.a.get(0));
    assertEquals(Integer.valueOf(0x2), tlvHolderListObject.a.get(1));

    // 依次检查列表对象
    assertEquals(3, tlvHolderListObject.b.size());

    TLVObject b0 = tlvHolderListObject.b.get(0);
    assertEquals((byte) 0xBB, b0.type);
    assertEquals(0x0003, b0.length);
    assertArrayEquals(new byte[]{0x01, 0x01, 0x00}, b0.value);

    TLVObject b1 = tlvHolderListObject.b.get(1);
    assertEquals((byte) 0xBB, b1.type);
    assertEquals(0x0001, b1.length);
    assertArrayEquals(new byte[]{0x01}, b1.value);

    TLVObject b2 = tlvHolderListObject.b.get(2);
    assertEquals((byte) 0xBB, b2.type);
    assertEquals(0x0002, b2.length);
    assertArrayEquals(new byte[]{0x01, 0x01}, b2.value);

    // 检查对象转字节数组
    byte[] toBytes = TLVHolderListObjectPacketParser.toBytes(tlvHolderListObject);
    assertArrayEquals(bytes, toBytes);

# 2. 介绍

## 2.1. 使用@ParsePacket注解标注实体类

    @ParsePacket(
        "header:1", // 需要按照解析的顺序写
        "cmd:2", // 格式为：[字段名:数据长度]
        "len:2", // 数据长度需要与数据类型对应，如例子所示
        "seq:2",
        "mac:6",
        "data:this.len-6", // 可以用“this.fieldname”来引用任意public的属性或方法
        "check:1",
        "tail:1"
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
    
## 2.2. 框架自动生成解析类 <类名>PacketParser

    public class TargetObjectPacketParser {
        public static final TargetObject parse(byte[] src) {
            return parse(src, new TargetObject());
        }
    
        public static final TargetObject parse(byte[] bytes, TargetObject src) {
            ...
            return src;
        }
    
        public static final byte[] toBytes(TargetObject src) {
            ...
            return byteBuffer.array();
        }
    }

注：需Builder一次项目，PacketParser才会生成。
    
## 2.3. 使用方法

    String data = "AA11220008556677889911223344556677";
    byte[] bytes = hexToBytes(data);
    // bytes to object
    TargetObject targetObject = TargetObjectPacketParser.parse(bytes); // suffix with "PacketParser"
    //object to bytes
    byte[] toBytes = TargetObjectPacketParser.toBytes(targetObject);

**Gradle**

    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            ...
            classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8' // add this
            ...
        }
    }
    
    apply plugin: 'com.android.application'
    apply plugin: 'com.neenbedankt.android-apt'

    apt 'com.squareup:javapoet:1.8.0'
    apt 'com.google.auto:auto-common:0.6'
    apt 'com.google.auto.service:auto-service:1.0-rc2'
    apt 'com.legendmohe.maven:packetparser-compiler:x.y'
    compile 'com.legendmohe.maven:packetparser-annotation:x.y'

[packetparser-compiler ![Download](https://api.bintray.com/packages/legendmohe/maven/packetparser-compiler/images/download.svg) ](https://bintray.com/legendmohe/maven/packetparser-compiler/_latestVersion)  
[packetparser-annotation ![Download](https://api.bintray.com/packages/legendmohe/maven/packetparser-annotation/images/download.svg) ](https://bintray.com/legendmohe/maven/packetparser-annotation/_latestVersion)
