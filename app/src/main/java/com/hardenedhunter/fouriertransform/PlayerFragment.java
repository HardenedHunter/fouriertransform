package com.hardenedhunter.fouriertransform;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment {

    IPlayerEventListener playerEventListener;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        try {
            playerEventListener = (IPlayerEventListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onSomeEventListener");
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PlayerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PlayerFragment newInstance() {
        PlayerFragment fragment = new PlayerFragment();
        /*Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);*/
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }*/
    }

    // Обработчик кнопок для управления плееером (старт, пауза, стоп и т. п.)
    public void onAudioControlClick(View view) {
        switch (view.getId()) {
            case R.id.buttonPlayPause:
                playerEventListener.startPauseEvent();
                break;
            case R.id.buttonStop:
                playerEventListener.stopEvent();
                break;
        }
    }

    public void onWaveClick(View view) {
        playerEventListener.sinEvent();
    }

    // Обработчик кнопок для старта музыки, их надо убрать и сделать список музыки с девайса.
    // Все песни пока лежат в ./res/raw
    // Также есть одна идея с audioSessionId, свяжишь со мной как дойдёшь сюда.
    public void onSongClick(View view) {
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
        playerEventListener.songSelectedEvent(song);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_player, container, false);

        Button btnStart = v.findViewById(R.id.buttonPlayPause);
        Button btnStop = v.findViewById(R.id.buttonStop);
        Button btnSin = v.findViewById(R.id.buttonSin);
        Button btnDrake = v.findViewById(R.id.buttonDrake);
        Button btnFool = v.findViewById(R.id.buttonFool);
        Button btnSamurai = v.findViewById(R.id.buttonSamurai);
        Button btn5khz = v.findViewById(R.id.button5Khz);
        Button btn20khz = v.findViewById(R.id.button20Khz);

        btnStart.setOnClickListener(this::onAudioControlClick);
        btnStop.setOnClickListener(this::onAudioControlClick);
        btnSin.setOnClickListener(this::onWaveClick);
        btn5khz.setOnClickListener(this::onSongClick);
        btn20khz.setOnClickListener(this::onSongClick);
        btnDrake.setOnClickListener(this::onSongClick);
        btnFool.setOnClickListener(this::onSongClick);
        btnSamurai.setOnClickListener(this::onSongClick);

        return v;
    }
}