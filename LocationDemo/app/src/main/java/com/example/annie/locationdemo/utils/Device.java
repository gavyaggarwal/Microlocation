package com.example.annie.locationdemo.utils;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by Annie on 5/17/17.
 */

public class Device {
    public static Device instance = new Device();
    public String id;
    public String color;
    public Point location;
    public void setContext(Context context) {
        String hardwareID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        switch (hardwareID) {
            case "ccf6cfbb8ceac04d":
                id = "A";
                color = "#FF6384";
                location = new Point(-1, 0, 0);
                break;
            case "3203ace7c9325f23":
                id = "B";
                color = "#36A2EB";
                location = new Point(0, 0, -1);
                break;
            case "720d0aad5060d35f":
                id = "C";
                color = "#FFCE56";
                location = new Point(1, 0, 0);
                break;
            case "657a6df8ae381735":
                id = "D";
                color = "#4BC0C0";
                location = new Point(0, 0, 1);
                break;
            case "756736212b3e81e6":
                id = "E";
                color = "#9866FF";
                location = new Point(0, 0, 0);
                break;
            default:
                Log.d("Microlocation", "Encountered New Device with ID: " + hardwareID);
                id = hardwareID;
                color = "#666666";
                location = new Point();
                break;
        }
    }
}
