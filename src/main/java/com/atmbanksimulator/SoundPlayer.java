package com.atmbanksimulator;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.awt.Toolkit;

/**
 * Generates simple built-in sound effects without needing external audio files.
 * All sound control lives here, not in the View.
 */
public final class SoundPlayer {

    private static final float SAMPLE_RATE = 44100f;
    private static volatile boolean muted = false;

    private SoundPlayer() {
    }

    public static boolean toggleMute() {
        muted = !muted;
        return muted;
    }

    public static boolean isMuted() {
        return muted;
    }

    public static void setMuted(boolean value) {
        muted = value;
    }

    public static void playButtonPress() {
        if (muted) return;
        playToneAsync(880, 45, 0.18);
    }

    public static void playSuccess() {
        if (muted) return;
        playSequenceAsync(
                new Tone(880, 70, 0.20),
                new Tone(1175, 90, 0.22)
        );
    }

    public static void playError() {
        if (muted) return;
        playSequenceAsync(
                new Tone(320, 120, 0.24),
                new Tone(240, 160, 0.24)
        );
    }

    private static void playToneAsync(int frequencyHz, int durationMs, double volume) {
        Thread soundThread = new Thread(() -> {
            try {
                playTone(frequencyHz, durationMs, volume);
            } catch (Exception e) {
                if (!muted) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private static void playSequenceAsync(Tone... tones) {
        Thread soundThread = new Thread(() -> {
            try {
                for (Tone tone : tones) {
                    if (muted) return;
                    playTone(tone.frequencyHz, tone.durationMs, tone.volume);
                    Thread.sleep(20);
                }
            } catch (Exception e) {
                if (!muted) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        soundThread.setDaemon(true);
        soundThread.start();
    }

    private static void playTone(int frequencyHz, int durationMs, double volume) throws Exception {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);

        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();

            int totalSamples = (int) ((durationMs / 1000.0) * SAMPLE_RATE);
            byte[] buffer = new byte[totalSamples * 2];

            for (int i = 0; i < totalSamples; i++) {
                double angle = 2.0 * Math.PI * i * frequencyHz / SAMPLE_RATE;
                short sample = (short) (Math.sin(angle) * Short.MAX_VALUE * volume);

                buffer[i * 2] = (byte) (sample & 0xff);
                buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }

            line.write(buffer, 0, buffer.length);
            line.drain();
        }
    }

    private static class Tone {
        private final int frequencyHz;
        private final int durationMs;
        private final double volume;

        private Tone(int frequencyHz, int durationMs, double volume) {
            this.frequencyHz = frequencyHz;
            this.durationMs = durationMs;
            this.volume = volume;
        }
    }
}
