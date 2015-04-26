package com.hacktothefuture.hermes;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by ldaniels on 4/25/15.
 */
public class Wall {
    private LatLng m_latlng;
    private String m_id;

    public LatLng get_latlng() {
        return m_latlng;
    }

    public void set_latlng(LatLng m_latlng) {
        this.m_latlng = m_latlng;
    }

    public String get_id() {
        return m_id;
    }

    public void set_id(String m_id) {
        this.m_id = m_id;
    }
}
