import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;


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
    private boolean isBlueMode = false; // Flag for rotation mode



    private Image robotImage;

    public FTCPathPlanner() {
        setTitle("FTC Path Planner");
        setSize(700, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Load the robot image
        robotImage = new ImageIcon("src/robot.png").getImage();

        // Load the field image
        Image fieldImage = new ImageIcon("src/field.png").getImage();
        Image flippedfieldImage = new ImageIcon("src/flippedfield.png").getImage();// Replace with actual image path

        fieldLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Calculate the dynamic field size (keep square ratio)
                int panelSize = Math.min(getWidth(), getHeight());
                int dynamicFieldSize = panelSize; // Field size is now dynamic

                // Scale everything based on the dynamic field size
                double scaleFactor = dynamicFieldSize / (double) FIELD_SIZE_PIXELS;

                // Calculate horizontal offset to center the field
                int horizontalOffset = (getWidth() - dynamicFieldSize) / 2;

                // Apply scaling and translation transformations
                g2d.translate(horizontalOffset, 0); // Center field horizontally
                g2d.scale(scaleFactor, scaleFactor);

                // Draw the field image
                if (isBlueMode) {
                    g2d.drawImage(flippedfieldImage, 0, 0, FIELD_SIZE_PIXELS, FIELD_SIZE_PIXELS, null);
                } else {
                    g2d.drawImage(fieldImage, 0, 0, FIELD_SIZE_PIXELS, FIELD_SIZE_PIXELS, null);
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

                    // Draw action label if assigned
                    if (box.action != null) {
                        g2d.setColor(Color.BLACK);
                        int actionX = box.x - 58;
                        int actionY = adjustedY + BOX_SIZE_PIXELS / 2 + 32;
                        g2d.drawString(box.action, actionX, actionY);
                    }

                    // Draw text near the box
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(
                            String.format("%d (%.1f, %.1f, %.1f°)", i + 1, box.xInches, box.yInches, box.deg),
                            box.x - 58,
                            adjustedY + BOX_SIZE_PIXELS / 2 + 15
                    );

                    // Connect boxes with lines
                    if (i > 0) {
                        Box prev = boxes.get(i - 1);
                        g2d.drawLine(prev.x, FIELD_SIZE_PIXELS - prev.y, box.x, adjustedY);
                    }
                }

                // Reset scaling transformation after painting
                g2d.scale(1 / scaleFactor, 1 / scaleFactor);
                g2d.translate(-horizontalOffset, 0);
            }

        };

        addBox(24, 24, 0);

        fieldLabel.setPreferredSize(null); // Allow dynamic resizing

        fieldLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Calculate the dynamic scale factor
                int panelSize = Math.min(fieldLabel.getWidth(), fieldLabel.getHeight());
                double scaleFactor = panelSize / (double) FIELD_SIZE_PIXELS;

                // Calculate the horizontal offset to center the field
                int horizontalOffset = (fieldLabel.getWidth() - panelSize) / 2;

                // Adjust mouse coordinates to the original field coordinate system
                int originalX = (int) ((e.getX() - horizontalOffset) / scaleFactor);
                int originalY = (int) (e.getY() / scaleFactor);

                boolean nearBox = false;
                for (Box box : boxes) {
                    int centerX = box.x;
                    int centerY = FIELD_SIZE_PIXELS - box.y; // Flip Y-axis
                    if (Math.hypot(originalX - centerX, originalY - centerY) <= 10) {
                        selectedBox = box;
                        dragOffsetX = originalX - centerX;
                        dragOffsetY = originalY - centerY;
                        nearBox = true;
                        break;
                    }
                }

                if (!nearBox) {
                    int adjustedY = FIELD_SIZE_PIXELS - originalY; // Flip Y-axis
                    double xInches = (originalX / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;
                    double yInches = (adjustedY / (double) FIELD_SIZE_PIXELS) * FIELD_SIZE_INCHES;
                    boxes.add(new Box(originalX, adjustedY, xInches, yInches));
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
                    // Calculate the dynamic scale factor
                    int panelSize = Math.min(fieldLabel.getWidth(), fieldLabel.getHeight());
                    double scaleFactor = panelSize / (double) FIELD_SIZE_PIXELS;

                    // Calculate the horizontal offset to center the field
                    int horizontalOffset = (fieldLabel.getWidth() - panelSize) / 2;

                    // Adjust mouse coordinates to the original field coordinate system
                    int draggedX = (int) ((e.getX() - horizontalOffset) / scaleFactor);
                    int draggedY = (int) (e.getY() / scaleFactor);

                    int newX = draggedX - dragOffsetX;
                    int newY = draggedY - dragOffsetY;

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

        JPanel rotationPanel = new JPanel(new FlowLayout());
        JButton redButton = new JButton("Red");
        JButton blueButton = new JButton("Blue");

        redButton.addActionListener(e -> {
            isBlueMode = false;
            repaint();
        });

        blueButton.addActionListener(e -> {
            isBlueMode = true;
            repaint();
        });

        rotationPanel.add(redButton);
        rotationPanel.add(blueButton);

        add(rotationPanel, BorderLayout.NORTH); // Add rotation buttons at the top

        // Action selection panel
        JPanel actionPanel = new JPanel(new FlowLayout());
        JComboBox<String> actionDropdown = new JComboBox<>(new String[] {
                "None", "groundPickup", "specimanPickup", "specimanDrop", "dropBasket", "resetArm"
        });

        JTextField boxIndexFieldDropDown = new JTextField(5); // Input for box index
        JButton applyActionButton = new JButton("Assign Action");

        // Apply action to a specific box
        applyActionButton.addActionListener(e -> {
            try {
                int boxIndex = Integer.parseInt(boxIndexFieldDropDown.getText()) - 1; // Convert to 0-based index
                String selectedAction = (String) actionDropdown.getSelectedItem();

                if (boxIndex >= 0 && boxIndex < boxes.size()) {
                    boxes.get(boxIndex).action = selectedAction.equals("None") ? null : selectedAction;
                    repaint();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid box index.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for index.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Add components to the panel
        actionPanel.add(new JLabel("Box Index:"));
        actionPanel.add(boxIndexFieldDropDown);
        actionPanel.add(new JLabel("Action:"));
        actionPanel.add(actionDropdown);
        actionPanel.add(applyActionButton);

        // Add actionPanel to the controls
        controlsPanel.add(actionPanel);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                fieldLabel.repaint();
            }
        });


    }

    private void generatePathPlan() {
        if (boxes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No boxes available for export!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder codeBuilder = new StringBuilder();

        // Add the specified header at the top
        codeBuilder.append("package org.firstinspires.ftc.teamcode.autonomous;\n\n");
        codeBuilder.append("import com.qualcomm.robotcore.eventloop.opmode.Autonomous;\n");
        codeBuilder.append("import org.firstinspires.ftc.teamcode.drive.AutonomousController;\n");
        codeBuilder.append("import com.acmerobotics.dashboard.config.Config;\n\n");
        codeBuilder.append("@Autonomous\n");
        codeBuilder.append("@Config\n\n");

        // Generate class header
        codeBuilder.append("public class PathPlan extends AutonomousController {\n\n");

        // Generate variables for each box
        for (int i = 0; i < boxes.size(); i++) {
            Box box = boxes.get(i);
            codeBuilder.append(String.format(
                    "    public static double x%d = %.2f;\n", i + 1, box.xInches));
            codeBuilder.append(String.format(
                    "    public static double y%d = %.2f;\n", i + 1, box.yInches));
            codeBuilder.append(String.format(
                    "    public static double deg%d = %.1f;\n", i + 1, box.deg));
        }

        // Add the runOpMode method
        codeBuilder.append("\n    @Override\n");
        codeBuilder.append("    public void runOpMode() {\n");
        codeBuilder.append(String.format(
                "        initOpMode(y1, 144 - x1, -deg1);\n"));
        codeBuilder.append("        waitForStart();\n\n");
        codeBuilder.append("        if (opModeIsActive()) {\n");
        codeBuilder.append("            mainRun();\n");
        codeBuilder.append("            stop();\n");
        codeBuilder.append("        }\n");
        codeBuilder.append("    }\n\n");

        // Add the mainRun method
        codeBuilder.append("    public void mainRun() {\n");
        for (int i = 1; i < boxes.size(); i++) { // Start from the second box
            Box box = boxes.get(i);
            codeBuilder.append(String.format(
                    "        lineTo(y%d, 144 - x%d, -deg%d);\n", i + 1, i + 1, i + 1));
            if (box.action != null) {
                codeBuilder.append(String.format(
                        "        %s();\n", box.action));
            }
        }

        codeBuilder.append("        stop();\n");
        codeBuilder.append("    }\n");

        // Close the class
        codeBuilder.append("}\n");

        // Display generated code in a dialog
        JTextArea codeArea = new JTextArea(codeBuilder.toString());
        codeArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(codeArea);

        // Create a panel for the popup
        JPanel popupPanel = new JPanel(new BorderLayout());
        popupPanel.add(scrollPane, BorderLayout.CENTER);

        // Add a copy button
        JButton copyButton = new JButton("Copy to Clipboard");
        copyButton.addActionListener(e -> {
            StringSelection stringSelection = new StringSelection(codeBuilder.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            JOptionPane.showMessageDialog(this, "Code copied to clipboard!", "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        popupPanel.add(copyButton, BorderLayout.SOUTH);

        // Show dialog
        JOptionPane.showMessageDialog(this, popupPanel, "Generated Path Plan", JOptionPane.INFORMATION_MESSAGE);
    }




    private void savePathPlan() {
        if (boxes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No boxes available for export!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Path Plan");
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(fileToSave)) {
                // Save the rotation mode
                writer.write("isBlueMode=" + isBlueMode + "\n");

                // Save each box's details
                for (Box box : boxes) {
                    writer.write(String.format(
                            "x=%.2f,y=%.2f,deg=%.1f,action=%s\n",
                            box.xInches, box.yInches, box.deg, (box.action != null ? box.action : "None")
                    ));
                }

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

                // Load rotation mode
                if (scanner.hasNextLine()) {
                    String rotationLine = scanner.nextLine();
                    if (rotationLine.startsWith("isBlueMode=")) {
                        isBlueMode = Boolean.parseBoolean(rotationLine.split("=")[1]);
                    }
                }

                // Load each box's details
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] parts = line.split(",");

                    double xInches = Double.parseDouble(parts[0].split("=")[1]);
                    double yInches = Double.parseDouble(parts[1].split("=")[1]);
                    double deg = Double.parseDouble(parts[2].split("=")[1]);
                    String action = parts[3].split("=")[1];
                    action = action.equals("None") ? null : action;

                    int xPixels = (int) ((xInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);
                    int yPixels = (int) ((yInches / FIELD_SIZE_INCHES) * FIELD_SIZE_PIXELS);

                    boxes.add(new Box(xPixels, yPixels, xInches, yInches, deg, action));
                }

                repaint();
                JOptionPane.showMessageDialog(this, "Path plan loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException | NumberFormatException e) {
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
        return parseCoordinates(line, "initOpMode");
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
    String action; // Assigned action (e.g., "specimanDrop")

    public Box(int x, int y, double xInches, double yInches) {
        this(x, y, xInches, yInches, 0, null);
    }

    public Box(int x, int y, double xInches, double yInches, double deg) {
        this(x, y, xInches, yInches, deg, null);
    }

    public Box(int x, int y, double xInches, double yInches, double deg, String action) {
        this.x = x;
        this.y = y;
        this.xInches = xInches;
        this.yInches = yInches;
        this.deg = deg;
        this.action = action;
    }

    @Override
    public String toString() {
        return String.format("Box [x=%.1f, y=%.1f, deg=%.1f, action=%s]", xInches, yInches, deg, action);
    }
}
