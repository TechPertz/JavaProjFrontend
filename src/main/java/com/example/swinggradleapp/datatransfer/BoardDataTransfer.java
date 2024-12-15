package com.example.swinggradleapp.datatransfer;

import com.example.swinggradleapp.MainFrame;

public class BoardDataTransfer {

    /**
     * Handles incoming messages related to board data.
     * Expected message format: DRAW:x,y,colorHex
     *
     * @param message   The incoming message string.
     * @param mainFrame Reference to the main application frame for updating the UI.
     */
    public static void handleIncomingMessage(String message, MainFrame mainFrame) {
        if (message.startsWith("DRAW:")) {
            String[] parts = message.substring(5).split(",");
            if (parts.length == 3) {
                try {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    String colorHex = parts[2];
                    mainFrame.drawLine(x, y, colorHex);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid DRAW message format: " + message);
                }
            }
        } else if (message.startsWith("JOIN:")) {
            String username = message.substring(5);
            mainFrame.addUser(username);
        } else if (message.startsWith("LEAVE:")) {
            String username = message.substring(6);
            mainFrame.removeUser(username);
        }
        // Add more message handling as needed
    }
}
