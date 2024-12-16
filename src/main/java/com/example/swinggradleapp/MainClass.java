package com.example.swinggradleapp;

import javax.swing.*;

public class MainClass {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame("Collaborative Whiteboard");
            frame.setVisible(true);
        });
    }
}
