package com.example.swinggradleapp.client;

import com.example.swinggradleapp.MainFrame;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class RealClient implements Client {
    private WebSocketClient webSocketClient;
    private final MainFrame mainFrame;
    private final Gson gson = new Gson();

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

    @Override
    public void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        } else {
            System.err.println("WebSocket is not open. Cannot send message: " + message);
        }
    }

    @Override
    public void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    private void handleServerMessage(String message) {
        JsonObject jsonMessage = JsonParser.parseString(message).getAsJsonObject();
        String type = jsonMessage.get("type").getAsString();

        switch (type) {
            case "CONFIRM":
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
            case "UPDATE":
                JsonArray pointsArray = jsonMessage.get("points").getAsJsonArray();
                List<MainFrame.PointData> points = new ArrayList<>();
                for (int i = 0; i < pointsArray.size(); i++) {
                    JsonObject pointObj = pointsArray.get(i).getAsJsonObject();
                    int y = pointObj.get("x").getAsInt();
                    int x = pointObj.get("y").getAsInt();
                    int pen = pointObj.get("pen").getAsInt();
                    points.add(new MainFrame.PointData(x, y, pen));
                }
                mainFrame.applyPoints(points);
                break;

            case "ERROR":
                String errorMsg = jsonMessage.get("message").getAsString();
                JOptionPane.showMessageDialog(mainFrame,
                        "Server Error: " + errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                break;


            default:
                System.err.println("Unknown message type: " + type);
        }
    }


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
