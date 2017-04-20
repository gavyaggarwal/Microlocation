package com.example.redcross.app.demos;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.example.redcross.app.utils.ServerConnection;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by gavya on 4/14/2017.
 */

public class AccelerometerDemo implements SensorEventListener {
    public AccelerometerDemo(Context context) {
        Log.d("Accelerometer", "started");
        senSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

//        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senBarometer = senSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);


//        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
//        senSensorManager.registerListener(this, senGyroscope , SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senBarometer , SensorManager.SENSOR_DELAY_NORMAL);
    }
    public final double pressureToDistanceConstant = 8.945077;
    // The pressure decreases by 0.11179333 hPa for every meter increase in altitude
    private SensorManager senSensorManager;
//    private Sensor senAccelerometer;
//    private Sensor senGyroscope;
    private Sensor senBarometer;
    private boolean calibrating = true;
    private LinkedList<Float> pressures = new LinkedList<>();
    private float initalPressure = 0;


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
//        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            float x = event.values[0];
//            float y = event.values[1];
//            float z = event.values[2];
////            ServerConnection.instance.sendDebug("Accelerometer X", x);
////            ServerConnection.instance.sendDebug("Accelerometer Y", y);
////            ServerConnection.instance.sendDebug("Accelerometer Z", z);
//        }
//        if (mySensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            float x = event.values[0];
//            float y = event.values[1];
//            float z = event.values[2];
////            ServerConnection.instance.sendDebug("Gyroscope X", x);
////            ServerConnection.instance.sendDebug("Gyroscope Y", y);
////            ServerConnection.instance.sendDebug("Gyroscope Z", z);
//        }
        if (mySensor.getType() == Sensor.TYPE_PRESSURE) {
            float p = event.values[0];
            if (calibrating) {
                pressures.add(new Float(p));
                if (pressures.size() == 100) {
                    calibrating = false;
                    float averagePressure = 0;
                    ListIterator<Float> listIterator = pressures.listIterator();
                    while (listIterator.hasNext()) {
                        averagePressure += listIterator.next().floatValue();
                    }
                    averagePressure /= 100;
                    initalPressure = averagePressure;
                }
            } else {
                //ServerConnection.instance.sendDebug("Current Pressure (hPa)", p);
                pressures.remove();
                pressures.add(new Float(p));

                float averagePressure = 0;
                ListIterator<Float> listIterator = pressures.listIterator();
                while (listIterator.hasNext()) {
                    averagePressure += listIterator.next().floatValue();
                }
                averagePressure /= 100;

                double altitude = (initalPressure - averagePressure) * pressureToDistanceConstant;
                ServerConnection.instance.sendDebug("Height Change (m)", (float) altitude);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Accelerometer", "accuracy changed: " + Integer.toString(accuracy));
    }
}
