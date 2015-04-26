package com.hacktothefuture.hermes;

import java.util.List;

/**
 * Created by ldaniels on 4/25/15.
 */
public class JsonLocation {

    private String type;
    private List<Float> coordinates;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Float> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<Float> coordinates) {
        this.coordinates = coordinates;
    }
}
