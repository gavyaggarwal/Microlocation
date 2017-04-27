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
    private long updateTime = 0;
    private final double HPA_PER_METER = 0.11179333;
    // The pressure decreases by 0.11179333 hPa for every meter increase in altitude
    public float referencePressure;
    public float referenceHeight;
    public float currentPressure;

    public void setContext(Context context) {
        Log.d("Sensors", "Turning on Sensors");
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorManager.registerListener(this, barometer , SensorManager.SENSOR_DELAY_NORMAL);

        Sensor rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public boolean getIsMoving() {
        return isCurrentlyMoving || new Date().getTime() - updateTime < 1000;
    }

    public double estimatedHeight() {
        //Server.instance.sendDebug("Estimated Height", (float) (-currentPressure / HPA_PER_METER));
        return (referencePressure - currentPressure) / HPA_PER_METER / 2 + referenceHeight;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (x > 0.01 || y > 0.01 || z > 0.01) {
                if (!isCurrentlyMoving) updateTime = new Date().getTime();
                isCurrentlyMoving = true;
            }
            else {
                if (isCurrentlyMoving) updateTime = new Date().getTime();
                isCurrentlyMoving = false;
            }

        }
        if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
            float p = event.values[0];
            // Let p = smooth average of values
            currentPressure = p;
            if (!getIsMoving()) {
                referencePressure = p;
                referenceHeight = Device.instance.y;
            }
        }
        if (mySensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float scalar = event.values[3];
            Log.d("Rotation", String.valueOf(x) + " " + String.valueOf(y) + " " + String.valueOf(z) + " " + String.valueOf(scalar));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Sensors", "Accuracy Changed: " + Integer.toString(accuracy) + " " + sensor);
    }
}
