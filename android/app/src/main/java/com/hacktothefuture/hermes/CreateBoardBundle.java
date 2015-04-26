package com.hacktothefuture.hermes;

/**
 * Created by ldaniels on 4/25/15.
 */
public class CreateBoardBundle {
    private JsonLocation location;
    private String content;

    public JsonLocation getLocation() {
        return location;
    }

    public void setLocation(JsonLocation location) {
        this.location = location;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
