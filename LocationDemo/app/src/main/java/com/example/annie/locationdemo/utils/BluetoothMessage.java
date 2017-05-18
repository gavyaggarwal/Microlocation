package com.example.annie.locationdemo.utils;


import java.nio.ByteBuffer;

/**
 * Created by Annie on 5/16/17.
 */

public class BluetoothMessage {
    public static final int SIZE = 13;
    public final Point loc;
    public final char dev;
    public BluetoothMessage(Point loc_, char dev_) {
        loc = loc_;
        dev = dev_;
    }
    public BluetoothMessage(byte[] message) {
        dev = (char) message[0];
        float x = ByteBuffer.wrap(message, 1, 4).getFloat();
        float y = ByteBuffer.wrap(message, 5, 4).getFloat();
        float z = ByteBuffer.wrap(message, 9, 4).getFloat();
        loc = new Point(x, y, z);
    }
    public byte[] encode() {
        byte[] message = new byte[SIZE];
        byte[] xArr = ByteBuffer.allocate(4).putFloat((float) loc.x).array();
        byte[] yArr = ByteBuffer.allocate(4).putFloat((float) loc.y).array();
        byte[] zArr = ByteBuffer.allocate(4).putFloat((float) loc.z).array();
        message[0] = (byte) dev;
        System.arraycopy(xArr, 0, message, 1, 4);
        System.arraycopy(yArr, 0, message, 5, 4);
        System.arraycopy(zArr, 0, message, 9, 4);
        return message;
    }
}
