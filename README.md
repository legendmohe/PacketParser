# PacketParser

convert bytes to object

example:

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
    
convert bytes to Object or Object to bytes:

    String data = "AA11220008556677889911223344556677";
    byte[] bytes = hexToBytes(data);
    // bytes to object
    TargetObject targetObject = TargetObjectPacketParser.parse(bytes); // suffix with "PacketParser"
    //object to bytes
    byte[] toBytes = TargetObjectPacketParser.toBytes(targetObject);

# gradle

compile 'com.legendmohe.maven:packetparser-annonation:0.1'
apt 'com.legendmohe.maven:packetparser-complier:0.1'
