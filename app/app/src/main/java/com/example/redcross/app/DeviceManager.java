package com.example.redcross.app;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * Created by gavya on 4/14/2017.
 */

public final class DeviceManager {
    public static String getDeviceID(Context context) {
        String id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        switch (id) {
            case "ccf6cfbb8ceac04d":
                return "A";
            case "3203ace7c9325f23":
                return "B";
            case "720d0aad5060d35f":
                return "C";
            case "cb7458c492fc64a9":
                return "D";
            default:
                Log.d("Microlocation", "Encountered New Device with ID: " + id);
                return id;
        }
    }
    public static String getDeviceColor(Context context) {
        String id = getDeviceID(context);
        switch (id) {
            case "A":
                return "#FF6384";
            case "B":
                return "#36A2EB";
            case "C":
                return "#FFCE56";
            case "D":
                return "#4BC0C0";
            case "E":
                return "#9866FF";
            default:
                return "#666666";
        }
    }
}
