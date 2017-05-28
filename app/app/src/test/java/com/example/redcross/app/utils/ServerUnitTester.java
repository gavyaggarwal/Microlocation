package com.example.redcross.app.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServerUnitTester {
    @Test
    public void sendLocation() throws Exception {
        Server server = new Server();

        Device.instance.location = new Point(0, 1, 2);

        // Various tests for accuracy to see if sendLocation() fails
        server.sendLocation();
    }

    @Test
    public void sendDebug() throws Exception {
        Server server = new Server();

        // Various tests for accuracy to see if sendDebug() fails
        server.sendDebug("Estimated Height Change", (float)0.0000);
    }

}