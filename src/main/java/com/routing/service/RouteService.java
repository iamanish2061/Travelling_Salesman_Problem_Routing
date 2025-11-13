package com.routing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.routing.config.ApiKeys;
import com.routing.entity.OpenCageRequest;
import com.routing.entity.RouteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    @Autowired
    private ApiKeys apiKeys;
    @Autowired
    private RestTemplate restTemplate;

    // starting the process
    public RouteResponse startRoutingAlgorithm(OpenCageRequest request){
        List<String> places = new ArrayList<>(request.getAddresses());
        if(places.size()<2){
            throw new RuntimeException("At Least two addresses are required to find route!");
        }

        log.info("Fetching coordinates from OpenCage!");
        List<double[]> coordinates = getLatLonList(places);

        log.info("Fetching distance matrix from osrm!");
        double[][] distanceMatrix = buildDistanceMatrix(coordinates);

        log.info("Applying Nearest Neighbour!");
        List<Integer> routeIdx = nearestNeighbor(distanceMatrix, places.size());
        log.info("After nearest neighbour");
        printRoute(routeIdx, places, distanceMatrix);

        routeIdx = twoOpt(routeIdx, distanceMatrix);
        log.info("After nearest neighbour");
        printRoute(routeIdx, places, distanceMatrix);

        return generateRouteWithDistances(places, distanceMatrix, routeIdx);

    }

    private RouteResponse generateRouteWithDistances(List<String> places, double[][] distanceMatrix, List<Integer> routeIdx) {

        List<String> routePlaces = new ArrayList<>();
        List<Double> distanceResult = new ArrayList<>();

        for(int i=0; i<routeIdx.size()-1; i++){
            int from = routeIdx.get(i);
            int to = routeIdx.get(i + 1);
            routePlaces.add(places.get(from));
            distanceResult.add(distanceMatrix[from][to]);
        }

        routePlaces.add(places.get(routeIdx.get(routeIdx.size()-1)));

        double totalDistance = routeCost(routeIdx, distanceMatrix);

        return new RouteResponse(routePlaces, distanceResult, totalDistance);
    }

    // latitude and longitude for a multiple address using OpenCage
    private List<double[]> getLatLonList(List<String> addresses) {
        List<double[]> coordinates = new ArrayList<>();
        try {
            for (String addr : addresses) {
                String url = "https://api.opencagedata.com/geocode/v1/json?q="
                        + URLEncoder.encode(addr, StandardCharsets.UTF_8)
                        + "&key=" + apiKeys.getApiKey();

                JsonNode response = restTemplate.getForObject(url, JsonNode.class);

                if (response != null && response.has("results") && !response.get("results").isEmpty()) {
                    JsonNode geometry = response.get("results").get(0).get("geometry");
                    double lat = geometry.get("lat").asDouble();
                    double lon = geometry.get("lng").asDouble();
                    coordinates.add(new double[]{lat, lon});
                } else {
                    log.warn("No result for address: {}", addr);
                    coordinates.add(new double[]{0.0, 0.0});
                }

                Thread.sleep(1000);
            }

        } catch (Exception e) {
            log.error("Error fetching coordinates {}", e.getMessage());
            coordinates.add(new double[]{0.0, 0.0});
        }
        return coordinates;
    }

    // distance matrix using OSRM
    private double[][] buildDistanceMatrix(List<double[]> coordinates) {
        coordinates.removeIf(c->c[0]==0.00 && c[1]==0.00);

        int n = coordinates.size();
        double[][] matrix = new double[n][n];

        try {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                double lat = coordinates.get(i)[0];
                double lon = coordinates.get(i)[1];
                sb.append(lon).append(",").append(lat);
                if (i < n - 1) sb.append(";");
            }

            String url = "http://router.project-osrm.org/table/v1/driving/" + sb.toString() +"?annotations=distance";

            JsonNode response = restTemplate.getForObject(url, JsonNode.class);

            if (response != null && response.has("distances")) {
                JsonNode distances = response.get("distances");
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        matrix[i][j] = distances.get(i).get(j).asDouble() / 1000.0;
                    }
                }
            }else{
                System.out.println("Failed");
            }
        } catch (Exception e) {
            log.error("OSRM distance matrix request failed or returned empty response.");
        }
        return matrix;
    }


//    algorithm
    //applying nearest neighbour
    private List<Integer> nearestNeighbor(double[][] distances, int size) {
        boolean[] visited = new boolean[size];
        List<Integer> route = new ArrayList<>();
        int current = 0; // from first
        route.add(current);
        visited[current] = true;

        for (int step = 1; step < size; step++) {
            double best = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < size; j++) {
                if (!visited[j] && distances[current][j] < best) {
                    best = distances[current][j];
                    next = j;
                }
            }
            route.add(next);
            visited[next] = true;
            current = next;
        }
        route.add(0); // return to first
        return route;
    }


    // improving the result of nearest neighbour using 2-opt
    private List<Integer> twoOpt(List<Integer> routeIdx, double[][] distances) {
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int i = 1; i < routeIdx.size() - 2; i++) {
                for (int j = i + 1; j < routeIdx.size() - 1; j++) {
                    double before = distances[routeIdx.get(i - 1)][routeIdx.get(i)] +
                            distances[routeIdx.get(j)][routeIdx.get(j + 1)];
                    double after  = distances[routeIdx.get(i - 1)][routeIdx.get(j)] +
                            distances[routeIdx.get(i)][routeIdx.get(j + 1)];
                    if (after < before) {
                        Collections.reverse(routeIdx.subList(i, j + 1));
                        improved = true;
                    }
                }
            }
        }
        return routeIdx;
    }


    // Print route with names and total cost
    void printRoute(List<Integer> routeIdx, List<String> places, double[][] distances) {
        StringBuilder sb = new StringBuilder();
        for (int idx : routeIdx)
            sb.append(places.get(idx)).append(" -> ");
        sb.append("END");
        log.info("Route: {}", sb);
        log.info("Total distance: {} km", routeCost(routeIdx, distances));
    }


//    finding total cost
    private double routeCost(List<Integer> route, double[][] distances) {
        double cost = 0;
        for (int i = 0; i < route.size() - 1; i++)
            cost += distances[route.get(i)][route.get(i + 1)];
        return cost;
    }

}





