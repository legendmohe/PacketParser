这个工具通过自动生成解析类，实现了字节数组和对象之间的转换。

# 使用@ParsePacket注解标注实体类:

    @ParsePacket(
            "header:1|cmd:2|len:2|seq:2|mac:6|data:this.len-6|check:1|tail:1"
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
    
# 框架自动生成解析类 <类名>PacketParser

    public class TargetObjectPacketParser {
        public static final TargetObject parse(byte[] src) {
            return parse(src, new TargetObject());
        }
    
        public static final TargetObject parse(byte[] bytes, TargetObject src) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
            src.header = byteBuffer.get();
            src.cmd = byteBuffer.getShort();
            src.len = byteBuffer.getShort();
            src.seq = byteBuffer.getShort();
            src.mac = new byte[6];
            byteBuffer.get(src.mac);
            src.data = new byte[src.len-6];
            byteBuffer.get(src.data);
            src.check = byteBuffer.get();
            src.tail = byteBuffer.get();
            return src;
        }
    
        public static final byte[] toBytes(TargetObject src) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(1 + 2 + 2 + 2 + 6 + src.len-6 + 1 + 1);
            byteBuffer.put(src.header);
            byteBuffer.putShort(src.cmd);
            byteBuffer.putShort(src.len);
            byteBuffer.putShort(src.seq);
            byteBuffer.put(src.mac);
            byteBuffer.put(src.data);
            byteBuffer.put(src.check);
            byteBuffer.put(src.tail);
            return byteBuffer.array();
        }
    }
    
## 使用方法:

    String data = "AA11220008556677889911223344556677";
    byte[] bytes = hexToBytes(data);
    // bytes to object
    TargetObject targetObject = TargetObjectPacketParser.parse(bytes); // suffix with "PacketParser"
    //object to bytes
    byte[] toBytes = TargetObjectPacketParser.toBytes(targetObject);

# gradle

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
    apt 'com.legendmohe.maven:packetparser-complier:x.y'
    compile 'com.legendmohe.maven:packetparser-annotation:x.y'
