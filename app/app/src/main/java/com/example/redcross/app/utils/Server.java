package com.example.redcross.app.utils;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by gavya on 4/14/2017.
 */

public class Server {
    public static Server instance = new Server();
    private static WebSocketClient client;
    private static boolean connectionOpen;
    public String deviceID;

    public Server() {
        client = makeWebsocket();
        if (client != null) {
            client.connect();
        } else {
            return;
        }

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

    public static class ConnectivityChangeReceiver
            extends BroadcastReceiver {

        boolean connected = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            debugIntent(intent, "networkchange");
        }

        private void debugIntent(Intent intent, String tag) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Log.d(tag, extras.get("networkInfo").toString());
                if (extras.get("noConnectivity") != null) {
                    connected = false;
                }
                if (extras.get("networkInfo") != null) {
                    if (extras.get("networkInfo").toString().contains("DISCON")) {
                        Log.d(tag, "wifi disconnected");
                        connected = false;
                    }
                    else {
                        if (connected == false) {
                            client = makeWebsocket();
                            client.connect();
                            Log.d(tag, "wifi connected, reconnect to server");
                            connected = true;
                        }
                    }
                }
            }
            else {
                Log.d(tag, "no extras");
            }
        }

    }

    private static WebSocketClient makeWebsocket() {
        URI uri;
        try {
            uri = new URI("ws://microlocation.herokuapp.com/messages");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }

        return new WebSocketClient(uri) {
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
    }
}
