package com.musaarazzi.common.utils;

public class Chapter {
    private String title;
    private boolean selected;

    public Chapter(String title, boolean selected) {
        this.title = title;
        this.selected = selected;
    }

    public String getTitle() {
        return title;
    }

    public boolean getSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return "Chapter [title=" + this.title + ", selected=" + this.selected + "]";
    }
}