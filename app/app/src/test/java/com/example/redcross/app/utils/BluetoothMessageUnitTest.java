package com.example.redcross.app.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Gavy Aggarwal on 5/12/17.
 */
public class BluetoothMessageUnitTest {
    @Test
    public void messageEncodingWorks() throws Exception {
        float x = 123.456f;
        float y = 234.567f;
        float z = 345.678f;
        Point loc = new Point(x, y, z);
        char id = 'A';
        byte[] message = new BluetoothMessage(loc, id).encode();

        assertTrue(message.length == BluetoothMessage.SIZE);

        BluetoothMessage m = new BluetoothMessage(message);

        assertEquals(loc, m.loc);
        assertEquals(id, m.dev);
    }
}