package com.example.redcross.app;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.loopj.android.http.PersistentCookieStore;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {

    public static PersistentCookieStore myCookieStore;
    public static Context mainContext;
    public WebSocketClient mWebSocketClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        URI uri;
        try {
            uri = new URI("ws://microlocation.herokuapp.com/messages");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                mWebSocketClient.send("{ \"device\": \"A\", \"field\": \"Location\", \"x\": 0.52, \"y\": -0.213, \"z\": 1.30}");
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("Websocket", "Message " + "{ \"device\": \"A\", \"field\": \"Location\","
                        + "\"x\": 0.52, \"y\": -0.213, \"z\": 1.30}");
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };

        mWebSocketClient.connect();
//        new SocketHandler("{ \"device\": \"A\", \"field\": \"Location\","
//                + "\"x\": 0.52, \"y\": -0.213, \"z\": 1.30}", this);
        Log.d("debugMessage", "we got to this point");
    }
}