package com.example.amap3d.managers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.amap3d.MainActivity;

import static android.content.Context.MODE_PRIVATE;

public class StorageManager {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    /*private static StorageManager storageManager;

    private StorageManager() {

    }

    public StorageManager getInstance() {
        if (storageManager == null) {
            storageManager = new StorageManager();
        }
        return storageManager;
    }*/

    public static void storage(String key, String content) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(key, content);
        editor.commit();
    }

    public static String get(String key) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        String content = sharedPreferences.getString(key, null);
        return content;
    }

    public static void delete(String key) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.remove(key);
    }
}
