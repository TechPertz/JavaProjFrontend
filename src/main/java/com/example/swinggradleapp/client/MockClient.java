package com.example.swinggradleapp.client;

import com.example.swinggradleapp.MainFrame;
import com.example.swinggradleapp.utils.Config;
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
    private final String boardId;

    public MockClient(MainFrame mainFrame, String boardId) {
        this.mainFrame = mainFrame;
        this.boardId = boardId;
    }

    @Override
    public boolean connect() {
        SwingUtilities.invokeLater(() -> showInfoDialog("Connected to Mock Server with boardId: " + boardId, "Mock Connection"));
        startMockBroadcasts();
        return true;
    }

    private void startMockBroadcasts() {
        mockTimer = new Timer();
        mockTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JsonObject updateMessage = createMockUpdateMessage();
                List<MainFrame.PointData> points = parsePoints(updateMessage.getAsJsonArray("points"));
                mainFrame.applyPoints(points);
            }
        }, 5000, 5000);
    }

    private JsonObject createMockUpdateMessage() {
        JsonObject updateMessage = new JsonObject();
        updateMessage.addProperty("type", "UPDATE");
        JsonArray pointsArray = generateRandomPoints(50); // Increased points for better simulation
        updateMessage.add("points", pointsArray);
        return updateMessage;
    }

    private JsonArray generateRandomPoints(int count) {
        JsonArray pointsArray = new JsonArray();
        for (int i = 0; i < count; i++) {
            JsonObject point = new JsonObject();
            int x = (int) (Math.random() * Config.BOARD_WIDTH);  // x as horizontal (columns)
            int y = (int) (Math.random() * Config.BOARD_HEIGHT); // y as vertical (rows)
            int pen = Math.random() < 0.5 ? 0 : 1;
            point.addProperty("x", x);
            point.addProperty("y", y);
            point.addProperty("pen", pen);
            pointsArray.add(point);
        }
        return pointsArray;
    }

    private List<MainFrame.PointData> parsePoints(JsonArray pointsArray) {
        List<MainFrame.PointData> points = new ArrayList<>();
        for (int i = 0; i < pointsArray.size(); i++) {
            JsonObject pointObj = pointsArray.get(i).getAsJsonObject();
            int x = pointObj.get("x").getAsInt(); // horizontal
            int y = pointObj.get("y").getAsInt(); // vertical
            int pen = pointObj.get("pen").getAsInt();
            points.add(new MainFrame.PointData(x, y, pen));
        }
        return points;
    }

    @Override
    public void sendMessage(String message) {
        JsonObject jsonMessage = gson.fromJson(message, JsonObject.class);
        String type = jsonMessage.get("type").getAsString();

        if ("DRAW".equals(type)) {
            JsonArray pointsArray = jsonMessage.get("points").getAsJsonArray();
            List<MainFrame.PointData> points = parsePoints(pointsArray);
            mainFrame.applyPoints(points);
        }
    }

    @Override
    public void close() {
        if (mockTimer != null) {
            mockTimer.cancel();
        }
        SwingUtilities.invokeLater(() -> showInfoDialog("Disconnected from Mock Server.", "Mock Disconnection"));
    }

    private void showInfoDialog(String message, String title) {
        JOptionPane.showMessageDialog(mainFrame, message, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
