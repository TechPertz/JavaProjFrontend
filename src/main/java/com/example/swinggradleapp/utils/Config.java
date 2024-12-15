package com.example.swinggradleapp.utils;

public class Config {
    // Toggle between mock and real client
    public static final boolean USE_REAL_CLIENT = false;

    // Backend server WebSocket URL (used when USE_REAL_CLIENT is true)
    public static final String SERVER_URL = "ws://your-live-backend-url:port";
}
