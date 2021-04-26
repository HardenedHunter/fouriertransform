package com.hardenedhunter.fouriertransform;

import android.media.audiofx.Visualizer;
import android.util.Log;

import java.util.Arrays;

public class FrequencyReceiver {
    private Visualizer visualizer;
    private BluetoothTransmitter transmitter;

    private static final int RESPONSE_SIZE = 16;       // Размер выходного массива
    private static final int CAPTURE_SIZE = 256;       // Размер захватываемых данных
    private static final float SMOOTH = 0.3f;          // Плавность изменений
    private static final float LOW_PASS = 1;           // Погрешность сигнала

    public FrequencyReceiver(BluetoothTransmitter transmitter) {
        this.transmitter = transmitter;
    }

    // Номера полос, которые мы берём из захватываемых данных
    private static final byte[] posOffset = {2, 3, 4, 6, 8, 10, 12, 14, 16, 20, 25, 30, 35, 60, 80, 100, 120};

    // Сюда сохраняются значения с предыдущего захвата для плавности изменений
    private static final float[] magnitudeOld = new float[RESPONSE_SIZE];
    // Прошедшее с итерации время и нужное время на итерацию
    private static final byte[] time = new byte[]{0, 0};

    private static long timeSpent = 0;
    private static long currentTime = 0;

    public void release() {
        if (visualizer != null)
            visualizer.release();
    }

    public void startSession(int audioSessionId) {
        release();
        visualizer = new Visualizer(audioSessionId);
        visualizer.setCaptureSize(CAPTURE_SIZE);
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                currentTime = System.nanoTime();
                timeSpent = currentTime;

//                long startTime = System.nanoTime();
                // Преобразовали данные fft в амплитуды соотв. частот
                float[] magnitudes = fftToMagnitude(fft);

                float[] response = new float[RESPONSE_SIZE];
                byte[] processedResponse = new byte[RESPONSE_SIZE];

                float maxValue = 0;
                for (int i = 0; i < RESPONSE_SIZE; i++) {
                    // Берём текущую амплитуду из массива распределения
                    float magnitude = magnitudes[posOffset[i]];
                    // Чтобы не терять остальные значения, прибавляем их с некоторым коэффициентом
                    byte linesBetween;
                    if (i != 0) {
                        linesBetween = (byte) (posOffset[i] - posOffset[i - 1]);
                        for (byte j = 0; j < linesBetween; j++)
                            magnitude += ((float) j / linesBetween) * magnitudes[posOffset[i] - linesBetween + j];
                        linesBetween = (byte) (posOffset[i + 1] - posOffset[i]);
                        for (byte j = 0; j < linesBetween; j++)
                            magnitude += ((float) j / linesBetween) * magnitudes[posOffset[i] + linesBetween - j];
                    }

                    // Чтобы не было скачков, берём частями текущее значение и предыдущее
                    magnitude = magnitude * SMOOTH + magnitudeOld[i] * (1 - SMOOTH);
                    magnitudeOld[i] = magnitude;

                    // Заодно параллельно считаем максимум
                    if (magnitude > maxValue) maxValue = magnitude;
                    response[i] = magnitude;
                }

                if (maxValue > LOW_PASS) {
                    for (int pos = 0; pos < RESPONSE_SIZE; pos++) {
                        // Приводим результат к диапазону 1-16
                        processedResponse[pos] = (byte) (response[pos] * 16 / maxValue);
                    }
//                    time[0] = (byte) ((System.nanoTime() - startTime) / 1000000);
                    if (transmitter != null)
                        transmitter.doInBackground(processedResponse, time);
                }

//                Log.d(LOG_TAG, Arrays.toString(magnitudes));
            }
        }, Visualizer.getMaxCaptureRate(), false, true);
        visualizer.setEnabled(true);
    }

    private static float[] fftToMagnitude(byte[] fft) {
        float[] magnitudes = new float[CAPTURE_SIZE / 2];
        magnitudes[0] = (float) Math.abs(fft[0]);
        for (int k = 1; k < CAPTURE_SIZE / 2; k++) {
            int i = k * 2;
            magnitudes[k] = (float) Math.hypot(fft[i], fft[i + 1]);
        }
        return magnitudes;
    }
}

