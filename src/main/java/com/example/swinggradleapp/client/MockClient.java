package com.example.swinggradleapp.client;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class MockClient implements Client {

    @Override
    public boolean connect() {
        // Simulate a successful connection
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Connected to Mock Server.",
                "Mock Connection",
                JOptionPane.INFORMATION_MESSAGE));
        return true;
    }

    @Override
    public void sendMessage(String message) {
        // Simulate receiving the same message
        SwingUtilities.invokeLater(() -> {
            // For simplicity, we'll just print the message to the console
            System.out.println("MockClient received message: " + message);
        });
    }

    @Override
    public void close() {
        // Simulate closing the connection
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                "Disconnected from Mock Server.",
                "Mock Disconnection",
                JOptionPane.INFORMATION_MESSAGE));
    }
}
