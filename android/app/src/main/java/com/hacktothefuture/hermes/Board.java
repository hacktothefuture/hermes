package com.hacktothefuture.hermes;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ldaniels on 4/25/15.
 */
public class Board {
    private JsonLocation location;
    private List<String> messages;
    private String _id;

    public JsonLocation getLocation() {
        return location;
    }

    public void setLocation(JsonLocation location) {
        this.location = location;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public LatLng get_latlng() {
        List<Float> coords = location.getCoordinates();
        return new LatLng(coords.get(0), coords.get(1));
    }
    public void set_latlng(LatLng latlng) {
        location = new JsonLocation();
        List<Float> coords = new ArrayList<>();
        coords.add((float)latlng.latitude);
        coords.add((float)latlng.longitude);
        location.setCoordinates(coords);
    }
}
