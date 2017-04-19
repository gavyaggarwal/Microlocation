package com.example.redcross.app;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

/**
 * Created by Annie on 4/18/17.
 */

public class InfoManager {

    private DeviceManager devInfo = new DeviceManager();

    public String deviceLocation(Context c) {

        double lat = 0;
        double lon = 0;
        String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);

        c.getSystemService(Context.LOCATION_SERVICE);

        LocationManager lmanage = (LocationManager)c.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location myLocation = lmanage.getLastKnownLocation("");
            Log.d("LOC_DEB",""+myLocation.getLatitude());
            Log.d("LOC_DEB",""+myLocation.getLongitude());
            lat = myLocation.getLatitude();
            lon = myLocation.getLongitude();
        } catch (SecurityException e) {
            Log.d("UH_OH","SPAGHETTIO");
        }

        return "L:" + id + ", " + lat + ", " + lon;
    }

    public void encodingData(String data) {

    }

    public void getData() {

    }
}
