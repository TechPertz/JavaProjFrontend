package com.example.swinggradleapp;

import com.example.swinggradleapp.client.Client;
import com.example.swinggradleapp.client.MockClient;
import com.example.swinggradleapp.client.RealClient;
import com.example.swinggradleapp.datatransfer.BoardDataTransfer;
import com.example.swinggradleapp.utils.Config;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

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
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    // Client Interface
    private Client client;

    // Gson instance for JSON handling
    private final Gson gson = new Gson();

    // Timer for periodic board updates
    private Timer updateTimer;

    // Pen radius
    private int penRadius = 10; // Default radius, can be modified

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
        this.setSize(1280, 720); // Set initial size
        this.setLocationRelativeTo(null); // Center on screen
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new BorderLayout(10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        loginPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel promptLabel = new JLabel("Enter Your Name:");
        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(200, 30)); // Adjust size for aesthetics
        enterButton = new JButton("Enter");
        enterButton.setPreferredSize(new Dimension(100, 30)); // Adjust size for aesthetics

        // Set a larger font for the nameField
        Font textFieldFont = new Font("SansSerif", Font.PLAIN, 48); // Creating a new Font object
        nameField.setFont(textFieldFont); // Applying the font to the JTextField


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

            // Initialize Client
            client = Config.USE_REAL_CLIENT
                    ? new RealClient(Config.SERVER_URL, MainFrame.this)
                    : new MockClient();
            boolean connected = client.connect();

            if (!connected) {
                JOptionPane.showMessageDialog(MainFrame.this,
                        "Unable to connect to the server.",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Send username to server
            JsonObject joinMessage = new JsonObject();
            joinMessage.addProperty("type", "JOIN");
            joinMessage.addProperty("username", name);
            client.sendMessage(gson.toJson(joinMessage));

            // Note: Server should respond with a confirmation message containing the board matrix
            // Handled in RealClient's onMessage

            // Switch to Whiteboard Panel
            cardLayout.show(mainPanel, "Whiteboard");

            // Start periodic board updates if using RealClient
            if (Config.USE_REAL_CLIENT) {
                startPeriodicUpdates();
            }
        });

        // Layout adjustments for better aesthetics
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.add(promptLabel);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(nameField);
        inputPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        inputPanel.add(enterButton);
        inputPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginPanel.add(inputPanel, BorderLayout.CENTER);
    }

    private void initWhiteboardPanel() {
        whiteboardPanel = new JPanel(new BorderLayout(10, 10));
        whiteboardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Left Panel: User List
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        userScrollPane.setBorder(BorderFactory.createTitledBorder("Users"));

        // Top Panel: Tools
        JPanel toolsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        penButton = new JButton("Pen");
        eraserButton = new JButton("Eraser");

        // Inside initWhiteboardPanel()

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
        whiteboardCanvas.setPreferredSize(new Dimension(1280, 720)); // Set preferred size
        whiteboardCanvas.setBackground(Color.WHITE); // Set background color

        // Set initial drawing color
        whiteboardCanvas.setForeground(Color.BLACK);

        // Mouse Event Handlers
        whiteboardCanvas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                drawCircle(e.getX(), e.getY(), whiteboardCanvas.getForeground());
                sendPixelUpdate(e.getX(), e.getY(), whiteboardCanvas.getForeground());
            }
        });

        whiteboardCanvas.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                drawCircle(e.getX(), e.getY(), whiteboardCanvas.getForeground());
                sendPixelUpdate(e.getX(), e.getY(), whiteboardCanvas.getForeground());
            }
        });

        // Right Panel: Whiteboard
        whiteboardPanel.add(toolsPanel, BorderLayout.NORTH);
        whiteboardPanel.add(whiteboardCanvas, BorderLayout.CENTER);
        whiteboardPanel.add(userScrollPane, BorderLayout.EAST);

        // Action Listeners for Tools using Lambda
        penButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.BLACK)); // Set pen color to black
        eraserButton.addActionListener(e -> whiteboardCanvas.setForeground(Color.WHITE)); // Set pen color to white (eraser)
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
     * Sends a pixel update to the server in JSON format.
     *
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @param color The new color of the pixel.
     */
    private void sendPixelUpdate(int x, int y, Color color) {
        JsonObject pixelUpdate = new JsonObject();
        pixelUpdate.addProperty("type", "DRAW");
        pixelUpdate.addProperty("x", x);
        pixelUpdate.addProperty("y", y);
        pixelUpdate.addProperty("color", color.equals(Color.BLACK) ? 1 : 0); // 1 for pen (black), 0 for eraser (white)
        client.sendMessage(gson.toJson(pixelUpdate));
    }

    /**
     * Starts a timer that requests the latest board matrix from the server every second.
     */
    private void startPeriodicUpdates() {
        updateTimer = new Timer();
        updateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                JsonObject updateRequest = new JsonObject();
                updateRequest.addProperty("type", "REQUEST_UPDATE");
                client.sendMessage(gson.toJson(updateRequest));
            }
        }, 0, 1000); // Every 1000 milliseconds (1 second)
    }

    /**
     * Stops the periodic update timer.
     */
    private void stopPeriodicUpdates() {
        if (updateTimer != null) {
            updateTimer.cancel();
        }
    }

    /**
     * Adds a user to the user list.
     *
     * @param user The username to add.
     */
    public void addUser(String user) {
        SwingUtilities.invokeLater(() -> userListModel.addElement(user));
    }

    /**
     * Removes a user from the user list.
     *
     * @param user The username to remove.
     */
    public void removeUser(String user) {
        SwingUtilities.invokeLater(() -> userListModel.removeElement(user));
    }

    /**
     * Draws a line on the canvas based on server-sent data.
     *
     * @param x        The x-coordinate.
     * @param y        The y-coordinate.
     * @param colorHex The color in hexadecimal format.
     */
    public void drawLine(double x, double y, String colorHex) {
        SwingUtilities.invokeLater(() -> {
            Graphics2D g2d = (Graphics2D) whiteboardCanvas.getGraphics();
            g2d.setColor(Color.decode(colorHex));
            g2d.fillOval((int) x - penRadius, (int) y - penRadius, penRadius * 2, penRadius * 2);
        });
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
}
