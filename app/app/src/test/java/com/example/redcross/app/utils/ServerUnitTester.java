package com.example.redcross.app.utils;

import org.junit.Test;

import static org.junit.Assert.*;

public class ServerUnitTester {
    @Test
    public void sendLocation() throws Exception {
        Server server = new Server();

        // Various tests for accuracy to see if sendLocation() fails
        server.sendLocation(0, 0, 0);
        server.sendLocation((float)0.0000, (float)0.0000, (float)0.0000);
        server.sendLocation((float)1.0001, (float)1.0001, (float)1.0001);
    }

    @Test
    public void sendDebug() throws Exception {
        Server server = new Server();

        // Various tests for accuracy to see if sendDebug() fails
        server.sendDebug("Estimated Height Change", (float)0.0000);
    }

}