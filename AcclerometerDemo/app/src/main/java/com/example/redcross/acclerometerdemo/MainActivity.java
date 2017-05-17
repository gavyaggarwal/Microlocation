package com.example.redcross.acclerometerdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.LinkedList;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private int accelXTextViewId;
    private int accelYTextViewId;
    private int accelZTextViewId;

    private int velocityXTextViewId;
    private int velocityYTextViewId;
    private int velocityZTextViewId;

    private int cardinalDirectionTextViewId;

    private Sensor accSensor;
    private Sensor magSensor;
    private SensorManager sensorManger;

    private class TimedMeasure {
        public float[] values;
        public float timestamp;
    }

    private static final float nanoToSec = 1.0f / 1000000000.0f;
    private static final float MEASUREMENT_DELAY_SEC= 0.0f;

    private Queue<TimedMeasure> accMeasurements;
    private Queue<TimedMeasure> magMeasurements;
    private float[] gravity;
    private float velX;
    private float velY;
    private float velZ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManger = (SensorManager)getSystemService(SENSOR_SERVICE);
        accSensor = sensorManger.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManger.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManger.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManger.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);

        accelXTextViewId = R.id.accelXTextView;
        accelYTextViewId = R.id.accelYTextView;
        accelZTextViewId = R.id.accelZTextView;

        velocityXTextViewId = R.id.velocityXTextView;
        velocityYTextViewId = R.id.velocityYTextView;
        velocityZTextViewId = R.id.velocityZTextView;

        cardinalDirectionTextViewId = R.id.cardinalDirectionTextView;

        accMeasurements = new LinkedList<TimedMeasure>();
        magMeasurements = new LinkedList<TimedMeasure>();
        gravity = new float[3];
        velX = 0.0f;
        velY = 0.0f;
        velZ = 0.0f;
    }

    private void safeQueueAdd(Queue<TimedMeasure> q, float[] f, float timestamp) {
        assert(q.size() <= 2);
        if (q.size() == 2) {
            q.remove();
        }
        TimedMeasure tm = new TimedMeasure();
        tm.values = f;
        tm.timestamp = timestamp;

        if (q.isEmpty() || ( (tm.timestamp - q.peek().timestamp) * nanoToSec > MEASUREMENT_DELAY_SEC)) {
            q.add(tm);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            safeQueueAdd(accMeasurements, event.values, event.timestamp);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            safeQueueAdd(magMeasurements, event.values, event.timestamp);
        }
        if (accMeasurements.size() == 2 && magMeasurements.size() == 2) {
            // Calculate linear acceleration and velocity
            TimedMeasure accTM = accMeasurements.remove();
            float accelX = accTM.values[0];
            float accelY = accTM.values[1];
            float accelZ = accTM.values[2];

            // Isolate the force of gravity via low-pass filter.
            float alpha = 0.8f; // low-pass constant (may need to be change)
            gravity[0] = alpha * gravity[0] + (1 - alpha) * accelX;
            gravity[1] = alpha * gravity[1] + (1 - alpha) * accelY;
            gravity[2] = alpha * gravity[2] + (1 - alpha) * accelZ;

            float linAccelX = accelX - gravity[0];
            float linAccelY = accelY - gravity[1];
            float linAccelZ = accelZ - gravity[2];

            float dT = nanoToSec * (accTM.timestamp - accMeasurements.peek().timestamp);
            velX += linAccelX * dT;
            velY += linAccelY * dT;
            velZ += linAccelZ * dT;

            // Set TextViews
            ((TextView)findViewById(accelXTextViewId)).setText("linAccel X: " + linAccelX);
            ((TextView)findViewById(accelYTextViewId)).setText("linAccel Y: " + linAccelY);
            ((TextView)findViewById(accelZTextViewId)).setText("linAccel Z: " + linAccelZ);

            ((TextView)findViewById(velocityXTextViewId)).setText("vel X: " + velX);
            ((TextView)findViewById(velocityYTextViewId)).setText("vel Y: " + velY);
            ((TextView)findViewById(velocityZTextViewId)).setText("vel Z: " + velZ);


            // Calculate cardinal direction.
            float rotMatR[] = new float[9];
            float rotMatI[] = new float[9];

            boolean getRotIsASuccess = SensorManager.getRotationMatrix(rotMatR, rotMatI, accMeasurements.peek().values, magMeasurements.peek().values);

            if (getRotIsASuccess) {
                float orientation[] = new float[3];
                float remappedRotMat[] = new float[9];

                SensorManager.remapCoordinateSystem(rotMatR, SensorManager.AXIS_X, SensorManager.AXIS_Y, remappedRotMat);

                SensorManager.getOrientation(remappedRotMat, orientation);
                float orientationAzimuth = orientation[0];

                float degreeWRTNorth = (float)(Math.toDegrees(orientationAzimuth)+360) % 360;

                TextView cardinalDirectionTextView = (TextView) findViewById(cardinalDirectionTextViewId);

                CharSequence cardinalDirectionText = "" + degreeWRTNorth;
                if (degreeWRTNorth > 315 || degreeWRTNorth < 45) {
                    cardinalDirectionText = cardinalDirectionText + "\nNORTH";
                }
                else if (degreeWRTNorth > 45 && degreeWRTNorth < 135) {
                    cardinalDirectionText = cardinalDirectionText + "\nEAST";
                }
                else if (degreeWRTNorth > 135 && degreeWRTNorth < 225) {
                    cardinalDirectionText = cardinalDirectionText + "\nSOUTH";
                }
                else {
                    cardinalDirectionText = cardinalDirectionText + "\nWEST";
                }
                cardinalDirectionTextView.setText(cardinalDirectionText);
//                Log.d("OnSensorChanged", "getting here!");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor,  int accuracy) {
        // pass
    }
}
