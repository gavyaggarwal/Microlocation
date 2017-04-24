package com.example.redcross.app.demos;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.util.Log;

import com.example.redcross.app.utils.BluetoothManager;
import com.example.redcross.app.utils.DeviceManager;
import com.example.redcross.app.utils.ServerConnection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by gavya on 4/14/2017.
 */

public class AccelerometerDemo implements SensorEventListener {
    public final double pressureToDistanceConstant = 8.945077;
    // The pressure decreases by 0.11179333 hPa for every meter increase in altitude
    private SensorManager senSensorManager;
//    private Sensor senAccelerometer;
//    private Sensor senGyroscope;
    private Sensor senBarometer;
    private boolean calibrating = true;
    private LinkedList<Float> pressures = new LinkedList<>();
    private float initalPressure = 0;
    private float avgPressure = 0;

    private Handler mHandler = new Handler();




    public AccelerometerDemo(Context context) {
        Log.d("Accelerometer", "started");
        senSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        senBarometer = senSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        senSensorManager.registerListener(this, senBarometer , SensorManager.SENSOR_DELAY_NORMAL);



        BluetoothManager.instance.start();

        mStatusChecker.run();
    }

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
                if (pressures.size() == 10) {
                    calibrating = false;
                    float averagePressure = 0;
                    ListIterator<Float> listIterator = pressures.listIterator();
                    while (listIterator.hasNext()) {
                        averagePressure += listIterator.next().floatValue();
                    }
                    averagePressure /= 10;
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
                averagePressure /= 10;

                BluetoothManager.instance.pressure = averagePressure;
                avgPressure = averagePressure;
                double altitude = (initalPressure - averagePressure) * pressureToDistanceConstant;
                ServerConnection.instance.sendDebug("Height Change (m)", (float) altitude);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("Accelerometer", "accuracy changed: " + Integer.toString(accuracy));
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                ArrayList<Map<BluetoothManager.DataType, Object>> devices = BluetoothManager.instance.getNearbyDevices();

                for (Map<BluetoothManager.DataType, Object> device: devices) {
                    Log.d("Accelerometer", device.get(BluetoothManager.DataType.DEVICE_NAME).toString());
                    Log.d("Accelerometer", device.get(BluetoothManager.DataType.AIR_PRESSURE).toString());
                    Log.d("Accelerometer", String.valueOf(avgPressure));

                    float remPressure = (float) device.get(BluetoothManager.DataType.AIR_PRESSURE);


                    double diff = (remPressure - avgPressure) * pressureToDistanceConstant;
                    ServerConnection.instance.sendDebug("Height Difference (m)", (float) diff);
                }

                BluetoothManager.instance.restart();

            } finally {
                mHandler.postDelayed(mStatusChecker, 100);
            }
        }
    };
}
