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

    public MockClient(MainFrame mainFrame, String boardId) {
        this.mainFrame = mainFrame;
        this.boardId = boardId;
    }

    @Override
    public boolean connect() {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame,
                "Connected to Mock Server with boardId: " + boardId,
                "Mock Connection",
                JOptionPane.INFORMATION_MESSAGE));

        startMockBroadcasts();

        return true;
    }

    private void startMockBroadcasts() {
        mockTimer = new Timer();
        mockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JsonObject updateMessage = new JsonObject();
                updateMessage.addProperty("type", "UPDATE");
                JsonArray pointsArray = new JsonArray();

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

                mainFrame.applyPoints(parsePoints(updateMessage.getAsJsonArray("points")));
            }
        }, 5000, 5000); // Every 5 seconds
    }

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

            mainFrame.applyPoints(points);

        }
    }

    @Override
    public void close() {
        if (mockTimer != null) {
            mockTimer.cancel();
        }
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(mainFrame,
                "Disconnected from Mock Server.",
                "Mock Disconnection",
                JOptionPane.INFORMATION_MESSAGE));
    }
}
