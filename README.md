# FTC Path Planner & Simulator (FTCPP)

A custom, ground-up path-planning tool written in Kotlin and Java to build, test, and export autonomous robot trajectories in seconds.

## The Problem
Early in the season, we spent upwards of 20 hours before each competition manually coding, deploying, and tuning autonomous paths. Using hardcoded, rigid coordinates made it incredibly slow to make minor adjustments at the field, wasting precious practice time and league match readiness.

## The Solution: FTCPP
We built our own custom visual path-planner and simulator from scratch. It models the real 144" x 144" FTC field using scaled graphics and a CAD image overlay of our robot. Instead of guessing coordinates, we just click on the field map to place coordinates, adjust heading angles, and assign mechanism actions at each step.

---

## Core Features

### 1. 1:1 CAD Scaling & Collision Visualization
* Uses a high-resolution CAD export of our robot frame (`robot.png`), scaled exactly to a real-world 18" x 18" footprint.
* Renders the robot orientation dynamically on the canvas, showing exactly how the chassis will align relative to field walls and scoring elements.

### 2. Click-to-Plot Coordinate System
* Translates on-screen pixels directly into real-world coordinate systems (inches) mapped to the FTC field origin.
* Waypoints are connected with visual path segments, showing the complete autonomous trajectory at a glance.

### 3. Integrated Action Sequence Pipeline
* Assign robot-specific mechanism actions directly to waypoints (e.g., `groundPickup`, `specimanDrop`, `dropBasket`, `resetArm`).
* The planner automatically structures these: preparing/moving the arm to a `"Ready"` state before the movement, executing the `lineTo()` trajectory, and running the `"Go"` sequence once the target pose is reached.

### 4. Roadrunner Code Generation (Three-Click Export)
* Instantly generates ready-to-run autonomous OpModes extending `AutonomousController`.
* Generates static coordinates mapped to `@Config` fields for live-tuning via FTC Dashboard.
* Copies the complete clean Java source file directly to your clipboard.

### 5. Persistent Coordinate Serialization
* Saves and loads complete paths to lightweight `.txt` files containing comma-separated coordinate metadata (`x=...,y=...,deg=...,action=...`), allowing the team to version-control paths in Git.

---

## Technical Architecture
* **GUI Engine**: Java Swing / AWT (flexible dynamic scaling layout).
* **Coordinate Engine**: Pixel-to-inch affine transformations modeling a 640px square grid to a 144" boundary.
* **Autonomous Format**: Standard `Pose2d` mapping, reversing heading orientation to align with mecanum drive models.
