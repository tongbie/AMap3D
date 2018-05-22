package com.example.amap3d.Gsons;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BieTong on 2018/3/15.
 */

public class BusDataGson {
    private String bus_lineName;
    private String bus_departureSite;
    private String bus_arriveSite;
    private String GPSDeviceIMEI;

    public String getBus_lineName() {
        if (bus_lineName == null) {
            bus_lineName = "";
        }
        return bus_lineName;
    }

    public String getBus_departureSite() {
        if (bus_departureSite == null) {
            bus_departureSite = "";
        }
        return bus_departureSite;
    }

    public String getBus_arriveSite() {
        if (bus_arriveSite == null) {
            bus_arriveSite = "";
        }
        return bus_arriveSite;
    }

    public String getGPSDeviceIMEI() {
        if (GPSDeviceIMEI == null) {
            GPSDeviceIMEI = "";
        }
        return GPSDeviceIMEI;
    }
}
