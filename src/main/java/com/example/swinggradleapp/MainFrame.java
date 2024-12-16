package com.example.swinggradleapp;

import com.example.swinggradleapp.client.Client;
import com.example.swinggradleapp.client.MockClient;
import com.example.swinggradleapp.client.RealClient;
import com.example.swinggradleapp.utils.Config;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * MainFrame represents the primary window of the Collaborative Whiteboard Application.
 */
public class MainFrame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private JPanel loginPanel;
    private JTextField nameField;
    private JButton enterButton;

    private JPanel whiteboardPanel;
    private JButton penButton;
    private JButton eraserButton;
    private Canvas whiteboardCanvas;

    private Client client;

    private final Gson gson = new Gson();

    private int penRadius = 10; // Default radius, can be modified

    private List<PointData> currentPoints = new ArrayList<>();

    private boolean isDrawing = false;

    private String boardId;

    private String username;

    public MainFrame(String title) {
        super(title);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        initLoginPanel();
        initWhiteboardPanel();

        mainPanel.add(loginPanel, "Login");
        mainPanel.add(whiteboardPanel, "Whiteboard");

        this.getContentPane().add(mainPanel);

        cardLayout.show(mainPanel, "Login");

        this.setSize(800, 600); // Adjusted initial size for better visibility
        this.setMinimumSize(new Dimension(800, 600)); // Set minimum size
        this.setLocationRelativeTo(null); // Center on screen
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sendLeaveMessage();
                if (client != null) {
                    client.close();
                }
            }
        });
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new GridBagLayout());
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // Padding

        JLabel promptLabel = new JLabel("Enter Your Name:");
        promptLabel.setFont(new Font("Arial", Font.PLAIN, 16));

        nameField = new JTextField(15); // Smaller width
        nameField.setFont(new Font("Arial", Font.PLAIN, 16));

        enterButton = new JButton("Enter");
        enterButton.setFont(new Font("Arial", Font.BOLD, 16));

        enterButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Name cannot be empty.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            enterButton.setEnabled(false);
            nameField.setEnabled(false);

            if (Config.USE_REAL_CLIENT) {
                new Thread(() -> performLogin(name)).start();
            } else {
                simulateLogin(name);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        loginPanel.add(promptLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        loginPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        loginPanel.add(enterButton, gbc);
    }


    private void simulateLogin(String name) {
        this.username = name;
        String mockUserId = "mockUser123";
        this.boardId = "mockBoard456";
        String confirmationMsg = "Welcome, " + name + "! (Mock Connection)";

        int[][] matrix = new int[800][600]; // Adjusted size to match whiteboardCanvas

        SwingUtilities.invokeLater(() -> {
            initializeWebSocket(boardId, matrix);

            JOptionPane.showMessageDialog(MainFrame.this,
                    confirmationMsg,
                    "Login Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void performLogin(String name) {
        try {
            URL url = new URL(Config.LOGIN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            JsonObject loginPayload = new JsonObject();
            loginPayload.addProperty("username", name);

            OutputStream os = conn.getOutputStream();
            byte[] input = gson.toJson(loginPayload).getBytes("utf-8");
            os.write(input, 0, input.length);
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) { // HTTP OK
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                StringBuilder responseBuilder = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseBuilder.append(responseLine.trim());
                }
                br.close();

                String response = responseBuilder.toString();
                JsonObject responseJson = JsonParser.parseString(response).getAsJsonObject();

                String userId = responseJson.get("user_id").getAsString();
                boardId = responseJson.get("board_id").getAsString();
                String confirmationMsg = responseJson.get("message").getAsString();
                JsonArray matrixArray = responseJson.get("board_matrix_data").getAsJsonArray();

                int[][] matrix = parseMatrix(matrixArray);



                SwingUtilities.invokeLater(() -> {
                    initializeWebSocket(boardId, matrix);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            confirmationMsg,
                            "Login Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Login failed with response code: " + responseCode,
                            "Login Error",
                            JOptionPane.ERROR_MESSAGE);
                    enterButton.setEnabled(true);
                    nameField.setEnabled(true);
                });
            }

            conn.disconnect();

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "An error occurred during login: " + ex.getMessage(),
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
                enterButton.setEnabled(true);
                nameField.setEnabled(true);
            });
        }
    }

    private int[][] parseMatrix(JsonArray matrixArray) {
        int rows = matrixArray.size();
        int cols = matrixArray.get(0).getAsJsonArray().size();
        System.out.println(rows);
        System.out.println(cols);
        int[][] matrix = new int[rows][cols];

        for (int x = 0; x < rows; x++) {
            JsonArray row = matrixArray.get(x).getAsJsonArray();
            for (int y = 0; y < cols; y++) {
                matrix[x][y] = row.get(y).getAsInt();
            }
        }

        return matrix;
    }

    private void initializeWebSocket(String boardId, int[][] matrix) {
        String websocketWithBoardId = Config.WEBSOCKET_URL + "?boardId=" + boardId;

        if (Config.USE_REAL_CLIENT) {
            client = new RealClient(websocketWithBoardId, this);
        } else {
            client = new MockClient(this, boardId);
        }

        boolean connected = client.connect();

        if (!connected) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Unable to connect to the WebSocket server.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                // Re-enable UI components
                enterButton.setEnabled(true);
                nameField.setEnabled(true);
            });
            return;
        }

//        handleInitialBoard(matrix);

        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "Whiteboard");
            handleInitialBoard(matrix);
        });
    }

    private void initWhiteboardPanel() {
        whiteboardPanel = new JPanel(new BorderLayout(10, 10));
        whiteboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        penButton = new JButton("Pen");
        eraserButton = new JButton("Eraser");

        JSlider penRadiusSlider = new JSlider(JSlider.HORIZONTAL, 1, 50, penRadius);
        penRadiusSlider.setMajorTickSpacing(10);
        penRadiusSlider.setMinorTickSpacing(1);
        penRadiusSlider.setPaintTicks(true);
        penRadiusSlider.setPaintLabels(true);
        penRadiusSlider.setBorder(BorderFactory.createTitledBorder("Pen Radius"));

        penRadiusSlider.addChangeListener(e -> penRadius = penRadiusSlider.getValue());

        toolsPanel.add(penRadiusSlider);
        toolsPanel.add(penButton);
        toolsPanel.add(eraserButton);

        whiteboardCanvas = new Canvas(); // Use default constructor
        whiteboardCanvas.setPreferredSize(new Dimension(800, 600)); // Adjusted size
        whiteboardCanvas.setBackground(Color.WHITE); // Set background color

        whiteboardCanvas.setForeground(Color.BLACK);

        whiteboardCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                isDrawing = true;
                currentPoints.clear();
                addPoint(e.getX(), e.getY(), whiteboardCanvas.getForeground());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isDrawing) {
                    addPoint(e.getX(), e.getY(), whiteboardCanvas.getForeground());
                    sendDrawMessage();
                    isDrawing = false;
                }
            }
        });

        whiteboardCanvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDrawing) {
                    addPoint(e.getX(), e.getY(), whiteboardCanvas.getForeground());
                }
            }
        });

        penButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.BLACK)); // Set pen color to black
        eraserButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.WHITE)); // Set pen color to white (eraser)

        whiteboardPanel.add(toolsPanel, BorderLayout.NORTH);
        whiteboardPanel.add(whiteboardCanvas, BorderLayout.CENTER);
    }

    private void addPoint(int x, int y, Color color) {
        int pen = color.equals(Color.BLACK) ? 1 : 0;
        currentPoints.add(new PointData(x, y, pen));
        drawCircle(x, y, color);
    }

    private void sendDrawMessage() {
        if (currentPoints.isEmpty()) return;

        JsonObject drawMessage = new JsonObject();
        drawMessage.addProperty("type", "DRAW");

        JsonArray pointsArray = new JsonArray();
        for (PointData point : currentPoints) {
            JsonObject pointObj = new JsonObject();
            pointObj.addProperty("x", point.y);
            pointObj.addProperty("y", point.x);
            pointObj.addProperty("pen", point.pen);
            pointsArray.add(pointObj);
        }
        drawMessage.add("points", pointsArray);

        client.sendMessage(gson.toJson(drawMessage));

        currentPoints.clear();
    }

    private void drawCircle(int x, int y, Color color) {
        Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
        g2d.setColor(color);
        g2d.fillOval(x - penRadius, y - penRadius, penRadius * 2, penRadius * 2);
    }

    private void sendLeaveMessage() {
        if (client != null && Config.USE_REAL_CLIENT && username != null) {
            JsonObject leaveMessage = new JsonObject();
            leaveMessage.addProperty("type", "LEAVE");
            leaveMessage.addProperty("username", username);
            client.sendMessage(gson.toJson(leaveMessage));
        }
    }

    public void updateBoard(int[][] matrix) {
        SwingUtilities.invokeLater(() -> {
            Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
            g2d.setColor(Color.WHITE); // Clear the board
            g2d.fillRect(0, 0, whiteboardCanvas.getWidth(), whiteboardCanvas.getHeight());

            System.out.println(matrix.length + " " + matrix[0].length);

            for (int x = 0; x < matrix.length; x++) {
                for (int y = 0; y < matrix[x].length; y++) {
                    if (matrix[x][y] == 1) { // Pen (black)
//                        System.out.println(x + " " + y);
                        g2d.setColor(Color.BLACK);
                        g2d.fillOval(x - penRadius, y - penRadius, penRadius * 2, penRadius * 2);
                    }
                    // If matrix[x][y] == 0, it's white; no need to draw since the background is white
                }
            }
        });
    }

    public void handleInitialBoard(int[][] matrix) {
        // Update the board with the initial matrix
        updateBoard(matrix);
    }

    public void applyPoints(List<PointData> points) {
        SwingUtilities.invokeLater(() -> {
            Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
            for (PointData point : points) {
                Color color = point.pen == 1 ? Color.BLACK : Color.WHITE;
                g2d.setColor(color);
                g2d.fillOval(point.x - penRadius, point.y - penRadius, penRadius * 2, penRadius * 2);
            }
        });
    }

    public static class PointData {
        int x;
        int y;
        int pen;

        public PointData(int x, int y, int pen) {
            this.x = x;
            this.y = y;
            this.pen = pen;
        }
    }
}
