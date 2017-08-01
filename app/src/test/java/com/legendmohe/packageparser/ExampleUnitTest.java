package com.legendmohe.packageparser;

import org.junit.Test;

import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {

    @Test
    public void testEmptyObject() throws Exception {
        String data = "";
        byte[] bytes = hexToBytes(data);
        EmptyObject emptyObject = EmptyObjectPacketParser.parse(bytes);
        assert emptyObject != null;

        byte[] toBytes = EmptyObjectPacketParser.toBytes(emptyObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void testEmptyInheritance() throws Exception {
        String data = "aa00010002";
        byte[] bytes = hexToBytes(data);
        EmptyChildObject emptyObject = EmptyChildObjectPacketParser.parse(bytes);

        assertEquals(0xaa, emptyObject.header & 0xff);
        assertEquals(0x0001, emptyObject.cmd & 0xffff);
        assertEquals(0x0002, emptyObject.len & 0xffff);

        byte[] toBytes = EmptyChildObjectPacketParser.toBytes(emptyObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void testParseBufferUnderflow() throws Exception {
        String data = "aa0001";
        byte[] bytes = hexToBytes(data);
        ParentObject emptyObject = null;
        try {
            emptyObject = ParentObjectPacketParser.parse(bytes);
        } catch (Exception e) {
            assertEquals(BufferUnderflowException.class, e.getClass());
        }
    }

    @Test
    public void testToBytesWithEmptyAttribute() throws Exception {
        String data = "aa00010000";
        byte[] bytes = hexToBytes(data);
        ParentObject emptyObject = new ParentObject();
        emptyObject.header = (byte) 0xaa;
        emptyObject.cmd = 0x0001;

        byte[] toBytes = ParentObjectPacketParser.toBytes(emptyObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void baseTest1() throws Exception {
        String data = "AA11220008556677889911223344556677";
        byte[] bytes = hexToBytes(data);
        TargetObject targetObject = TargetObjectPacketParser.parse(bytes);
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
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    public void baseTest2() throws Exception {
        String data = "AA11220008556677889911223344556677";
        byte[] bytes = hexToBytes(data);
        ChildObject childObject = ChildObjectPacketParser.parse(bytes);
        assertEquals((byte) 0xAA, childObject.header);
        assertEquals(0x1122, childObject.cmd);
        assertEquals(0x0008, childObject.len);
        assertEquals(0x5566, childObject.seq);
        assertArrayEquals(new byte[]{0x77, (byte) 0x88, (byte) 0x99, 0x11, 0x22, 0x33}, childObject.mac);
        assertArrayEquals(new byte[]{0x44, 0x55}, childObject.data);
        assertEquals(0x66, childObject.check);
        assertEquals(0x77, childObject.tail);

        byte[] toBytes = ChildObjectPacketParser.toBytes(childObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void baseTest3() throws Exception {
        String data = "AA11220008556677889911223344556677";
        byte[] bytes = hexToBytes(data);
        ChildObject childObject = ChildObjectPacketParser.parse(bytes);
        assertEquals((byte) 0xAA, childObject.header);
        assertEquals(0x1122, childObject.cmd);
        assertEquals(0x0008, childObject.len);
        assertEquals(0x5566, childObject.seq);
        assertArrayEquals(new byte[]{0x77, (byte) 0x88, (byte) 0x99, 0x11, 0x22, 0x33}, childObject.mac);
        assertArrayEquals(new byte[]{0x44, 0x55}, childObject.data);
        assertEquals(0x66, childObject.check);
        assertEquals(0x77, childObject.tail);

        byte[] toBytes = ChildObjectPacketParser.toBytes(childObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void baseTest4() throws Exception {
        String data = "0a0007BB00030101010c";
        byte[] bytes = hexToBytes(data);
        TLVHolderHeaderObject tlvHolderObject = TLVHolderHeaderObjectPacketParser.parse(bytes);
        assertEquals((byte) 0x0A, tlvHolderObject.type);
        assertEquals(0x0007, tlvHolderObject.length);
        assertEquals((byte) 0xBB, tlvHolderObject.tlvObject.type);
        assertEquals(0x0003, tlvHolderObject.tlvObject.length);
        assertArrayEquals(new byte[]{0x01, 0x01, 0x01}, tlvHolderObject.tlvObject.value);
        assertEquals((byte) 0x0c, tlvHolderObject.a);

        byte[] toBytes = TLVHolderHeaderObjectPacketParser.toBytes(tlvHolderObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void testParseListFields() throws Exception {
        String data = "0a0010BB000301010100000003020000000100000002BB0003010100BB000101BB00020101";
        byte[] bytes = hexToBytes(data);

        // 使用生成的TLVHolderListObjectPacketParser解析字节数组
        TLVHolderListObject tlvHolderListObject = TLVHolderListObjectPacketParser.parse(bytes);

        // 检查TLV头部，即继承TLVHeaderObject的部分
        assertEquals((byte) 0x0A, tlvHolderListObject.type);
        assertEquals(0x0010, tlvHolderListObject.length);

        // 检查对象作为字段
        assertEquals((byte) 0xBB, tlvHolderListObject.tlvObject.type);
        assertEquals(0x0003, tlvHolderListObject.tlvObject.length);
        assertArrayEquals(new byte[]{0x01, 0x01, 0x01}, tlvHolderListObject.tlvObject.value);

        // 检查普通字段
        assertEquals(0x3, tlvHolderListObject.c);

        // 检查列表字段
        assertEquals(0x02, tlvHolderListObject.aLen);
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
    }

    @Test
    public void testParseListFields2() throws Exception {
        String data = "0a0013BB000301010100000001020000000100000002";
        byte[] bytes = hexToBytes(data);

        TLVHolderListObject tlvHolderListObject = TLVHolderListObjectPacketParser.parse(bytes);
        assertEquals((byte) 0x0A, tlvHolderListObject.type);
        assertEquals(0x0013, tlvHolderListObject.length);

        assertEquals((byte) 0xBB, tlvHolderListObject.tlvObject.type);
        assertEquals(0x0003, tlvHolderListObject.tlvObject.length);
        assertArrayEquals(new byte[]{0x01, 0x01, 0x01}, tlvHolderListObject.tlvObject.value);

        // don't parse b
        assertEquals(0x1, tlvHolderListObject.c);

        assertEquals(0x02, tlvHolderListObject.aLen);
        assertEquals(Integer.valueOf(0x1), tlvHolderListObject.a.get(0));
        assertEquals(Integer.valueOf(0x2), tlvHolderListObject.a.get(1));

        assertEquals(null, tlvHolderListObject.b);

        byte[] toBytes = TLVHolderListObjectPacketParser.toBytes(tlvHolderListObject);
        assertArrayEquals(bytes, toBytes);
    }

    @Test
    public void testEmptyListAttribute() throws Exception {
        String data = "0a0013BB000301010100000004020000000100000002";
        byte[] bytes = hexToBytes(data);

        TLVHolderListObject tlvHolderListObject = TLVHolderListObjectPacketParser.parse(bytes);
        assertEquals((byte) 0x0A, tlvHolderListObject.type);
        assertEquals(0x0013, tlvHolderListObject.length);

        assertEquals((byte) 0xBB, tlvHolderListObject.tlvObject.type);
        assertEquals(0x0003, tlvHolderListObject.tlvObject.length);
        assertArrayEquals(new byte[]{0x01, 0x01, 0x01}, tlvHolderListObject.tlvObject.value);

        assertEquals(0x4, tlvHolderListObject.c);

        assertEquals(0x02, tlvHolderListObject.aLen);
        assertEquals(Integer.valueOf(0x1), tlvHolderListObject.a.get(0));
        assertEquals(Integer.valueOf(0x2), tlvHolderListObject.a.get(1));

        // b is not null but an empty list
        assertEquals(0, tlvHolderListObject.b.size());

        byte[] toBytes = TLVHolderListObjectPacketParser.toBytes(tlvHolderListObject);
        assertArrayEquals(bytes, toBytes);
    }
}