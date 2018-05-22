package com.example.amap3d.Gsons;

/**
 * Created by BieTong on 2018/3/18.
 */

public class BusMoveGson {

    private String GPSDeviceIMEI;
    private double lat = 0;
    private double lng = 0;

    public String getGPSDeviceIMEI() {
        if (GPSDeviceIMEI == null) {
            GPSDeviceIMEI = "";
        }
        return GPSDeviceIMEI;
    }

    public void setGPSDeviceIMEI(String GPSDeviceIMEI) {
        this.GPSDeviceIMEI = GPSDeviceIMEI;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
