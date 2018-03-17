package com.example.amap3d;

/**
 * Created by BieTong on 2018/3/15.
 */

public class BusPositionGson {
    private String GPSDeviceIMEI;
    private String lat;
    private String lng;

    public String getGPSDeviceIMEI() {
        return GPSDeviceIMEI;
    }

    public void setGPSDeviceIMEI(String GPSDeviceIMEI) {
        this.GPSDeviceIMEI = GPSDeviceIMEI;
    }

    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }
}
