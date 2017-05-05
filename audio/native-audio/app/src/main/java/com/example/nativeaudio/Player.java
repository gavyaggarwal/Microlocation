package com.example.nativeaudio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTimestamp;
import android.media.AudioTrack;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

/**
 * Created by gavya on 5/2/2017.
 */
interface AudioSentCallback {
    void audioSent(long time);
}

public class Player extends Thread
{
    private boolean stopped = false;
    private AudioSentCallback callback;
    private boolean playTone = false;
    public long sendTime = 0;

    /**
     * Give the thread high priority so that it's not canceled unexpectedly, and start it
     */
    public Player(AudioSentCallback c)
    {
        callback = c;
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        start();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void run()
    {
        Log.i("AudioGavy", "Running Player Thread");

        AudioTrack player = null;
        final double CLIP_DURATION = 0.1;
        final int SAMPLE_RATE = 48000;
        final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
        final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
        final int MIN_BUFFER_LEN = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);
        final int NUM_SAMPLES = MIN_BUFFER_LEN / 2;
        final double VOLUME = 1;

        long NS_PER_SECOND = 1000000000;

        Log.d("AudioGavy", "Player Min Buffer Size: " + MIN_BUFFER_LEN + ", Sample Size: " + NUM_SAMPLES);

        long framesQueued = 0;
        double sample[] = new double[NUM_SAMPLES];
        double freqOfTone = 1000; // hz

        final byte soundClip[] = new byte[2 * NUM_SAMPLES];
        final byte blankClip[] = new byte[2 * NUM_SAMPLES];

        // fill out the array
        for (int i = 0; i < NUM_SAMPLES; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (SAMPLE_RATE / freqOfTone)) * VOLUME;
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            soundClip[idx++] = (byte) (val & 0x00ff);
            soundClip[idx++] = (byte) ((val & 0xff00) >>> 8);

        }

        /*
         * Initialize buffer to hold continuously recorded audio data, start recording, and start
         * playback.
         */
        try
        {
            player = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL, ENCODING, MIN_BUFFER_LEN, AudioTrack.MODE_STREAM);
            player.play();
            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            while(!stopped)
            {
                framesQueued += NUM_SAMPLES;
                if (playTone) {
                    player.write(soundClip, 0, soundClip.length);
                    playTone = false;
                    AudioTimestamp timestamp = new AudioTimestamp();
                    long time = System.nanoTime();
                    player.getTimestamp(timestamp);
                    long framesAfter = framesQueued - timestamp.framePosition;
                    long timeAfter = (framesAfter * NS_PER_SECOND) / SAMPLE_RATE;
                    //Log.d("AudioGavy", "Frames After: " + framesAfter + ", Time After: " + timeAfter);
                    callback.audioSent(time + timeAfter);
                } else {
                    player.write(blankClip, 0, blankClip.length);
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
            player.stop();
            player.release();
        }
    }

    public void playSound() {
        playTone = true;
    }

    /**
     * Called from outside of the thread in order to stop the recording/playback loop
     */
    private void close()
    {
        stopped = true;
    }

}