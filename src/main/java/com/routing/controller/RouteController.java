package com.routing.controller;

import com.routing.entity.OpenCageRequest;
import com.routing.entity.RouteResponse;
import com.routing.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RouteController {

    @Autowired
    private RouteService routeService;

    //you can extract locations from db or any sources
    // i am using postman to send list of address
    @PostMapping("/address-to-route")
    public ResponseEntity<RouteResponse> startAlgo(@RequestBody OpenCageRequest addresses) {
        RouteResponse response = routeService.startRoutingAlgorithm(addresses);
        return ResponseEntity.ok(response);
    }

}
