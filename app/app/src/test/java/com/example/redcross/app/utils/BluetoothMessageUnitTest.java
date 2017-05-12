package com.example.redcross.app.utils;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Created by Gavy Aggarwal on 5/12/17.
 */
public class BluetoothMessageUnitTest {
    @Test
    public void messageEncodingWorks() throws Exception {
        float x = 123.456f;
        float y = 234.567f;
        float z = 345.678f;
        char id = 'A';
        byte[] message = new BluetoothMessage(x, y, z, id).encode();

        assertTrue(message.length == BluetoothMessage.SIZE);

        BluetoothMessage m = new BluetoothMessage(message);

        assertEquals(x, m.x, 1e-7);
        assertEquals(y, m.y, 1e-7);
        assertEquals(z, m.z, 1e-7);
        assertEquals(id, m.dev);
    }
}