package com.hardenedhunter.fouriertransform;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, IPlayerEventListener, ISettingsEventListener, ISongEventListener {
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
    // Адаптер Bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private Bundle settings;
    private List<Song> songs;
    private int currentPlayingSongPosition = -1;

    private PlayerFragment playerFragment;
    private SettingsFragment settingsFragment;
    private SongFragment songFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        settings = new Bundle();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            settings = setup(bluetoothAdapter);
        }

        loadSettings(settings);
        loadPlayer();

        // Запрашиваем разрешение на всё
        showPermission(Manifest.permission.RECORD_AUDIO, REQUEST_PERMISSION_RECORD_AUDIO);
        showPermission(Manifest.permission.BLUETOOTH, REQUEST_PERMISSION_BLUETOOTH);
        showPermission(Manifest.permission.BLUETOOTH_ADMIN, REQUEST_PERMISSION_BLUETOOTH_ADMIN);
        showPermission(Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_READ_STORAGE);
        showPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_WRITE_STORAGE);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.bottomNavigationViewMain);
        navigation.setSelectedItemId(R.id.navigation_home);

        navigation.setOnNavigationItemSelectedListener(this::onNavigationItemSelectedListener);
    }

    private boolean onNavigationItemSelectedListener(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.navigation_list:
                loadList();
                break;
            case R.id.navigation_home:
                loadPlayer();
                break;
            case R.id.navigation_settings:
                loadSettings(settings);
                break;
            default:
                return false;
        }
        return true;
    }

    private void loadList() {
        songFragment = new SongFragment();
        loadFragment(songFragment);
    }

    private void loadSettings(Bundle settings) {
        settingsFragment = new SettingsFragment();
        settingsFragment.setArguments(settings);
        loadFragment(settingsFragment);
    }

    private void loadPlayer() {
        playerFragment = new PlayerFragment();
        if (currentPlayingSongPosition != -1) {
            Bundle settings = new Bundle();
            Song song = songs.get(currentPlayingSongPosition);
            settings.putString("artist", song.getArtist());
            settings.putString("name", song.getTitle());
            settings.putBoolean("isPlaying", mediaPlayer.isPlaying());
            playerFragment.setArguments(settings);
        }
        loadFragment(playerFragment);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.constraintLayoutContent, fragment)
                .commitNow();
    }

    private void playSong(Song song) {
        releaseMediaPlayer();
        if (receiver == null)
            receiver = new FrequencyReceiver(transmitter);
        if (song.getId() == 0)
            return;

        TextView songArtist = getTextView(R.id.textViewSongAuthor);
        TextView songName = getTextView(R.id.textViewSongName);
        if (songArtist != null && songName != null) {
            songArtist.setText(song.getArtist());
            songName.setText(song.getTitle());
        }

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            mediaPlayer.setDataSource(song.getPath());
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Toast.makeText(this, R.string.media_error, Toast.LENGTH_SHORT).show();
        }



        // Берёт сессию музыки у плеера и передает ресиверу,
        // который будет брать из неё аудиоволны
        int audioSessionId = mediaPlayer.getAudioSessionId();
        Log.println(Log.DEBUG, "Audio", "started session with audioSessionId=" + audioSessionId);
        if (audioSessionId != -1) {
            receiver.startSession(audioSessionId);
        }
        mediaPlayer.setOnCompletionListener(this);
    }

    // Создание списка сопряжённых Bluetooth-устройств
    private Bundle setup(BluetoothAdapter bluetoothAdapter) {
        Bundle bundleSettings = new Bundle();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // Если есть сопряжённые устройства
        if (pairedDevices.size() > 0) {
            ArrayList<String> pairedDeviceArrayList = new ArrayList<>();
            // Добавляем сопряжённые устройства в список на экране - Имя + MAC-адресс
            for (BluetoothDevice device : pairedDevices)
                pairedDeviceArrayList.add(device.getName() + "\n" + device.getAddress());
            bundleSettings.putStringArrayList("devices", pairedDeviceArrayList);
            bundleSettings.putString("name", bluetoothAdapter.getName());
        }
        return bundleSettings;
    }

    // Методы интерфейсов  MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener
    @Override
    public void onCompletion(MediaPlayer mp) {
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void songSelectedEvent(int songPosition) {
        currentPlayingSongPosition = songPosition;
        if (songs != null)
            playSong(songs.get(currentPlayingSongPosition));
    }

    @Override
    public void songsLoaded(List<Song> songs) {
        this.songs = songs;
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
    private static final int REQUEST_PERMISSION_READ_STORAGE = 4;
    private static final int REQUEST_PERMISSION_WRITE_STORAGE = 5;

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

    private View getPlayerView(){
        View view = null;
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.constraintLayoutContent);
        if (fragment != null)
        {
            view = fragment.getView();
        }
        return view;
    }

    private TextView getTextView(int resourceId) {
        View view = getPlayerView();
        TextView result = null;
        if (view != null) {
            result = findViewById(resourceId);
        }
        return result;
    }

    private ImageButton getButton(int resourceId) {
        View view = getPlayerView();
        ImageButton button = null;
        if (view != null) {
            button = findViewById(resourceId);
        }
        return button;
    }

    //region listeners
    @Override
    public void startPauseEvent() {
        if (mediaPlayer == null)
            return;

        ImageButton imageButton = getButton(R.id.imageButtonPlayPause);

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            if (imageButton != null) {
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
            }
        } else {
            mediaPlayer.start();
            if (imageButton != null) {
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            }
        }
    }

    @Override
    public void nextEvent() {
        if (songs != null && currentPlayingSongPosition < songs.size() - 1) {
            currentPlayingSongPosition++;
            playSong(songs.get(currentPlayingSongPosition));
            ImageButton imageButton = getButton(R.id.imageButtonPlayPause);
            if (imageButton != null) {
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            }
        }
    }

    @Override
    public void prevEvent() {
        if (songs != null && currentPlayingSongPosition > 0) {
            currentPlayingSongPosition--;
            playSong(songs.get(currentPlayingSongPosition));
            ImageButton imageButton = getButton(R.id.imageButtonPlayPause);
            if (imageButton != null) {
                imageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            }
        }
    }


    @Override
    public void macSelectedEvent(String mac) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(mac);
        bluetoothConnector = new ThreadBluetoothConnector(device);
        bluetoothConnector.start();  // Запускаем поток для подключения Bluetooth
    }
    // endregion
}

