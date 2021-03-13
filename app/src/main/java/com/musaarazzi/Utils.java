package com.musaarazzi;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    private static String getJsonFromAssets(Context context, String fileName) {
        String jsonString;
        try {
            InputStream is = context.getAssets().open(fileName);

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace(); // TODO insert message for exception
            return null;
        }

        return jsonString;
    }

    protected static List<Chapter> readJson(String TAG, Context context) {
        String jsonFileString = getJsonFromAssets(context, "chapters.json");
        Log.d(TAG, "Reading json...");

        Gson gson = new Gson();
        Type listChaptersType = new TypeToken<List<Chapters>>() {
        }.getType();

        List<Chapters> chapters = gson.fromJson(jsonFileString, listChaptersType);
        List<Chapter> chapterNames = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            chapterNames.add(new Chapter(chapters.get(i).getTitle(), false));
        }
        Log.d(TAG, "Chapter names obtained");
        return chapterNames;
    }

    public static class Chapter {
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
}

