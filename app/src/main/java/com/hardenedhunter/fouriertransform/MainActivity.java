package com.hardenedhunter.fouriertransform;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static android.R.layout.simple_list_item_1;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener {
    // UUID этого устройства для Bluetooth
    private static final UUID deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Поток, который будет коннектиться к устройству
    private ThreadBluetoothConnector bluetoothConnector;
    // Передатчик, который будет передавать данные
    private BluetoothTransmitter transmitter;
    // Приёмник, который берёт из аудио-потока значения (сразу как FFT)
    private FrequencyReceiver receiver;
    // Чисто для тестов, просто отправляет на матрицу значения
    private ThreadWave waveGenerator;
    // Плеер, который играет музыку
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        TextView viewDevice = findViewById(R.id.textViewDevice);
        viewDevice.setText(bluetoothAdapter.getName());

        // Запрашиваем разрешение на всё
        showPermission(Manifest.permission.RECORD_AUDIO, REQUEST_PERMISSION_RECORD_AUDIO);
        showPermission(Manifest.permission.BLUETOOTH, REQUEST_PERMISSION_BLUETOOTH);
        showPermission(Manifest.permission.BLUETOOTH_ADMIN, REQUEST_PERMISSION_BLUETOOTH_ADMIN);

        if (bluetoothAdapter.isEnabled()) {
            setup(bluetoothAdapter);
        }
    }

    // Создание списка сопряжённых Bluetooth-устройств
    private void setup(BluetoothAdapter bluetoothAdapter) {
        ListView devicesView = findViewById(R.id.listBluetooth);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // Если есть сопряжённые устройства
        if (pairedDevices.size() > 0) {
            ArrayList<String> pairedDeviceArrayList = new ArrayList<>();
            // Добавляем сопряжённые устройства в список на экране - Имя + MAC-адресс
            for (BluetoothDevice device : pairedDevices)
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            ArrayAdapter<String> pairedDeviceAdapter = new ArrayAdapter<>(this, simple_list_item_1, pairedDeviceArrayList);
            devicesView.setAdapter(pairedDeviceAdapter);
            // Клик по нужному устройству
            devicesView.setOnItemClickListener((parent, view, position, id) -> {
                String itemValue = (String) devicesView.getItemAtPosition(position);
                String MAC = itemValue.substring(itemValue.length() - 17); // Вычленяем MAC-адрес
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(MAC);
                bluetoothConnector = new ThreadBluetoothConnector(device);
                bluetoothConnector.start();  // Запускаем поток для подключения Bluetooth
            });
        }
    }

    // Методы интерфейсов  MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    // Обработчик кнопок для управления плееером (старт, пауза, стоп и т. п.)
    public void onAudioControlClick(View view) {
        if (mediaPlayer == null)
            return;
        switch (view.getId()) {
            case R.id.buttonPlayPause:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                } else {
                    mediaPlayer.start();
                }
                break;
            case R.id.buttonStop:
                mediaPlayer.stop();
                break;
        }
    }

    // Поток для коннекта с Bluetooth, надо бы его вынести и сделать с callback'ом,
    // который принимает сокет
    private class ThreadBluetoothConnector extends Thread {
        private BluetoothSocket bluetoothSocket = null;

        private ThreadBluetoothConnector(BluetoothDevice device) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException eConnect) {
                eConnect.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Не удалось подключиться, " +
                        "проверьте Bluetooth-устройство", Toast.LENGTH_LONG).show());
                try {
                    bluetoothSocket.close();
                } catch (IOException eClose) {
                    eClose.printStackTrace();
                }
            }

            if (success) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this,
                        "Подключились", Toast.LENGTH_LONG).show());
                transmitter = new BluetoothTransmitter(bluetoothSocket);
                waveGenerator = new ThreadWave(bluetoothSocket);
            }
        }

        public void cancel() {
            runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show());
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Обработчик кнопок для старта музыки, их надо убрать и сделать список музыки с девайса.
    // Все песни пока лежат в ./res/raw
    // Также есть одна идея с audioSessionId, свяжишь со мной как дойдёшь сюда.
    public void onSongClick(View view) {
        releaseMediaPlayer();
        if (receiver == null)
            receiver = new FrequencyReceiver(transmitter);
        int song = 0;
        switch (view.getId()) {
            case R.id.buttonDrake:
                song = R.raw.drake;
                break;
            case R.id.buttonSamurai:
                song = R.raw.ohlsson;
                break;
            case R.id.buttonFool:
                song = R.raw.fool;
                break;
            case R.id.button5Khz:
                song = R.raw.b5;
                break;
            case R.id.button20Khz:
                song = R.raw.b20;
                break;
        }

        if (song == 0)
            return;

        mediaPlayer = MediaPlayer.create(this, song);
        mediaPlayer.start();

        // Берёт сессию музыки у плеера и передает ресиверу,
        // который будет брать из неё аудиоволны
        int audioSessionId = mediaPlayer.getAudioSessionId();
        if (audioSessionId != -1) {
            receiver.startSession(audioSessionId);
        }
        mediaPlayer.setOnCompletionListener(this);
    }

    // Релиз захваченных ресурсов
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (receiver != null) {
            receiver.release();
            receiver = null;
        }
    }

    public void OnWaveClick(View view) {
        if (waveGenerator != null) {
            waveGenerator.start();
        }
    }

    // Поток для генерации волны, не связан с музыкой и просто отправляет
    // значения каждые сколько-то мс
    private class ThreadWave extends Thread {
        private final OutputStream connectedOutputStream;

        public ThreadWave(BluetoothSocket socket) {
            OutputStream out = null;
            try {
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            byte[] wave = new byte[16];
            byte counter = 0;
            byte k = 1;
            for (int i = 0; i < 3000; i++) {
                try {
                    counter += k;
                    if (counter == 16)
                        k = -1;
                    if (counter == 0)
                        k = 1;
                    connectedOutputStream.write(wave);
                    for (int j = 0; j < 16; j++) {
                        wave[j] = counter;
                    }
                    sleep(15);
                } catch (Exception ignored) {
                    Log.d("ERROR", "counter");
                }
            }
        }
    }

    @Override
    protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        releaseMediaPlayer();
        if (bluetoothConnector != null) bluetoothConnector.cancel();
    }


    // Разрешение на всякие вещи
    private static final int REQUEST_PERMISSION_BLUETOOTH = 1;
    private static final int REQUEST_PERMISSION_BLUETOOTH_ADMIN = 2;
    private static final int REQUEST_PERMISSION_RECORD_AUDIO = 3;

    private void showPermission(String permission, int requestCode) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
            requestPermission(permission, requestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(MainActivity.this, "Permission denied.",
                    Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }
}
