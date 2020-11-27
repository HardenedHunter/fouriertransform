package com.hardenedhunter.fouriertransform;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.OutputStream;

public class BluetoothTransmitter extends AsyncTask<byte[], Integer, Integer> {
    private final OutputStream connectedOutputStream;

    public BluetoothTransmitter(BluetoothSocket socket) {
        OutputStream out = null;
        try {
            out = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        connectedOutputStream = out;
    }

    /**
     * Основной метод, который ещё и выполняется в другом потоке.
     * Отправляет данные по Bluetooth.
     *
     * @param bytes Первый массив - то, что передаём. Второй состоит
     *              из двух элементов - сколько прошло времени при
     *              вычислениях (в мс), и сколько должно пройти всего.
     *              Оставшееся время просто спим, чтобы каждый пакет
     *              отправлялся с одинаковой задержкой.
     * @return Возвращает 0, это заглушка, нигде не используется.
     */
    @Override
    public Integer doInBackground(byte[]... bytes) {
        byte timeSpentAlready = bytes[1][0];
        byte targetDelay = bytes[1][1];
        try {
            if (timeSpentAlready < targetDelay)
                Thread.sleep(targetDelay - timeSpentAlready);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sendByBluetooth(bytes[0]);
        return 0;
    }

    private void sendByBluetooth(byte[] buffer) {
        try {
            connectedOutputStream.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
