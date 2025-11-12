package com.routing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.routing.config.ApiKeys;
import com.routing.entity.OpenCageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class RouteService {

    @Autowired
    private ApiKeys apiKeys;
    private final RestTemplate restTemplate = new RestTemplate();

    private List<String> places = new ArrayList<>();
    double[][] distances;

    // starting the process
    public Map<String, Double> startRoutingAlgorithm(OpenCageRequest addresses){
        this.places = addresses.getAddresses();
        System.out.println(places);

        List<double[]> geoCodes = getLatLonList(addresses);
        distances = buildDistanceMatrix(geoCodes);

        List<Integer> routeIdx = nearestNeighbor();
        System.out.println("After applying nearest neighbour");
        printRoute(routeIdx);

        List<Integer> finalRouteIdx = twoOpt(routeIdx);
        System.out.println("After applying 2 opt");
        printRoute(finalRouteIdx);

        return generateFinalRouteWithDiatances(finalRouteIdx);

    }

    private Map<String, Double> generateFinalRouteWithDiatances(List<Integer> finalRouteIdx) {
        Map<String, Double> result = new HashMap<>();

        for (int i=0; i<places.size(); i++){
            if(i== places.size()-1){
                result.put(places.get(finalRouteIdx.get(i)), routeCost(finalRouteIdx));
                continue;
            }
            result.put(places.get(finalRouteIdx.get(i)), distances[finalRouteIdx.get(i)][finalRouteIdx.get(i+1)]);
        }

        return result;
    }


    // latitude and longitude for a multiple address using OpenCage
    private List<double[]> getLatLonList(OpenCageRequest addressObj) {
        List<double[]> coordinates = new ArrayList<>();
        try {
            List<String> addresses = addressObj.getAddresses();

            for (String addr : addresses) {
                String url = "https://api.opencagedata.com/geocode/v1/json?q="
                        + java.net.URLEncoder.encode(addr, "UTF-8")
                        + "&key=" + apiKeys.getApiKey();

                JsonNode response = restTemplate.getForObject(url, JsonNode.class);

                if (response != null && response.has("results") && !response.get("results").isEmpty()) {
                    JsonNode geometry = response.get("results").get(0).get("geometry");
                    double lat = geometry.get("lat").asDouble();
                    double lon = geometry.get("lng").asDouble();
                    coordinates.add(new double[]{lat, lon});
                } else {
                    // fallback if no result
                    coordinates.add(new double[]{0.0, 0.0});
                }

                // Optional: pause 1 second to respect free API rate limits
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.out.println("Error while fetching longitude and latitude!! "+ e.getMessage());
        }

        return coordinates;


    }

    // distance matrix using OSRM
    private double[][] buildDistanceMatrix(List<double[]> coordinates) {
        coordinates.removeIf(d-> d[0]==0.00 && d[1]==0.00);

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

            System.out.println(response);

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
            System.out.println("Error while fetching distance matrix!! "+e.getMessage());
        }

        return matrix;
    }



//    algorithm
    //applying nearest neighbour
    private List<Integer> nearestNeighbor() {
        int n = places.size();
        boolean[] visited = new boolean[n];
        List<Integer> route = new ArrayList<>();
        int current = 0; // from first
        route.add(current);
        visited[current] = true;

        for (int step = 1; step < n; step++) {
            double best = Double.MAX_VALUE;
            int next = -1;
            for (int j = 0; j < n; j++) {
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
    private List<Integer> twoOpt(List<Integer> routeIdx) {
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
    void printRoute(List<Integer> routeIdx) {
        for (int idx : routeIdx) System.out.print(places.get(idx) + " -> ");
        System.out.println("END");
        System.out.printf("Total distance: %.2f km%n%n", routeCost(routeIdx));
    }

//    finding total cost
    private double routeCost(List<Integer> route) {
        double cost = 0;
        for (int i = 0; i < route.size() - 1; i++)
            cost += distances[route.get(i)][route.get(i + 1)];
        return cost;
    }

}





