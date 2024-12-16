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
import java.awt.image.BufferedImage;
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
    private DrawingPanel drawingPanel;

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

        // Set the size exactly to BOARD_WIDTH x BOARD_HEIGHT + space for tools (e.g., 200 width for side panel)
        this.setSize(Config.BOARD_WIDTH + 200, Config.BOARD_HEIGHT + 100); // Example: 1000x700 for BOARD_WIDTH=800, BOARD_HEIGHT=600
        this.setMinimumSize(new Dimension(Config.BOARD_WIDTH + 200, Config.BOARD_HEIGHT + 100)); // Set minimum size
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


    /**
     * Simulates the login process when USE_REAL_CLIENT is false.
     *
     * @param name The username entered by the user.
     */
    private void simulateLogin(String name) {
        this.username = name;
        String mockUserId = "mockUser123";
        this.boardId = "mockBoard456";
        String confirmationMsg = "Welcome, " + name + "! (Mock Connection)";

        int[][] matrix = new int[Config.BOARD_HEIGHT][Config.BOARD_WIDTH]; // Adjusted size to match whiteboardCanvas

        SwingUtilities.invokeLater(() -> {
            initializeWebSocket(boardId, matrix);

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

            JsonObject loginPayload = new JsonObject();
            loginPayload.addProperty("username", name);

            OutputStream os = conn.getOutputStream();
            byte[] input = gson.toJson(loginPayload).getBytes("utf-8");
            os.write(input, 0, input.length);
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) { // HTTP OK or Created
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

    /**
     * Parses the board matrix from JsonArray to 2D int array.
     *
     * @param matrixArray The JsonArray representing the board matrix.
     * @return The 2D int array.
     */
    private int[][] parseMatrix(JsonArray matrixArray) {
        int rows = matrixArray.size();
        if (rows == 0) {
            throw new IllegalArgumentException("Received empty matrix from server.");
        }
        int cols = matrixArray.get(0).getAsJsonArray().size();
        System.out.println("Matrix Dimensions: Rows = " + rows + ", Columns = " + cols);
        int[][] matrix = new int[rows][cols];

        for (int y = 0; y < rows; y++) { // Iterate over rows (y)
            JsonArray row = matrixArray.get(y).getAsJsonArray();
            if (row.size() != cols) {
                throw new IndexOutOfBoundsException("Row " + y + " has " + row.size() + " columns; expected " + cols + ".");
            }
            for (int x = 0; x < cols; x++) { // Iterate over columns (x)
                matrix[y][x] = row.get(x).getAsInt();
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

        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "Whiteboard");
            handleInitialBoard(matrix);
        });
    }

    /**
     * Initializes the whiteboard panel where users can draw and erase.
     */
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

        penRadiusSlider.addChangeListener(e -> {
            penRadius = penRadiusSlider.getValue();
            System.out.println("Pen Radius set to: " + penRadius);
        });

        toolsPanel.add(penRadiusSlider);
        toolsPanel.add(penButton);
        toolsPanel.add(eraserButton);

        drawingPanel = new DrawingPanel(Config.BOARD_WIDTH, Config.BOARD_HEIGHT);
        drawingPanel.setPreferredSize(new Dimension(Config.BOARD_WIDTH, Config.BOARD_HEIGHT));
        drawingPanel.setBackground(Color.WHITE);

        penButton.addActionListener(e -> {
            drawingPanel.setCurrentColor(Color.BLACK); // Set pen color to black
            System.out.println("Pen tool selected.");
        });
        eraserButton.addActionListener(e -> {
            drawingPanel.setCurrentColor(Color.WHITE); // Set pen color to white (eraser)
            System.out.println("Eraser tool selected.");
        });

        whiteboardPanel.add(toolsPanel, BorderLayout.NORTH);
        whiteboardPanel.add(drawingPanel, BorderLayout.CENTER);
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
            System.out.println("Sent LEAVE message to server.");
        }
    }

    /**
     * Updates the entire board based on the received matrix from the server.
     *
     * @param matrix The 2D array representing the board state.
     */
    public void updateBoard(int[][] matrix) {
        SwingUtilities.invokeLater(() -> {
            drawingPanel.clearCanvas();

            System.out.println("Updating board with matrix of size: " + matrix.length + "x" + matrix[0].length);

            for (int y = 0; y < matrix.length; y++) { // Iterate over rows (y)
                for (int x = 0; x < matrix[y].length; x++) { // Iterate over columns (x)
                    if (matrix[y][x] == 1) { // Pen (black)
                        drawingPanel.drawPoint(y, x, Color.BLACK); // Draw at (row, column) => (y, x)
                    }
                    // If matrix[y][x] == 0, it's white; no need to draw since the background is white
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
        System.out.println("Initial board data loaded.");
    }

    /**
     * Applies a list of points to the canvas.
     *
     * @param points List of points to apply.
     */
    public void applyPoints(List<PointData> points) {
        SwingUtilities.invokeLater(() -> {
            for (PointData point : points) {
                Color color = point.pen == 1 ? Color.BLACK : Color.WHITE;
                drawingPanel.drawPoint(point.x, point.y, color);
            }
            System.out.println("Applied " + points.size() + " points from server.");
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

    /**
     * Custom JPanel for drawing, backed by a BufferedImage for persistent rendering.
     */
    private class DrawingPanel extends JPanel {
        private final BufferedImage canvasImage;
        private Graphics2D g2d;
        private Color currentColor = Color.BLACK;

        public DrawingPanel(int width, int height) {
            this.canvasImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            this.g2d = canvasImage.createGraphics();
            this.g2d.setColor(Color.WHITE);
            this.g2d.fillRect(0, 0, width, height);
            this.g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Add mouse listeners to the DrawingPanel
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    isDrawing = true;
                    currentPoints.clear();
                    int row = e.getY();
                    int col = e.getX();
                    addPoint(row, col, currentColor);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isDrawing) {
                        int row = e.getY();
                        int col = e.getX();
                        addPoint(row, col, currentColor);
                        sendDrawMessage();
                        isDrawing = false;
                    }
                }
            });

            this.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDrawing) {
                        int row = e.getY();
                        int col = e.getX();
                        addPoint(row, col, currentColor);
                    }
                }
            });
        }

        public void setCurrentColor(Color color) {
            this.currentColor = color;
        }

        /**
         * Draws a point on the canvas.
         *
         * @param row   The row (y-coordinate).
         * @param col   The column (x-coordinate).
         * @param color The color to draw.
         */
        public void drawPoint(int row, int col, Color color) {
            // Ensure row and col are within bounds
            if (row < 0) row = 0;
            if (row > Config.BOARD_HEIGHT) row = Config.BOARD_HEIGHT;
            if (col < 0) col = 0;
            if (col > Config.BOARD_WIDTH) col = Config.BOARD_WIDTH;

            g2d.setColor(color);
            g2d.fillOval(col - penRadius, row - penRadius, penRadius * 2, penRadius * 2);
            repaint();
        }

        /**
         * Clears the canvas by filling it with white color.
         */
        public void clearCanvas() {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, canvasImage.getWidth(), canvasImage.getHeight());
            repaint();
            System.out.println("Canvas cleared.");
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(canvasImage, 0, 0, null);
        }
    }

    /**
     * Adds a point to the currentPoints list and draws it on the canvas.
     *
     * @param row   The row (y-coordinate).
     * @param col   The column (x-coordinate).
     * @param color The color to draw.
     */
    private void addPoint(int row, int col, Color color) {
        int pen = color.equals(Color.BLACK) ? 1 : 0;
        currentPoints.add(new PointData(col, row, pen)); // x = column (width), y = row (height)
        drawingPanel.drawPoint(row, col, color);
        System.out.println("Added point: (" + row + ", " + col + ") with pen=" + pen);
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
            pointObj.addProperty("x", point.x); // column (width)
            pointObj.addProperty("y", point.y); // row (height)
            pointObj.addProperty("pen", point.pen);
            pointsArray.add(pointObj);
        }
        drawMessage.add("points", pointsArray);

        client.sendMessage(gson.toJson(drawMessage));
        System.out.println("Sent DRAW message with " + currentPoints.size() + " points.");

        currentPoints.clear();
    }
}
