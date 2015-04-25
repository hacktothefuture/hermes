package com.hacktothefuture.hermes;

import java.util.List;

/**
 * Created by ldaniels on 4/25/15.
 */
public class Message {
    private Location location;
    private List<String> board;
    private String _id;

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public List<String> getBoard() {
        return board;
    }

    public void setBoard(List<String> board) {
        this.board = board;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }
}
