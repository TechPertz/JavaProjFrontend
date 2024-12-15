package com.example.swinggradleapp.client;

import com.example.swinggradleapp.MainFrame;
import com.example.swinggradleapp.datatransfer.BoardDataTransfer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.net.URI;

/**
 * RealClient manages real-time communication with the backend server using WebSockets.
 */
public class RealClient implements Client {
    private WebSocketClient webSocketClient;
    private final MainFrame mainFrame;
    private final Gson gson = new Gson();

    /**
     * Constructs a RealClient with the specified server URI and MainFrame reference.
     *
     * @param serverUri The WebSocket server URI.
     * @param mainFrame Reference to the MainFrame for UI updates.
     */
    public RealClient(String serverUri, MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.webSocketClient = new WebSocketClient(URI.create(serverUri)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Connected to Server.",
                            "Connection Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            }

            @Override
            public void onMessage(String message) {
                SwingUtilities.invokeLater(() -> {
                    System.out.println("RealClient received message: " + message);
                    handleServerMessage(message);
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                            "Disconnected from Server.",
                            "Disconnected",
                            JOptionPane.WARNING_MESSAGE);
                });
            }

            @Override
            public void onError(Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(mainFrame,
                            "An error occurred: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        };
    }

    /**
     * Attempts to establish a blocking connection to the WebSocket server.
     *
     * @return true if connected successfully, false otherwise.
     */
    @Override
    public boolean connect() {
        try {
            webSocketClient.connectBlocking();
            return webSocketClient.isOpen();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a message to the WebSocket server.
     *
     * @param message The message to send.
     */
    @Override
    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        } else {
            System.err.println("WebSocket is not open. Cannot send message: " + message);
        }
    }

    /**
     * Closes the WebSocket connection.
     */
    @Override
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    /**
     * Handles incoming messages from the server.
     *
     * @param message The incoming message string.
     */
    private void handleServerMessage(String message) {
        JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
        String type = jsonMessage.get("type").getAsString();

        switch (type) {
            case "CONFIRM":
                // Server confirmation message with board data
                String username = jsonMessage.get("username").getAsString();
                JsonArray matrixArray = jsonMessage.get("matrix").getAsJsonArray();
                int[][] matrix = parseMatrix(matrixArray);
                JOptionPane.showMessageDialog(mainFrame,
                        "Welcome, " + username + "! Connected to the server.",
                        "Connection Confirmed",
                        JOptionPane.INFORMATION_MESSAGE);
                mainFrame.handleInitialBoard(matrix);
                break;

            case "DRAW":
                // Server broadcasts a draw action
                int x = jsonMessage.get("x").getAsInt();
                int y = jsonMessage.get("y").getAsInt();
                int colorInt = jsonMessage.get("color").getAsInt();
                String colorHex = colorInt == 1 ? "#000000" : "#FFFFFF"; // 1: black, 0: white
                mainFrame.drawLine(x, y, colorHex);
                break;

            case "UPDATE":
                // Server sends the entire board matrix
                JsonArray updatedMatrixArray = jsonMessage.get("matrix").getAsJsonArray();
                int[][] updatedMatrix = parseMatrix(updatedMatrixArray);
                mainFrame.updateBoard(updatedMatrix);
                break;

            // Handle more message types as needed

            default:
                System.err.println("Unknown message type: " + type);
        }
    }

    /**
     * Parses a JsonArray into a 2D int array representing the board matrix.
     *
     * @param matrixArray The JsonArray containing the board matrix.
     * @return The 2D int array.
     */
    private int[][] parseMatrix(JsonArray matrixArray) {
        int rows = matrixArray.size();
        int cols = matrixArray.get(0).getAsJsonArray().size();
        int[][] matrix = new int[rows][cols];

        for (int x = 0; x < rows; x++) {
            JsonArray row = matrixArray.get(x).getAsJsonArray();
            for (int y = 0; y < cols; y++) {
                matrix[x][y] = row.get(y).getAsInt();
            }
        }

        return matrix;
    }
}
