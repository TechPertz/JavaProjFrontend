package com.example.swinggradleapp.utils;

/**
 * Config class manages application configuration settings.
 */
public class Config {
    public static final boolean USE_REAL_CLIENT = true;

    public static final String WEBSOCKET_URL = "ws://localhost:8080/ws/draw"; // Update when server is live

    public static final String LOGIN_URL = "http://localhost:8080/login"; // Update when server is live

    // Centralizing board dimensions
    public static final int BOARD_WIDTH = 800;  // Width in pixels (columns)
    public static final int BOARD_HEIGHT = 600; // Height in pixels (rows)
}
