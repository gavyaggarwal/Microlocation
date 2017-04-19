package com.example.redcross.app;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static android.content.ContentValues.TAG;

@TargetApi(21)

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

        LocationManager lmanage = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location myLocation = lmanage.getLastKnownLocation("");
            Log.d("LOC_DEB", "" + myLocation.getLatitude());
            Log.d("LOC_DEB", "" + myLocation.getLongitude());
            lat = myLocation.getLatitude();
            lon = myLocation.getLongitude();
        } catch (SecurityException e) {
            Log.d("UH_OH", "SPAGHETTIO");
        }

        return "L:" + id + ", " + lat + ", " + lon;
    }

    public void encodingData(String data) {

    }

    public void getData() {

    }


    public void parseAdvertisementPacket(final byte[] scanRecord) {

        byte[] advertisedData = Arrays.copyOf(scanRecord, scanRecord.length);

        int offset = 0;
        int i = 0;
        ArrayList<UUID> uuids = new ArrayList<UUID>();
        int MfgData[] = new int[33];

        while (offset < (advertisedData.length - 2)) {
            int len = advertisedData[offset++];
            if (len == 0)
                break;

            int type = advertisedData[offset++];
            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (len > 1) {
                        int uuid16 = advertisedData[offset++] & 0xFF;
                        uuid16 |= (advertisedData[offset++] << 8);
                        len -= 2;
                        uuids.add(UUID.fromString(String.format(
                                "%08x-0000-1000-8000-00805f9b34fb", uuid16)));
                    }
                    break;
                case 0x06:// Partial list of 128-bit UUIDs
                case 0x07:// Complete list of 128-bit UUIDs
                    // Loop through the advertised 128-bit UUID's.
                    while (len >= 16) {
                        try {
                            // Wrap the advertised bits and order them.
                            ByteBuffer buffer = ByteBuffer.wrap(advertisedData,
                                    offset++, 16).order(ByteOrder.LITTLE_ENDIAN);
                            long mostSignificantBit = buffer.getLong();
                            long leastSignificantBit = buffer.getLong();
                            uuids.add(new UUID(leastSignificantBit,
                                    mostSignificantBit));
                        } catch (IndexOutOfBoundsException e) {
                            // Defensive programming.
                            Log.e("BTDevFilter.parseUUID", e.toString());
                            continue;
                        } finally {
                            // Move the offset to read the next uuid.
                            offset += 15;
                            len -= 16;
                        }
                    }
                    break;
                case 0xFF:  // Manufacturer Specific Data
                    Log.d(TAG, "Manufacturer Specific Data size:" + len + " bytes");
                    while (len > 1) {
                        if (i < 32) {
                            MfgData[i++] = advertisedData[offset++];
                        }
                        len -= 1;
                    }
                    Log.d(TAG, "Manufacturer Specific Data saved." + MfgData.toString());
                    break;
                default:
                    offset += (len - 1);
                    break;
            }
        }


    }
}
