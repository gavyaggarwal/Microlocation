package com.example.redcross.app;

import android.Manifest;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.redcross.app.demos.TrilaterationDemo;
import com.example.redcross.app.utils.Device;
import com.example.redcross.app.utils.Server;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Disable Screen from Turning Off
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize Device Manager and Server Connection
        Device.instance.setContext(this);
        Server.instance.deviceID = Device.instance.id;

        // Turn on sensors
        Sensors.instance.setContext(this);

        // Initialize Broadcast Reciever to manage Wi-Fi connection
        registerReceiver(
                new Server.ConnectivityChangeReceiver(),
                new IntentFilter(
                        ConnectivityManager.CONNECTIVITY_ACTION));

        // Configure UI
        TextView textView = (TextView)findViewById(R.id.textView);
        textView.setText("Device " + Device.instance.id);

        int backgroundColor = Color.parseColor(Device.instance.color);
        getWindow().getDecorView().setBackgroundColor(backgroundColor);

        ToggleButton button = (ToggleButton)findViewById(R.id.toggleButton) ;
        button.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (isChecked) {
                    Sensors.instance.startCalibrating();
                } else {
                    Sensors.instance.stopCalibrating();
                }
            }
        }) ;

        // Request necessary permissions
        requestPermissions();

        // Start Demo
        new TrilaterationDemo(this);

        //Sensors.instance.setContext(this);
    }


    public void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permissions", "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }
}