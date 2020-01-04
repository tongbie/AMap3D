package com.example.amap3d.gsons;

import java.util.List;

public class TodayTimetableGson {

    private String id;
    private int from;
    private int to;
    private Object through;
    private int nextBus;
    private Object nextBusTime;
    private List<String> departure_time;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public Object getThrough() {
        return through;
    }

    public void setThrough(Object through) {
        this.through = through;
    }

    public int getNextBus() {
        return nextBus;
    }

    public void setNextBus(int nextBus) {
        this.nextBus = nextBus;
    }

    public Object getNextBusTime() {
        return nextBusTime;
    }

    public void setNextBusTime(Object nextBusTime) {
        this.nextBusTime = nextBusTime;
    }

    public List<String> getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(List<String> departure_time) {
        this.departure_time = departure_time;
    }
}
