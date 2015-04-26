package com.hacktothefuture.hermes;

import com.squareup.otto.Bus;

/**
 * Created by ldaniels on 4/25/15.
 */
public class LocationBus {
    private static final Bus m_instance = new Bus();

    public static Bus getInstance() {
        return m_instance;
    }
}
