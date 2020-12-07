package com.hardenedhunter.fouriertransform;

public class Song {

    private int id;
    private String title;
    private String artist;
    private String path;

    public Song(int id, String title, String artist, String path) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getPath() {
        return path;
    }
}
