package com.hardenedhunter.fouriertransform;

import java.util.List;

public interface ISongEventListener {

    void songSelectedEvent(int songPosition);

    void songsLoaded(List<Song> songs);

}
