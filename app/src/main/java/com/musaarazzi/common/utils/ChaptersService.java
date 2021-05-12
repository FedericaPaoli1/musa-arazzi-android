package com.musaarazzi.common.utils;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChaptersService {
    private static String getJsonFromAssets(Context context) {
        String jsonString;
        try {
            InputStream is = context.getAssets().open("chapters.json");

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            jsonString = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonString;
    }

    public static List<Chapter> readJson(String TAG, Context context) {
        String jsonFileString = getJsonFromAssets(context);
        Log.d(TAG, "Reading json");

        Gson gson = new Gson();
        Type listChaptersType = new TypeToken<List<Chapters>>() {
        }.getType();

        List<Chapters> chapters = gson.fromJson(jsonFileString, listChaptersType);
        List<Chapter> chapterList = new ArrayList<>();
        if (chapters != null) {
            for (int i = 0; i < chapters.size(); i++) {
                chapterList.add(new Chapter(chapters.get(i).getTitle(), chapters.get(i).getText(), chapters.get(i).getPosition(), false));
            }
        }
        return chapterList;
    }
}
