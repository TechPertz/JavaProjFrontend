package com.example.swinggradleapp;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        // Create and set up the window.
        MainFrame frame = new MainFrame("Collaborative Whiteboard - Login");
        frame.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        // Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null); // Center on screen
        frame.setVisible(true);
    }
}
