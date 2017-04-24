package com.example.redcross.app.utils;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * Created by gavya on 4/14/2017.
 */

public class Device {
    public static Device instance = new Device();
    public String id;
    public String color;
    public float x;
    public float y;
    public float z;
    public void setContext(Context context) {
        String hardwareID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        switch (hardwareID) {
            case "ccf6cfbb8ceac04d":
                id = "A";
                color = "#FF6384";
                x = -1;
                y = 0;
                z = 0;
                break;
            case "3203ace7c9325f23":
                id = "B";
                color = "#36A2EB";
                x = 0;
                y = 0;
                z = -1;
                break;
            case "720d0aad5060d35f":
                id = "C";
                color = "#FFCE56";
                x = 1;
                y = 0;
                z = 0;
                break;
            case "cb7458c492fc64a9":
                id = "D";
                color = "#4BC0C0";
                x = 0;
                y = 0;
                z = 1;
                break;
            case "756736212b3e81e6":
                id = "E";
                color = "#9866FF";
                x = 0;
                y = 0;
                z = 0;
                break;
            default:
                Log.d("Microlocation", "Encountered New Device with ID: " + hardwareID);
                id = hardwareID;
                color = "#666666";
                x = 0;
                y = 0;
                z = 0;
                break;
        }
    }
}
