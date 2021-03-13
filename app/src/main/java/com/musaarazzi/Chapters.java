package com.musaarazzi;

public class Chapters {
    private int id;
    private String title;
    private String text;
    private int audio;

    public Chapters(int id, String title, String text, int audio) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.audio = audio;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getAudio() {
        return audio;
    }

    public void setAudio(int audio) {
        this.audio = audio;
    }

    @Override
    public String toString() {
        return "Chapters [id=" + this.id + ", title=" + this.title + ", text=" + this.text + ", audio=" + this.audio + "]";
    }
}
