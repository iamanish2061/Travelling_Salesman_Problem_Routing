package com.routing.controller;

import com.routing.entity.OpenCageRequest;
import com.routing.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RouteController {

    @Autowired
    private RouteService routeService;

    //you can extract locations from db or any sources
    // i am using postman to send list of address
    @GetMapping("/address-to-route")
    public Map<String, Double> startAlgo(@RequestBody OpenCageRequest addresses) {
        return routeService.startRoutingAlgorithm(addresses);
    }

}
