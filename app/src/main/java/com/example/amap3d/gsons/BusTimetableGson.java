package com.example.amap3d.gsons;

import java.util.List;

public class BusTimetableGson {

    private String id;
    private int from;
    private int to;
    private Object through;
    private Object start_date;
    private Object end_date;
    private List<String> weekend;
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

    public Object getStart_date() {
        return start_date == null ? "" : start_date;
    }

    public void setStart_date(Object start_date) {
        this.start_date = start_date;
    }

    public Object getEnd_date() {
        return end_date == null ? "" : end_date;
    }

    public void setEnd_date(Object end_date) {
        this.end_date = end_date;
    }

    public List<String> getWeekend() {
        return weekend;
    }

    public void setWeekend(List<String> weekend) {
        this.weekend = weekend;
    }

    public String getDeparture_time() {
        StringBuilder timeBuilder = new StringBuilder("");
        for (String time : departure_time) {
            timeBuilder.append(time + ", ");
        }
        String timeString;
        switch (timeBuilder.length()) {
            case 1:
                timeString = timeBuilder.toString();
                break;
            case 0:
                timeString = "";
                break;
            default:
                timeString = timeBuilder.substring(0, timeBuilder.length() - 2);
        }
        return timeString;
    }

    public void setDeparture_time(List<String> departure_time) {
        this.departure_time = departure_time;
    }
}
