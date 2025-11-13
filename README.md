Hello!
This is Optimized Route Finder with Spring Boot!

I have built a Traveling Salesman Problem (TSP) solution as a Spring Boot REST API!

First of all create the account in OpenCage for api key
https://opencagedata.com/users/sign_in#geocoding
you will get 2500 request per day

you can go through the documentation of
OpenCage: https://opencagedata.com/api
and
OSRM: https://project-osrm.org/docs/v5.5.1/api/#general-options

Guidance of my code
1. pom.xml file
   this includes all required dependencies like Web, Jackson ...

2.resources/application.properties
the api key of ApiKeys class is initialized here

3.classes
ApiKeys.java - contains apiKey field only used to sent request to OpenCage
GlobalExceptionHandler - for handling exception that occurs throughout the program

RoutingApplication - main class with bean to initialize RestTemplate
OpenCageRequest.java - addresses that needs to be sent to OpenCage api for finding coordinates
RouteResponse.java - contains fields needed to send response to the initial request

RouteController.java - end point to handle the request
RouteService.java - class that contains methods like:
->getting latitude and longitude from OpenCage api
-> getting distance matrix from OSRM api

and algorithm like:
-> Nearest Neighbour
-> 2-opt

other helper methods like: printing route, finding total cost, generating response



