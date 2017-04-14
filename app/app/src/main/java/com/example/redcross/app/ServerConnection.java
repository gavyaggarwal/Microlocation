package com.example.redcross.app;

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
                Log.i("Websocket", "Opened");
                client.send("{ \"device\": \"A\", \"field\": \"Location\", \"x\": 0.52, \"y\": -0.213, \"z\": 1.30}");
            }

            @Override
            public void onMessage(String message) {
                Log.i("Websocket", message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i("Websocket", "Closed " + reason);
            }

            @Override
            public void onError(Exception ex) {
                Log.i("Websocket", "Error " + ex.getMessage());
            }
        };

        client.connect();
    }

}
