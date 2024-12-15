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

    // Login Panel Components
    private JPanel loginPanel;
    private JTextField nameField;
    private JButton enterButton;

    // Whiteboard Panel Components
    private JPanel whiteboardPanel;
    private JButton penButton;
    private JButton eraserButton;
    private Canvas whiteboardCanvas;

    // Client Interface
    private Client client;

    // Gson instance for JSON handling
    private final Gson gson = new Gson();

    // Pen radius
    private int penRadius = 10; // Default radius, can be modified

    // List to store points during a drawing session
    private List<PointData> currentPoints = new ArrayList<>();

    // Flag to track if mouse is pressed
    private boolean isDrawing = false;

    // Board ID for WebSocket connection
    private String boardId;

    // Username (used for LEAVE message in real client)
    private String username;

    public MainFrame(String title) {
        super(title);

        // Initialize CardLayout
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Initialize Panels
        initLoginPanel();
        initWhiteboardPanel();

        // Add Panels to CardLayout
        mainPanel.add(loginPanel, "Login");
        mainPanel.add(whiteboardPanel, "Whiteboard");

        // Add mainPanel to JFrame
        this.getContentPane().add(mainPanel);

        // Set initial panel
        cardLayout.show(mainPanel, "Login");

        // Set JFrame properties
        this.setSize(800, 600); // Adjusted initial size for better visibility
        this.setMinimumSize(new Dimension(800, 600)); // Set minimum size
        this.setLocationRelativeTo(null); // Center on screen
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Handle window closing to send LEAVE message
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

    /**
     * Initializes the login panel where users can enter their username.
     */
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

        // Add Action Listener to Enter Button using Lambda
        enterButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Name cannot be empty.",
                        "Validation Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Disable UI components during login
            enterButton.setEnabled(false);
            nameField.setEnabled(false);

            if (Config.USE_REAL_CLIENT) {
                // Perform real login via HTTP POST
                new Thread(() -> performLogin(name)).start();
            } else {
                // Simulate login with mock data
                simulateLogin(name);
            }
        });

        // Layout adjustments for better aesthetics
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

    /**
     * Simulates the login process when USE_REAL_CLIENT is false.
     *
     * @param name The username entered by the user.
     */
    private void simulateLogin(String name) {
        // Generate mock data
        this.username = name;
        String mockUserId = "mockUser123";
        this.boardId = "mockBoard456";
        String confirmationMsg = "Welcome, " + name + "! (Mock Connection)";

        // Generate a simple initial matrix (all zeros)
        int[][] matrix = new int[800][600]; // Adjusted size to match whiteboardCanvas

        // Initialize and connect MockClient
        SwingUtilities.invokeLater(() -> {
            // Initialize WebSocket with mock data
            initializeWebSocket(boardId, matrix);

            // Show confirmation message
            JOptionPane.showMessageDialog(MainFrame.this,
                    confirmationMsg,
                    "Login Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    /**
     * Performs the login POST request to the server.
     *
     * @param name The username entered by the user.
     */
    private void performLogin(String name) {
        try {
            URL url = new URL(Config.LOGIN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Create JSON payload
            JsonObject loginPayload = new JsonObject();
            loginPayload.addProperty("username", name);

            // Write payload to request body
            OutputStream os = conn.getOutputStream();
            byte[] input = gson.toJson(loginPayload).getBytes("utf-8");
            os.write(input, 0, input.length);
            os.close();

            // Get response
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

                // Extract fields
                String userId = responseJson.get("user_id").getAsString();
                boardId = responseJson.get("board_id").getAsString();
                String confirmationMsg = responseJson.get("message").getAsString();
                JsonArray matrixArray = responseJson.get("board_matrix_data").getAsJsonArray();

                // Parse matrix
                int[][] matrix = parseMatrix(matrixArray);

                // Initialize and connect RealClient
                SwingUtilities.invokeLater(() -> {
                    initializeWebSocket(boardId, matrix);
                    // Show confirmation message
                    JOptionPane.showMessageDialog(MainFrame.this,
                            confirmationMsg,
                            "Login Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                });

            } else {
                // Handle non-OK response
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Login failed with response code: " + responseCode,
                            "Login Error",
                            JOptionPane.ERROR_MESSAGE);
                    // Re-enable UI components
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
                // Re-enable UI components
                enterButton.setEnabled(true);
                nameField.setEnabled(true);
            });
        }
    }

    /**
     * Parses the board matrix from JsonArray to 2D int array.
     *
     * @param matrixArray The JsonArray representing the board matrix.
     * @return The 2D int array.
     */
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

    /**
     * Initializes the WebSocket connection after receiving boardId and initial matrix.
     *
     * @param boardId The boardId received from the login response.
     * @param matrix  The initial board matrix.
     */
    private void initializeWebSocket(String boardId, int[][] matrix) {
        // Update the WebSocket URL with boardId as query parameter
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

        // Show Whiteboard Panel
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "Whiteboard");
            // Update the board with the initial matrix
            handleInitialBoard(matrix);
        });
    }

    /**
     * Initializes the whiteboard panel where users can draw and erase.
     */
    private void initWhiteboardPanel() {
        whiteboardPanel = new JPanel(new BorderLayout(10, 10));
        whiteboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Panel: Tools
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        penButton = new JButton("Pen");
        eraserButton = new JButton("Eraser");

        // Pen Radius Slider
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

        // Canvas for Drawing
        whiteboardCanvas = new Canvas(); // Use default constructor
        whiteboardCanvas.setPreferredSize(new Dimension(800, 600)); // Adjusted size
        whiteboardCanvas.setBackground(Color.WHITE); // Set background color

        // Set initial drawing color
        whiteboardCanvas.setForeground(Color.BLACK);

        // Mouse Event Handlers
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

        // Action Listeners for Tools using Lambda
        penButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.BLACK)); // Set pen color to black
        eraserButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.WHITE)); // Set pen color to white (eraser)

        whiteboardPanel.add(toolsPanel, BorderLayout.NORTH);
        whiteboardPanel.add(whiteboardCanvas, BorderLayout.CENTER);
    }

    /**
     * Adds a point to the currentPoints list and draws it on the canvas.
     *
     * @param x     The x-coordinate.
     * @param y     The y-coordinate.
     * @param color The color to draw.
     */
    private void addPoint(int x, int y, Color color) {
        int pen = color.equals(Color.BLACK) ? 1 : 0;
        currentPoints.add(new PointData(x, y, pen));
        drawCircle(x, y, color);
    }

    /**
     * Sends the DRAW message with the list of points.
     */
    private void sendDrawMessage() {
        if (currentPoints.isEmpty()) return;

        JsonObject drawMessage = new JsonObject();
        drawMessage.addProperty("type", "DRAW");

        JsonArray pointsArray = new JsonArray();
        for (PointData point : currentPoints) {
            JsonObject pointObj = new JsonObject();
            pointObj.addProperty("x", point.x);
            pointObj.addProperty("y", point.y);
            pointObj.addProperty("pen", point.pen);
            pointsArray.add(pointObj);
        }
        drawMessage.add("points", pointsArray);

        client.sendMessage(gson.toJson(drawMessage));

        currentPoints.clear();
    }

    /**
     * Draws a filled circle on the canvas at the specified coordinates.
     *
     * @param x     The x-coordinate.
     * @param y     The y-coordinate.
     * @param color The color to draw.
     */
    private void drawCircle(int x, int y, Color color) {
        Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
        g2d.setColor(color);
        g2d.fillOval(x - penRadius, y - penRadius, penRadius * 2, penRadius * 2);
    }

    /**
     * Sends a LEAVE message to the server when the user disconnects.
     */
    private void sendLeaveMessage() {
        if (client != null && Config.USE_REAL_CLIENT && username != null) {
            JsonObject leaveMessage = new JsonObject();
            leaveMessage.addProperty("type", "LEAVE");
            leaveMessage.addProperty("username", username);
            client.sendMessage(gson.toJson(leaveMessage));
        }
    }

    /**
     * Updates the entire board based on the received matrix from the server.
     *
     * @param matrix The 2D array representing the board state.
     */
    public void updateBoard(int[][] matrix) {
        SwingUtilities.invokeLater(() -> {
            Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
            g2d.setColor(Color.WHITE); // Clear the board
            g2d.fillRect(0, 0, whiteboardCanvas.getWidth(), whiteboardCanvas.getHeight());

            // Redraw based on the matrix
            for (int x = 0; x < matrix.length; x++) {
                for (int y = 0; y < matrix[x].length; y++) {
                    if (matrix[x][y] == 1) { // Pen (black)
                        g2d.setColor(Color.BLACK);
                        g2d.fillOval(x - penRadius, y - penRadius, penRadius * 2, penRadius * 2);
                    }
                    // If matrix[x][y] == 0, it's white; no need to draw since the background is white
                }
            }
        });
    }

    /**
     * Handles the confirmation message from the server containing the initial board data.
     *
     * @param matrix The 2D array representing the initial board state.
     */
    public void handleInitialBoard(int[][] matrix) {
        // Update the board with the initial matrix
        updateBoard(matrix);
    }

    /**
     * Applies a list of points to the canvas.
     *
     * @param points List of points to apply.
     */
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

    /**
     * Represents a point with x, y coordinates and pen status.
     */
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
