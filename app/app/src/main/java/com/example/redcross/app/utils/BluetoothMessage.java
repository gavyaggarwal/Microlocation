package com.example.redcross.app.utils;

import java.nio.ByteBuffer;

/**
 * Created by gavya on 5/12/2017.
 */

public class BluetoothMessage {
    public static final int SIZE = 13;
    public final float x;
    public final float y;
    public final float z;
    public final char dev;
    public BluetoothMessage(float x_, float y_, float z_, char dev_) {
        x = x_;
        y = y_;
        z = z_;
        dev = dev_;
    }
    public BluetoothMessage(byte[] message) {
        dev = (char) message[0];
        x = ByteBuffer.wrap(message, 1, 4).getFloat();
        y = ByteBuffer.wrap(message, 5, 4).getFloat();
        z = ByteBuffer.wrap(message, 9, 4).getFloat();
    }
    public byte[] encode() {
        byte[] message = new byte[SIZE];
        byte[] xArr = ByteBuffer.allocate(4).putFloat(x).array();
        byte[] yArr = ByteBuffer.allocate(4).putFloat(y).array();
        byte[] zArr = ByteBuffer.allocate(4).putFloat(z).array();
        message[0] = (byte) dev;
        System.arraycopy(xArr, 0, message, 1, 4);
        System.arraycopy(yArr, 0, message, 5, 4);
        System.arraycopy(zArr, 0, message, 9, 4);
        return message;
    }
}
