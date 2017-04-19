package com.example.redcross.app;

import android.content.Context;
import android.provider.Settings.Secure;
import android.util.Log;

/**
 * Created by gavya on 4/14/2017.
 */

public class DeviceManager {
    public static DeviceManager instance = new DeviceManager();
    public String id;
    public String color;
    public int[] initialLocation;
    public void setContext(Context context) {
        String hardwareID = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
        switch (hardwareID) {
            case "ccf6cfbb8ceac04d":
                id = "A";
                color = "#FF6384";
                initialLocation = new int[]{-1, 0, 0};
                break;
            case "3203ace7c9325f23":
                id = "B";
                color = "#36A2EB";
                initialLocation = new int[]{0, 0, -1};
                break;
            case "720d0aad5060d35f":
                id = "C";
                color = "#FFCE56";
                initialLocation = new int[]{1, 0, 0};
                break;
            case "cb7458c492fc64a9":
                id = "D";
                color = "#4BC0C0";
                initialLocation = new int[]{0, 0, 1};
                break;
            case "<ADD ID HERE>":
                id = "E";
                color = "#9866FF";
                initialLocation = new int[]{0, 0, 0};
            default:
                Log.d("Microlocation", "Encountered New Device with ID: " + hardwareID);
                id = hardwareID;
                color = "#666666";
                initialLocation = new int[]{0, 0, 0};
                break;
        }
    }
}
