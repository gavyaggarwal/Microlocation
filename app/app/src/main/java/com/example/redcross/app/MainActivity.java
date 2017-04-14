package com.example.redcross.app;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.loopj.android.http.PersistentCookieStore;

import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configure UI
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText("Device " + DeviceManager.getDeviceID(this));

        int backgroundColor = Color.parseColor(DeviceManager.getDeviceColor(this));
        getWindow().getDecorView().setBackgroundColor(backgroundColor);

        // Configure Server Connection
        ServerConnection connection = ServerConnection.instance;
        connection.deviceID = DeviceManager.getDeviceID(this);
        //connection.sendLocation(1, 1, 1);

        // Start Demo
        new AccelerometerDemo(this);

    }
}