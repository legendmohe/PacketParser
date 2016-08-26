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
    TargetObject targetObject = TargetObjectPacketParser.parse(bytes); // suffix with "PacketParser"
    assertEquals((byte) 0xAA, targetObject.header);
    assertEquals(0x1122, targetObject.cmd);
    assertEquals(0x0008, targetObject.len);
    assertEquals(0x5566, targetObject.seq);
    assertArrayEquals(new byte[]{0x77, (byte) 0x88, (byte) 0x99, 0x11, 0x22, 0x33}, targetObject.mac);
    assertArrayEquals(new byte[]{0x44, 0x55}, targetObject.data);
    assertEquals(0x66, targetObject.check);
    assertEquals(0x77, targetObject.tail);

    byte[] toBytes = TargetObjectPacketParser.toBytes(targetObject);
    assertArrayEquals(bytes, toBytes);

# gradle

compile 'com.legendmohe.maven:packetparser-annonation:0.1'
apt 'com.legendmohe.maven:packetparser-complier:0.1'
