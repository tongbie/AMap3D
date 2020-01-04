package com.example.amap3d.gsons;

public class AdressGson {

    private int id;
    private String name;
    private long lat_start;
    private long lat_end;
    private long lng_start;
    private long lng_end;
    private String radius;

    public boolean isInThisArea(long lat, long lng) {
        return (lat > lat_start && lat < lat_end) && (lng > lng_start && lng < lat_end);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLat_start() {
        return lat_start;
    }

    public void setLat_start(long lat_start) {
        this.lat_start = lat_start;
    }

    public long getLat_end() {
        return lat_end;
    }

    public void setLat_end(long lat_end) {
        this.lat_end = lat_end;
    }

    public long getLng_start() {
        return lng_start;
    }

    public void setLng_start(long lng_start) {
        this.lng_start = lng_start;
    }

    public long getLng_end() {
        return lng_end;
    }

    public void setLng_end(long lng_end) {
        this.lng_end = lng_end;
    }

    public String getRadius() {
        return radius;
    }

    public void setRadius(String radius) {
        this.radius = radius;
    }
}
