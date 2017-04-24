package com.example.redcross.app.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.Date;

/**
 * Created by gavya on 4/24/2017.
 */

public class Sensors implements SensorEventListener {
    public static Sensors instance = new Sensors();
    private boolean isCurrentlyMoving = false;
    private Date updateDate = new Date();
    private final double pressureToDistanceConstant = 8.945077;
    // The pressure decreases by 0.11179333 hPa for every meter increase in altitude
    public float referencePressure;
    public float referenceHeight;
    public float currentPressure;

    //private Handler mHandler = new Handler();


    public void setContext(Context context) {
        Log.d("Sensors", "Turning on Sensors");
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, barometer , SensorManager.SENSOR_DELAY_NORMAL);

        //Bluetooth.instance.start();

        //mStatusChecker.run();
    }

    public boolean getIsMoving() {
        return isCurrentlyMoving || new Date().getTime() - updateDate.getTime() < 1000;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (x > 0.01 || y > 0.01 || z > 0.01) {
                if (!isCurrentlyMoving) updateDate = new Date();
                isCurrentlyMoving = true;
            }
            else {
                if (isCurrentlyMoving) updateDate = new Date();
                isCurrentlyMoving = false;
            }

        }
        if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
            float p = event.values[0];
            // Let p = smooth average of values
            currentPressure = p;
            if (!isCurrentlyMoving) {
                referencePressure = p;
                referenceHeight = Device.instance.y;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Sensors", "Accuracy Changed: " + Integer.toString(accuracy) + " " + sensor);
    }

//    Runnable mStatusChecker = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                ArrayList<Map<Bluetooth.DataType, Object>> devices = Bluetooth.instance.getNearbyDevices();
//
//                for (Map<Bluetooth.DataType, Object> device: devices) {
//                    //Log.d("Accelerometer", device.get(Bluetooth.DataType.DEVICE_NAME).toString());
//                    //Log.d("Accelerometer", device.get(Bluetooth.DataType.AIR_PRESSURE).toString());
//                    //Log.d("Accelerometer", String.valueOf(avgPressure));
//
//                    float remPressure = (float) device.get(Bluetooth.DataType.AIR_PRESSURE);
//
//
//                    double diff = (remPressure - avgPressure) * pressureToDistanceConstant;
//                    //Server.instance.sendDebug("Height Difference (m)", (float) diff);
//                    //Log.d("Accelerometer", "Height Difference " + String.valueOf(diff));
//                }
//
//                Bluetooth.instance.restart();
//
//            } finally {
//                mHandler.postDelayed(mStatusChecker, 1000);
//            }
//        }
//    };
}
