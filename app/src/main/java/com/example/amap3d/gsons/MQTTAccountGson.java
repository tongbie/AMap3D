package com.example.amap3d.gsons;

/**
 * Created by BieTong on 2018/3/17.
 */

public class MQTTAccountGson {
    private String username;
    private String password;

    public String getUsername() {
        if(username==null){
            return ""
;        }
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if(password==null){
            return "";
        }
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
