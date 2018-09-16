package com.example.amap3d.managers;

import android.content.SharedPreferences;
import android.support.annotation.Nullable;

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
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
//        try {
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
//            MyCookie[] myCookies = new MyCookie[cookieList.size()];
//            for (int i = 0; i < cookieList.size(); i++) {
//                Cookie cookie = cookieList.get(i);
//                MyCookie myCookie = new MyCookie();
//                myCookie.name = cookie.name();
//                myCookie.value = cookie.value();
//                myCookie.expiresAt = cookie.expiresAt();
//                myCookie.domain = cookie.domain().toCharArray();//有毒
//                myCookie.path = cookie.path();//有毒
//                myCookie.secure = cookie.secure();
//                myCookie.httpOnly = cookie.httpOnly();
//                myCookie.hostOnly = cookie.hostOnly();
//                myCookie.persistent = cookie.persistent();
//                myCookies[i] = myCookie;
//            }
//            objectOutputStream.writeObject(myCookies);
//            String objectString = byteArrayOutputStream.toString("ISO-8859-1");
//            objectOutputStream.flush();
//            objectOutputStream.close();
//            byteArrayOutputStream.close();
//            editor.putString(key, objectString);
//            editor.apply();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Utils.uiToast("序列化失败\n" + e.getMessage());
//        }
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
    public static List<Cookie> get(String key, @Nullable Object obj) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
//        String objectString = sharedPreferences.getString(key, null);
//        List<Cookie> cookieList = new ArrayList<>();
//        ObjectInputStream objectInputStream = null;
//        ByteArrayInputStream byteArrayInputStream = null;
//        MyCookie[] myCookies;
//        try {
//            assert objectString != null;
//            byteArrayInputStream = new ByteArrayInputStream(objectString.getBytes("ISO-8859-1"));
//            objectInputStream = new ObjectInputStream(byteArrayInputStream);
//            Object object = objectInputStream.readObject();
//            myCookies = (MyCookie[]) object;
//            for (MyCookie myCooky : myCookies) {
//                cookieList.add(myCooky.createCookie());
//            }
//            objectInputStream.close();
//            byteArrayInputStream.close();
//        } catch (Exception e) {
//            Utils.uiToast("反序列化失败" + e.getMessage());
//            e.printStackTrace();
//            return cookieList;
//        }
//        return cookieList;
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
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.putString(key, content);
        editor.apply();
    }

    public static String get(String key) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        return sharedPreferences.getString(key, null);
    }

    public static void delete(String key) {
        sharedPreferences = MainActivity.getActivity().getSharedPreferences(key, MODE_PRIVATE);
        editor = sharedPreferences.edit();
        editor.remove(key);
        editor.apply();
    }
}
