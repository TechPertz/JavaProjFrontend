package com.example.swinggradleapp.utils;

/**
 * Config class manages application configuration settings.
 */
public class Config {
    // Toggle between mock and real client
    public static final boolean USE_REAL_CLIENT = true;

    // Backend server WebSocket URL (used when USE_REAL_CLIENT is true)
    public static final String WEBSOCKET_URL = "ws://localhost:8080/ws/draw"; // Update when server is live

    // Backend server login endpoint
    public static final String LOGIN_URL = "http://localhost:8080/login"; // Update when server is live
}
