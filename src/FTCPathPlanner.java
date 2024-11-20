import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class FTCPathPlanner extends JFrame {
    private JLabel fieldLabel;
    private ArrayList<Box> boxes = new ArrayList<>();
    private static final int FIELD_SIZE_PIXELS = 640;
    private static final int FIELD_SIZE_INCHES = 144;
    private static final int GRID_INTERVAL_INCHES = 24;
    private static final int BOX_SIZE_INCHES = 18;
    private static final int BOX_SIZE_PIXELS = (BOX_SIZE_INCHES * FIELD_SIZE_PIXELS) / FIELD_SIZE_INCHES;
    private Box selectedBox = null; // Box being dragged
    private int dragOffsetX = 0;    // Offset from the click point
    private int dragOffsetY = 0;


    private Image robotImage;

    public FTCPathPlanner() {
        setTitle("FTC Path Planner");
        setSize(700, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Load the robot image
        robotImage = new ImageIcon("src/robot.png").getImage();

        // Load the field image
        Image fieldImage = new ImageIcon("src/field.png").getImage(); // Replace with actual image path

        fieldLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(fieldImage, 0, 0, FIELD_SIZE_PIXELS, FIELD_SIZE_PIXELS, null);

                // Draw gridlines
                g2d.setColor(Color.RED);
                int gridSpacingPixels = (GRID_INTERVAL_INCHES * FIELD_SIZE_PIXELS) / FIELD_SIZE_INCHES;
                for (int i = 0; i <= FIELD_SIZE_PIXELS; i += gridSpacingPixels) {
                    g2d.drawLine(i, 0, i, FIELD_SIZE_PIXELS); // Vertical lines
                    g2d.drawLine(0, i, FIELD_SIZE_PIXELS, i); // Horizontal lines
                }

                // Draw all boxes
                for (int i = 0; i < boxes.size(); i++) {
                    Box box = boxes.get(i);
                    int adjustedY = FIELD_SIZE_PIXELS - box.y;

                    AffineTransform originalTransform = g2d.getTransform();

                    // Translate and rotate
                    g2d.translate(box.x, adjustedY);
                    g2d.rotate(Math.toRadians(box.deg));

                    // Draw robot image centered
                    g2d.drawImage(robotImage, -BOX_SIZE_PIXELS / 2, -BOX_SIZE_PIXELS / 2, BOX_SIZE_PIXELS, BOX_SIZE_PIXELS, null);

                    // Restore transform
                    g2d.setTransform(originalTransform);

                    // Draw marker at center
                    g2d.fillOval(box.x - 5, adjustedY - 5, 10, 10);

                    // **Change the text color and position**
                    g2d.setColor(Color.BLACK); // Use black for better contrast
                    g2d.drawString(
                            String.format("%d (%.1f, %.1f, %.1f°)", i + 1, box.xInches, box.yInches, box.deg),
                            box.x - 30, // Slightly adjust left alignment
                            adjustedY + BOX_SIZE_PIXELS / 2 + 15 // Position text below the robot
                    );

                    // Connect boxes with lines
                    if (i > 0) {
                        Box prev = boxes.get(i - 1);
                        g2d.drawLine(prev.x, FIELD_SIZE_PIXELS - prev.y, box.x, adjustedY);
                    }
                }
            }
        };

        addBox(24, 24, 0);

        fieldLabel.setPreferredSize(new Dimension(FIELD_SIZE_PIXELS, FIELD_SIZE_PIXELS));
        fieldLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point clickedPoint = e.getPoint();
                boolean nearBox = false;
                for (Box box : boxes) {
                    int centerX = box.x;
                    int centerY = FIELD_SIZE_PIXELS - box.y; // Flip Y-axis
                    // Check if the click is within the marker (center dot area)
                    if (clickedPoint.distance(centerX, centerY) <= 10) {
                        selectedBox = box;
                        dragOffsetX = clickedPoint.x - centerX;
                        dragOffsetY = clickedPoint.y - centerY;
                        nearBox = true;
                        break;
                    }
                }

                if (!nearBox) {
                    int adjustedY = FIELD_SIZE_PIXELS - clickedPoint.y; // Flip Y-axis
                    double xInches = (clickedPoint.x / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;
                    double yInches = (adjustedY / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;
                    boxes.add(new Box(clickedPoint.x, adjustedY, xInches, yInches));
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                selectedBox = null; // Stop dragging
            }
        });

        fieldLabel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedBox != null) {
                    Point draggedPoint = e.getPoint();
                    int newX = draggedPoint.x - dragOffsetX;
                    int newY = draggedPoint.y - dragOffsetY;

                    // Update box pixel coordinates
                    selectedBox.x = newX;
                    selectedBox.y = FIELD_SIZE_PIXELS - newY; // Flip Y-axis

                    // Update box real-world coordinates
                    selectedBox.xInches = (newX / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;
                    selectedBox.yInches = (selectedBox.y / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;

                    repaint(); // Repaint the field with updated position
                }
            }
        });


        add(fieldLabel, BorderLayout.CENTER);

        // Input panel for initializing the first box
        JPanel initializePanel = new JPanel(new FlowLayout());
        JTextField xField = new JTextField(5);
        JTextField yField = new JTextField(5);
        JTextField indexField = new JTextField(5); // New index field
        JButton applyButton = new JButton("Apply");

        applyButton.addActionListener(e -> {
            try {
                double xInches = Double.parseDouble(xField.getText());
                double yInches = Double.parseDouble(yField.getText());

                int xPixels = (int) ((xInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);
                int yPixels = (int) ((yInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);

                if (!indexField.getText().isEmpty()) {
                    int index = Integer.parseInt(indexField.getText()) - 1; // Convert to 0-based index
                    if (index >= 0 && index < boxes.size()) {
                        boxes.set(index, new Box(xPixels, yPixels, xInches, yInches));
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid box index.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    boxes.add(new Box(xPixels, yPixels, xInches, yInches));
                }

                repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for X and Y.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        initializePanel.add(new JLabel("X (inches):"));
        initializePanel.add(xField);
        initializePanel.add(new JLabel("Y (inches):"));
        initializePanel.add(yField);
        initializePanel.add(new JLabel("Box Index (optional):"));
        initializePanel.add(indexField);
        initializePanel.add(applyButton);

        // Panel to change angle of a box
        JPanel anglePanel = new JPanel(new FlowLayout());
        JTextField boxIndexField = new JTextField(5);
        JTextField angleField = new JTextField(5);
        JButton setAngleButton = new JButton("Set Angle");

        setAngleButton.addActionListener(e -> {
            try {
                int boxIndex = Integer.parseInt(boxIndexField.getText()) - 1; // 1-based index
                double angle = Double.parseDouble(angleField.getText());

                if (boxIndex >= 0 && boxIndex < boxes.size()) {
                    boxes.get(boxIndex).deg = angle;
                    repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid box index.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for index and angle.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        anglePanel.add(new JLabel("Box Index:"));
        anglePanel.add(boxIndexField);
        anglePanel.add(new JLabel("Angle (°):"));
        anglePanel.add(angleField);
        anglePanel.add(setAngleButton);

        // Panel to remove a box at a specific index
        JPanel removePanel = new JPanel(new FlowLayout());
        JTextField removeIndexField = new JTextField(5);
        JButton removeButton = new JButton("Remove Box");

        removeButton.addActionListener(e -> {
            try {
                int removeIndex = Integer.parseInt(removeIndexField.getText()) - 1; // 1-based index

                if (removeIndex >= 0 && removeIndex < boxes.size()) {
                    boxes.remove(removeIndex);
                    repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid box index.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for index.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        removePanel.add(new JLabel("Remove Box Index:"));
        removePanel.add(removeIndexField);
        removePanel.add(removeButton);

        // Button to generate path plan
        JButton generatePathPlanButton = new JButton("Generate Path Plan");
        generatePathPlanButton.addActionListener(e -> generatePathPlan());

        // Button to save path plan as a file
        JButton savePathPlanButton = new JButton("Save Path Plan");
        savePathPlanButton.addActionListener(e -> savePathPlan());

        // Button to load path plan from a file
        JButton loadPathPlanButton = new JButton("Load Path Plan");
        loadPathPlanButton.addActionListener(e -> loadPathPlan());

        // Combine all controls in a single panel
        JPanel controlsPanel = new JPanel(new GridLayout(6, 1));
        controlsPanel.add(initializePanel);
        controlsPanel.add(anglePanel);
        controlsPanel.add(removePanel);
        controlsPanel.add(generatePathPlanButton);
        controlsPanel.add(savePathPlanButton);
        controlsPanel.add(loadPathPlanButton);

        add(controlsPanel, BorderLayout.SOUTH);
    }

    private void generatePathPlan() {
        if (boxes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No boxes available for export!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder codeBuilder = new StringBuilder();
        Box firstBox = boxes.get(0);
        codeBuilder.append(String.format("localizer.setPoseEstimate(new Pose2d(%.2f, %.2f, Math.toRadians(%.1f)));\n", firstBox.xInches, firstBox.yInches, firstBox.deg));

        for (int i = 1; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            codeBuilder.append(String.format("lineTo(%.2f, %.2f, %.1f);\n", box.xInches, box.yInches, box.deg));
        }

        JTextArea codeArea = new JTextArea(codeBuilder.toString());
        codeArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(codeArea);

        JOptionPane.showMessageDialog(this, scrollPane, "Generated Path Plan", JOptionPane.INFORMATION_MESSAGE);
    }

    private void savePathPlan() {
        if (boxes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No boxes available for export!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder codeBuilder = new StringBuilder();
        Box firstBox = boxes.get(0);
        codeBuilder.append(String.format("localizer.setPoseEstimate(new Pose2d(%.2f, %.2f, Math.toRadians(%.1f)));\n", firstBox.xInches, firstBox.yInches, firstBox.deg));

        for (int i = 1; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            codeBuilder.append(String.format("lineTo(%.2f, %.2f, %.1f);\n", box.xInches, box.yInches, box.deg));
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Path Plan");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(fileToSave)) {
                writer.write(codeBuilder.toString());
                JOptionPane.showMessageDialog(this, "Path plan saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadPathPlan() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Path Plan");
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            try (Scanner scanner = new Scanner(fileToLoad)) {
                boxes.clear(); // Clear current boxes

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("localizer.setPoseEstimate")) {
                        double[] parsed = parsePose2d(line);
                        addBox(parsed[0], parsed[1], parsed[2]);
                    } else if (line.startsWith("lineTo")) {
                        double[] parsed = parseLineTo(line);
                        addBox(parsed[0], parsed[1], parsed[2]);
                    }
                }

                repaint();
                JOptionPane.showMessageDialog(this, "Path plan loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addBox(double xInches, double yInches, double deg) {
        int xPixels = (int) ((xInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);
        int yPixels = (int) ((yInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);
        boxes.add(new Box(xPixels, yPixels, xInches, yInches, deg));
    }

    private double[] parsePose2d(String line) {
        return parseCoordinates(line, "localizer.setPoseEstimate");
    }

    private double[] parseLineTo(String line) {
        return parseCoordinates(line, "lineTo");
    }

    private double[] parseCoordinates(String line, String prefix) {
        // Remove the method prefix, parentheses, and Math.toRadians
        line = line.replace(prefix, "")
                .replace("new Pose2d(", "")
                .replace("Math.toRadians(", "")
                .replace(")", "")
                .replace(";", "")
                .replace("(", "")
                .trim();

        // Split by commas to extract the three double values
        String[] parts = line.split(",");
        return new double[]{
                Double.parseDouble(parts[0].trim()), // x value
                Double.parseDouble(parts[1].trim()), // y value
                Double.parseDouble(parts[2].trim())  // angle in degrees
        };
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FTCPathPlanner planner = new FTCPathPlanner();
            planner.setVisible(true);
        });
    }
}

class Box {
    int x, y; // Center in pixels
    double xInches, yInches; // Coordinates in inches
    double deg; // Angle in degrees

    public Box(int x, int y, double xInches, double yInches) {
        this(x, y, xInches, yInches, 0);
    }

    public Box(int x, int y, double xInches, double yInches, double deg) {
        this.x = x;
        this.y = y;
        this.xInches = xInches;
        this.yInches = yInches;
        this.deg = deg; // Default angle
    }

    @Override
    public String toString() {
        return String.format("Box [x=%.1f, y=%.1f, deg=%.1f]", xInches, yInches, deg);
    }
}
