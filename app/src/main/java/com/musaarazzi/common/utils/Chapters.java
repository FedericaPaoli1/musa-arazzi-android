package com.musaarazzi.common.utils;

public class Chapters {
    private int id;
    private String title;
    private String text;
    private String position;

    public Chapters(int id, String title, String text, String position) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.position = position;
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

    public String getText() {
        return text;
    }

    public String getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Chapters [id=" + this.id + ", title=" + this.title + ", text=" + this.text + ", position=" + this.position + "]";
    }
}
