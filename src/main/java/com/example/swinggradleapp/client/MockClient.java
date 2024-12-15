package com.example.swinggradleapp.client;

import com.example.swinggradleapp.MainFrame;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MockClient simulates server interactions for testing purposes.
 */
public class MockClient implements Client {
    private final MainFrame mainFrame;
    private final Gson gson = new Gson();
    private Timer mockTimer;
    private String boardId;

    /**
     * Constructs a MockClient with the specified MainFrame and boardId.
     *
     * @param mainFrame Reference to the MainFrame for UI updates.
     * @param boardId   The boardId to simulate.
     */
    public MockClient(MainFrame mainFrame, String boardId) {
        this.mainFrame = mainFrame;
        this.boardId = boardId;
    }

    @Override
    public boolean connect() {
        // Simulate a successful connection
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame,
                "Connected to Mock Server with boardId: " + boardId,
                "Mock Connection",
                JOptionPane.INFORMATION_MESSAGE));

        // Simulate server sending broadcast updates periodically
        startMockBroadcasts();

        return true;
    }

    /**
     * Starts a timer to simulate server broadcasting updates.
     */
    private void startMockBroadcasts() {
        mockTimer = new Timer();
        mockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Simulate a random update
                JsonObject updateMessage = new JsonObject();
                updateMessage.addProperty("type", "UPDATE");
                JsonArray pointsArray = new JsonArray();

                // Generate random points
                for (int i = 0; i < 5; i++) {
                    JsonObject point = new JsonObject();
                    int x = (int) (Math.random() * 800);
                    int y = (int) (Math.random() * 600);
                    int pen = Math.random() < 0.5 ? 0 : 1;
                    point.addProperty("x", x);
                    point.addProperty("y", y);
                    point.addProperty("pen", pen);
                    pointsArray.add(point);
                }

                updateMessage.add("points", pointsArray);

                // Send the update to the mainFrame
                mainFrame.applyPoints(parsePoints(updateMessage.getAsJsonArray("points")));
            }
        }, 5000, 5000); // Every 5 seconds
    }

    /**
     * Parses a JsonArray of points into a list of PointData.
     *
     * @param pointsArray The JsonArray containing points.
     * @return The list of PointData.
     */
    private List<MainFrame.PointData> parsePoints(JsonArray pointsArray) {
        List<MainFrame.PointData> points = new ArrayList<>();
        for (int i = 0; i < pointsArray.size(); i++) {
            JsonObject pointObj = pointsArray.get(i).getAsJsonObject();
            int x = pointObj.get("x").getAsInt();
            int y = pointObj.get("y").getAsInt();
            int pen = pointObj.get("pen").getAsInt();
            points.add(new MainFrame.PointData(x, y, pen));
        }
        return points;
    }

    @Override
    public void sendMessage(String message) {
        // Simulate handling incoming DRAW message from client
        JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
        String type = jsonMessage.get("type").getAsString();

        if (type.equals("DRAW")) {
            JsonArray pointsArray = jsonMessage.get("points").getAsJsonArray();
            List<MainFrame.PointData> points = new ArrayList<>();
            for (int i = 0; i < pointsArray.size(); i++) {
                JsonObject pointObj = pointsArray.get(i).getAsJsonObject();
                int x = pointObj.get("x").getAsInt();
                int y = pointObj.get("y").getAsInt();
                int pen = pointObj.get("pen").getAsInt();
                points.add(new MainFrame.PointData(x, y, pen));
            }

            // Apply the points to the local board
            mainFrame.applyPoints(points);

            // Optionally, simulate broadcasting to other clients (not implemented here)
        }
    }

    @Override
    public void close() {
        // Simulate closing the connection
        if (mockTimer != null) {
            mockTimer.cancel();
        }
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame,
                "Disconnected from Mock Server.",
                "Mock Disconnection",
                JOptionPane.INFORMATION_MESSAGE));
    }
}
