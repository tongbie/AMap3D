package com.example.amap3d.gsons;

/**
 * Created by BieTong on 2018/3/15.
 */

public class BusPositionGson {
    private String GPSDeviceIMEI;
    private String lat;
    private String lng;

    public String getGPSDeviceIMEI() {
        if(GPSDeviceIMEI==null){
            GPSDeviceIMEI="";
        }
        return GPSDeviceIMEI;
    }

    public void setGPSDeviceIMEI(String GPSDeviceIMEI) {
        this.GPSDeviceIMEI = GPSDeviceIMEI;
    }

    public String getLat() {
        if(lat==null){
            lat="";
        }
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    public String getLng() {
        if(lng==null){
            return lng;
        }
        return lng;
    }

    public void setLng(String lng) {
        this.lng = lng;
    }
}
