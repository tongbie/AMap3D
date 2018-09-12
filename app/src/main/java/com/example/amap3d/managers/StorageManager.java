package com.example.amap3d.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.example.amap3d.MainActivity;
import com.example.amap3d.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static android.content.Context.MODE_PRIVATE;

public class StorageManager {
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    private StorageManager() {

    }

    public static void storage(String key, List<Cookie> cookieList) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        try {
            Cookie[] cookies = (Cookie[]) cookieList.toArray();
            String objectString = serialize(cookies);
            editor.putString(key, objectString);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("序列化失败\n" + e.getMessage());
        }
    }

    public static List<Cookie> get(String key, @Nullable Object obj) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        String content = sharedPreferences.getString(key, null);
        Cookie[] cookies = null;
        try {
            cookies = (Cookie[]) deserialize(content);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("反序列化失败\n" + e.getMessage());
        }
        List<Cookie> cookieList;
        if (cookies == null) {
            cookieList = new ArrayList<>();
        } else {
            cookieList = Arrays.asList(cookies);
        }
        return cookieList;
    }

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
        editor.commit();
    }

    public static String serialize(Object object) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(object);
        String str = byteArrayOutputStream.toString("ISO-8859-1");
        objectOutputStream.close();
        byteArrayOutputStream.close();
        return str;
    }

    public static Object deserialize(String string) throws Exception {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(string.getBytes("ISO-8859-1"));
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        byteArrayInputStream.close();
        objectInputStream.close();
        return object;
    }

    public static void storage(HttpUrl httpUrl, List<Cookie> cookieList) {
        PersistentCookieStore persistentCookieStore = new PersistentCookieStore(MainActivity.getActivity());
        for (Cookie cookie : cookieList) {
            persistentCookieStore.add(httpUrl, cookie);
        }
    }

    public static List<Cookie> get(HttpUrl httpUrl) {
        List<Cookie> cookieList = new PersistentCookieStore(MainActivity.getActivity()).get(httpUrl);
        return cookieList == null ? new ArrayList<Cookie>() : cookieList;
    }
}
