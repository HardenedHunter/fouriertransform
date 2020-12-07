package com.hardenedhunter.fouriertransform;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PlayerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlayerFragment extends Fragment {

    IPlayerEventListener playerEventListener;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String SONG_ARTIST_PARAM = "artist";
    private static final String SONG_NAME_PARAM = "name";
    private static final String SONG_PLAYING_PARAM = "isPlaying";

    // TODO: Rename and change types of parameters
    private static String songArtist;
    private static String songName;
    private static boolean isPlaying;

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
        Bundle args = new Bundle();
        args.putString(SONG_ARTIST_PARAM, songArtist);
        args.putString(SONG_NAME_PARAM, songName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            songArtist = getArguments().getString(SONG_ARTIST_PARAM);
            songName = getArguments().getString(SONG_NAME_PARAM);
            isPlaying = getArguments().getBoolean(SONG_PLAYING_PARAM);
        }
    }

    // Обработчик кнопок для управления плееером (старт, пауза, стоп и т. п.)
    public void onAudioControlClick(View view) {
        switch (view.getId()) {
            case R.id.imageButtonPlayPause:
                playerEventListener.startPauseEvent();
                break;
            case R.id.imageButtonNext:
                playerEventListener.nextEvent();
                break;
            case R.id.imageButtonPrev:
                playerEventListener.prevEvent();
                break;
        }
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_player, container, false);

        ImageButton btnStart = v.findViewById(R.id.imageButtonPlayPause);
        ImageButton btnNext = v.findViewById(R.id.imageButtonNext);
        ImageButton btnPrev = v.findViewById(R.id.imageButtonPrev);

        TextView textViewName = v.findViewById(R.id.textViewSongName);
        TextView textViewArtist = v.findViewById(R.id.textViewSongAuthor);

        if (songName != null && songArtist != null){
            textViewName.setText(songName);
            textViewArtist.setText(songArtist);

            if (isPlaying)
                btnStart.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause));
            else
                btnStart.setImageDrawable(getResources().getDrawable(R.drawable.ic_play));
        }

        btnStart.setOnClickListener(this::onAudioControlClick);
        btnNext.setOnClickListener(this::onAudioControlClick);
        btnPrev.setOnClickListener(this::onAudioControlClick);

        return v;
    }
}