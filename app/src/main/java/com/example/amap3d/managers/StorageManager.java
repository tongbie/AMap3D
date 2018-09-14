package com.example.amap3d.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Xml;

import com.example.amap3d.MainActivity;
import com.example.amap3d.Utils;

import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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

    /*写Cookie*/
    public static void storage(String key, List<Cookie> cookieList) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            MyCookie[] myCookies = new MyCookie[cookieList.size()];
            for (int i = 0; i < cookieList.size(); i++) {
                myCookies[i] = new MyCookie(cookieList.get(i));
            }
            objectOutputStream.writeObject(myCookies);
            String objectString = byteArrayOutputStream.toString("ISO-8859-1");
            objectOutputStream.flush();
            objectOutputStream.close();
            byteArrayOutputStream.close();
            editor.putString(key, objectString);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
            Utils.uiToast("序列化失败\n" + e.getMessage());
        }
    }

    static class MyCookie implements Serializable {
        public MyCookie(Cookie cookie) {
            this.name = cookie.name();
            this.value = cookie.value();
            this.expiresAt = cookie.expiresAt();
            this.domain = cookie.domain();
            this.path = cookie.path();
            this.secure = cookie.secure();
            this.httpOnly = cookie.httpOnly();
            this.hostOnly = cookie.hostOnly();
            this.persistent = cookie.persistent();
        }

        public Cookie getCookie() {
            Cookie cookie = null;
            Constructor constructor;
            try {
                constructor = Cookie.class
                        .getDeclaredConstructor(String.class, String.class, long.class, String.class, String.class,
                                boolean.class, boolean.class, boolean.class, boolean.class);
                constructor.setAccessible(true);
                cookie = (Cookie) constructor.newInstance(name, value, expiresAt, domain, path, secure, httpOnly, hostOnly, persistent);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return cookie;
        }

        public String name;
        public String value;
        public long expiresAt;
        public String domain;
        public String path;
        public boolean secure;
        public boolean httpOnly;
        public boolean hostOnly;
        public boolean persistent;
    }

    /*拿Cookie*/
    public static List<Cookie> get(String key, @Nullable Object obj) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        String content = sharedPreferences.getString(key, null);
        List<Cookie> cookieList = new ArrayList<>();
        ObjectInputStream objectInputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
        MyCookie[] myCookies = null;
        try {
            byteArrayInputStream = new ByteArrayInputStream(content.getBytes("ISO-8859-1"));
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            myCookies = (MyCookie[]) object;
            for (int i = 0; i < myCookies.length; i++) {
                cookieList.add(myCookies[i].getCookie());
            }
            objectInputStream.close();
            byteArrayInputStream.close();
        } catch (Exception e) {
            Utils.uiToast("反序列化失败" + e.getMessage());
            e.printStackTrace();
            return cookieList;
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
}
