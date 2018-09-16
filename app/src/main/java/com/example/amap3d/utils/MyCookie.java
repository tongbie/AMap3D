package com.example.amap3d.utils;

import java.io.Serializable;
import java.lang.reflect.Constructor;

import okhttp3.Cookie;

public class MyCookie implements Serializable {

    public String name;
    public String value;
    public long expiresAt;
    public char[] domain;
    public String path;
    public boolean secure;
    public boolean httpOnly;
    public boolean hostOnly;
    public boolean persistent;

    private static final long serialVersionUID = 1L;

    public Cookie getCookie() {
        Cookie cookie = null;
        Constructor constructor;
        try {
            constructor = Cookie.class
                    .getDeclaredConstructor(String.class, String.class, long.class, String.class, String.class,
                            boolean.class, boolean.class, boolean.class, boolean.class);
            constructor.setAccessible(true);
            cookie = (Cookie) constructor.newInstance(name, value, expiresAt, String.valueOf(domain), path, secure, httpOnly, hostOnly, persistent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cookie;
    }
}
