package com.musaarazzi.common.utils;

public class Chapter {
    private final String title;
    private final String text;
    private final String position;
    private boolean selected;

    public Chapter(String title, String text, String position, boolean selected) {
        this.title = title;
        this.text = text;
        this.position = position;
        this.selected = selected;
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

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "Chapter [title=" + this.title + ", text=" + this.text + ", position=" + this.position + ", selected=" + this.selected + "]";
    }
}
