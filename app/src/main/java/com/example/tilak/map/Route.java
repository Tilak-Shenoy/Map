package com.example.tilak.map;

import com.google.android.gms.maps.model.LatLng;

public class Route {
    private int distance;
    private String turns;
    private int duration;
    private double startLat;
    private double startLong;
    private double endLat;
    private double endLong;
    private LatLng start,end;

    public Route(int distance,String turns, int duration, double startLat, double startLong, double endLat, double endLong){
        this.distance=distance;
        this.turns=turns;
        this.duration=duration;
        this.startLat=startLat;
        this.startLong=startLong;
        this.endLat=endLat;
        this.endLong=endLong;
        start=new LatLng(startLat,startLong);
        end=new LatLng(endLat,endLong);
    }

    public double getEndLat() {
        return endLat;
    }

    public double getEndLong() {
        return endLong;
    }

    public double getStartLat() {
        return startLat;
    }

    public double getStartLong() {
        return startLong;
    }

    public int getDistance() {
        return distance;
    }

    public int getDuration() {
        return duration;
    }

    public String getTurns() {
        return turns;
    }

    public LatLng getEnd() {
        return end;
    }

    public LatLng getStart() {
        return start;
    }
}
