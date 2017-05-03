package com.example.nativeaudio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;


/**
 * Created by gavya on 4/28/2017.
 */

interface LoudNoiseCallback {
    void loudNoise(int volume, long readTime); // would be in any signature
}


/*
 * Thread to manage live recording/playback of voice input from the device's microphone.
 */
public class Recorder extends Thread
{
    private boolean stopped = false;
    private LoudNoiseCallback callback;

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public Recorder(LoudNoiseCallback c)
    {
        callback = c;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        start();
    }

    @Override
    public void run()
    {
        Log.i("AudioGavy", "Running Recorder Thread");
        AudioRecord recorder = null;
        final int SAMPLE_RATE = 48000;
        final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
        final int MIN_BUFFER_LEN = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);
        final int NUM_SAMPLES = 960;

        long NS_PER_SECOND = 1000000000;
        long NS_PER_SAMPLE = NS_PER_SECOND / SAMPLE_RATE;

        short[] buffer = new short[NUM_SAMPLES];
        Log.d("AudioGavy", "Min Buffer Size: " + MIN_BUFFER_LEN + ", Sample Size: " + NUM_SAMPLES);

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, MIN_BUFFER_LEN);
            recorder.startRecording();
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(!stopped)
            {
                long readTime = System.nanoTime();
                int N = recorder.read(buffer, 0, buffer.length);
                assert N == NUM_SAMPLES;
                for (int i = 0; i < buffer.length; i++) {
                    if (buffer[i] > 500) {
                        // Throttle callback to 100 times per second
                        int samplesBefore = buffer.length - i;
                        long timeBefore = (samplesBefore * NS_PER_SECOND) / SAMPLE_RATE;
                        callback.loudNoise(buffer[i], readTime - timeBefore);
                        //Log.d("AudioGavy", "Time Before " + timeBefore);
                        break;
                    }
                }
            }
        }
        catch(Throwable x)
        {
            Log.w("AudioGavy", "Error reading voice audio", x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */
        finally
        {
            recorder.stop();
            recorder.release();
        }
    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    private void close()
    {
        stopped = true;
    }

}