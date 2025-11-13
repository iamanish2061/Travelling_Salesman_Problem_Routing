package com.routing.entity;

import java.util.List;

public class RouteResponse {
    private List<String> places;
    private List<Double> distances;
    private double totalDistance;

    public RouteResponse() {
    }

    public RouteResponse(List<String> places, List<Double> distances, double totalDistance) {
        this.places = places;
        this.distances = distances;
        this.totalDistance = totalDistance;
    }

    public List<String> getPlaces() {
        return places;
    }

    public void setPlaces(List<String> places) {
        this.places = places;
    }

    public List<Double> getDistances() {
        return distances;
    }

    public void setDistances(List<Double> distances) {
        this.distances = distances;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(double totalDistance) {
        this.totalDistance = totalDistance;
    }
}
