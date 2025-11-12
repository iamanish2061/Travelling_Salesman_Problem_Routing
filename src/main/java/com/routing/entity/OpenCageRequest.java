package com.routing.entity;

import java.util.List;

public class OpenCageRequest {

    private List<String> addresses;

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    public OpenCageRequest() {
    }
}
