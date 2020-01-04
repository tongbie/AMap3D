package com.example.amap3d.gsons;

/**
 * Created by BieTong on 2018/3/15.
 */

public class BusDataGson {
    private String bus_lineName;
    private String bus_departureSite;
    private String bus_arriveSite;
    private String GPSDeviceIMEI;

    private String containsString = "司机：";

    public String getBus_lineName() {
        if (bus_lineName == null) {
            bus_lineName = "";
        }
        return bus_lineName;
    }

    public String getBus_departureSite() {
        if (bus_departureSite == null) {
            bus_departureSite = "";
        } else {
            if (bus_departureSite.contains(containsString)) {
                int index = bus_departureSite.indexOf(containsString) + 3;
                if (bus_departureSite.length() > index) {
                    bus_departureSite = containsString + bus_departureSite.substring(index, index + 1) + "师傅";
                }
            }
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
