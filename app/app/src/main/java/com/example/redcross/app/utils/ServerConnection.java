package com.example.redcross.app.utils;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by gavya on 4/14/2017.
 */

public class ServerConnection {
    public static ServerConnection instance = new ServerConnection();
    private WebSocketClient client;
    private boolean connectionOpen;
    public String deviceID;

    public ServerConnection() {
        URI uri;
        try {
            uri = new URI("ws://microlocation.herokuapp.com/messages");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        client = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                connectionOpen = true;
                Log.i("Microlocation", "Socket Open");
            }

            @Override
            public void onMessage(String message) {
                Log.i("Microlocation", "Received Message on Socket: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                connectionOpen = false;
                Log.i("Microlocation", "Socket Closed " + reason);
            }

            @Override
            public void onError(Exception ex) {
                connectionOpen = false;
                Log.i("Microlocation", "Socket Error " + ex.getMessage());
            }
        };

        client.connect();
    }

    public void sendLocation(float x, float y, float z) {
        if (connectionOpen && deviceID != null) {
            String xStr = Float.toString(x);
            String yStr = Float.toString(y);
            String zStr = Float.toString(z);
            client.send("{ \"device\": \"" + deviceID + "\", \"field\": \"Location\", \"x\": " +
                    xStr + ", \"y\": " + yStr + ", \"z\": " + zStr + "}");
        }
    }

    public void sendDebug(String name, float value) {
        if (connectionOpen && deviceID != null) {
            client.send("{ \"device\": \"" + deviceID + "\", \"field\": \"" + name + "\", " +
                    "\"value\": " + Float.toString(value) + "}");
        }
    }

}
