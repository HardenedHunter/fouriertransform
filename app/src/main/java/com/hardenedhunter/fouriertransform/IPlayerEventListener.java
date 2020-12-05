package com.hardenedhunter.fouriertransform;

public interface IPlayerEventListener {
    void startPauseEvent();

    void songSelectedEvent(int song);

    void stopEvent();

    void sinEvent();

    void currentPlayingSelectedEvent();
}
