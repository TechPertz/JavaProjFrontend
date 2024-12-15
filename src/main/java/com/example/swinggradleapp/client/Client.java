package com.example.swinggradleapp.client;

/**
 * Client interface defines the methods for WebSocket communication.
 */
public interface Client {
    /**
     * Connects to the WebSocket server or performs necessary setup.
     *
     * @return true if connected successfully, false otherwise.
     */
    boolean connect();

    /**
     * Sends a message to the WebSocket server.
     *
     * @param message The message to send.
     */
    void sendMessage(String message);

    /**
     * Closes the WebSocket connection.
     */
    void close();
}
