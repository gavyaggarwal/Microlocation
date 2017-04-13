package com.example.redcross.app;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.loopj.android.http.PersistentCookieStore;

public class MainActivity extends AppCompatActivity {

    public static PersistentCookieStore myCookieStore;
    public static Context mainContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myCookieStore = new PersistentCookieStore(this);
        new SocketHandler("{ \"device\": \"A\", \"field\": \"Location\","
                + "\"x\": 0.52, \"y\": -0.213, \"z\": 1.30}", this);
    }
}