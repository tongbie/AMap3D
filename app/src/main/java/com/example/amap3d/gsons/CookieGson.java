package com.example.amap3d.gsons;

import com.example.amap3d.utils.Utils;

import okhttp3.Cookie;

public class CookieGson {
    public String name;
    public String value;
    public long expiresAt;
    public String domain;
    public String path;
    public boolean secure;
    public boolean httpOnly;
    public boolean hostOnly;
    public boolean persistent;

    public CookieGson(String name, String value, long expiresAt, String domain, String path, boolean secure, boolean httpOnly, boolean hostOnly, boolean persistent) {
        this.name = name;
        this.value = value;
        this.expiresAt = expiresAt;
        this.domain = domain;
        this.path = path;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.hostOnly = hostOnly;
        this.persistent = persistent;
    }

    public CookieGson(Cookie cookie) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    public boolean isHostOnly() {
        return hostOnly;
    }

    public void setHostOnly(boolean hostOnly) {
        this.hostOnly = hostOnly;
    }

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }
}
