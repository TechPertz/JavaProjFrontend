package com.example.swinggradleapp;

import javax.swing.*;

/**
 * Main class to launch the Collaborative Whiteboard Application.
 */
public class Main {
    public static void main(String[] args) {
        // Ensure the application runs on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame("Collaborative Whiteboard");
            frame.setVisible(true);
        });
    }
}
