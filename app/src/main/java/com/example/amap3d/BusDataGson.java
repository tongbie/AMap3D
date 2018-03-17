package com.example.amap3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BieTong on 2018/3/15.
 */

public class BusDataGson {
    private String bus_lineName;
    private String bus_departureSite;
//    private String bus_arriveSite;
//    private int bus_status;
    private String GPSDeviceIMEI;
//    private int sort;

    public String getBus_lineName() {
        return bus_lineName;
    }

    public void setBus_lineName(String bus_lineName) {
        this.bus_lineName = bus_lineName;
    }

    public String getBus_departureSite() {
        return bus_departureSite;
    }

    public void setBus_departureSite(String bus_departureSite) {
        this.bus_departureSite = bus_departureSite;
    }

//    public String getBus_arriveSite() {
//        return bus_arriveSite;
//    }
//
//    public void setBus_arriveSite(String bus_arriveSite) {
//        this.bus_arriveSite = bus_arriveSite;
//    }
//
//    public int getBus_status() {
//        return bus_status;
//    }
//
//    public void setBus_status(int bus_status) {
//        this.bus_status = bus_status;
//    }

    public String getGPSDeviceIMEI() {
        return GPSDeviceIMEI;
    }

    public void setGPSDeviceIMEI(String GPSDeviceIMEI) {
        this.GPSDeviceIMEI = GPSDeviceIMEI;
    }

//    public int getSort() {
//        return sort;
//    }
//
//    public void setSort(int sort) {
//        this.sort = sort;
//    }
}
