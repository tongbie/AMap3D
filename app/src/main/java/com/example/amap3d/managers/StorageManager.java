package com.example.amap3d.managers;

import android.content.SharedPreferences;

import com.example.amap3d.MainActivity;
import com.example.amap3d.gsons.CookieGson;
import com.example.amap3d.utils.Utils;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;

import static android.content.Context.MODE_PRIVATE;

public class StorageManager {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private StorageManager() {

    }

    /*写Cookie*/
    public static void storage(String key, List<Cookie> cookieList) {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        List<CookieGson> cookieGsonList = new ArrayList<>();
        for (Cookie cookie : cookieList) {
            CookieGson cookieGson = new CookieGson(cookie);
            cookieGsonList.add(cookieGson);
        }
        String cookieString = Utils.gson.toJson(cookieGsonList);
        editor.putString(key, cookieString);
        editor.apply();
    }

    /*拿Cookie*/
    public static List<Cookie> getCookieList(String key) {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences(key, MODE_PRIVATE);
        String cookieString = sharedPreferences.getString(key, null);
        List<Cookie> cookieList = new ArrayList<>();
        if (cookieString != null) {
            List<CookieGson> cookieGsonList = Utils.gson.fromJson(cookieString, new TypeToken<List<CookieGson>>() {
            }.getType());
            for (CookieGson cookieGson : cookieGsonList) {
                cookieList.add(createCookie(cookieGson));
            }
        }
        return cookieList;
    }

    private static Cookie createCookie(CookieGson c) {
        Cookie cookie = null;
        Constructor constructor;
        try {
            constructor = Cookie.class
                    .getDeclaredConstructor(String.class, String.class, long.class, String.class, String.class,
                            boolean.class, boolean.class, boolean.class, boolean.class);
            constructor.setAccessible(true);
            cookie = (Cookie) constructor.newInstance(c.name, c.value, c.expiresAt, c.domain, c.path, c.secure, c.httpOnly, c.hostOnly, c.persistent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cookie;
    }

    public static void storage(String key, String content) {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(key, content);
        editor.commit();
    }

    public static String get(String key) {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences(key, MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    public static void delete(String key) {
        sharedPreferences = MainActivity.getInstance().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
