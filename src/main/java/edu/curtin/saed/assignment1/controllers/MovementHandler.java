/*Author : Vishmi Kalansooriya
 * File Name : MovementHandler.java
 * Purpose: The `MovementHandler` class manages the movement of Killer Robots within the game arena.
 * It handles queuing move commands, executing robot movements, and checking the validity of moves.
 * Last modified on: 11/09/2023
 */

package edu.curtin.saed.assignment1.controllers;

import edu.curtin.saed.assignment1.arena.JFXArena;
import edu.curtin.saed.assignment1.models.KillerRobot;
import edu.curtin.saed.assignment1.models.RobotMoveCommand;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class MovementHandler {
    private ArrayBlockingQueue<RobotMoveCommand> moveCommandQueue = new ArrayBlockingQueue<>(100);
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(7);
    private JFXArena arena;

    private final Map<KillerRobot, Boolean> robotTaskStatus = new HashMap<>();
    private final Timer robotTimer = new Timer();
    private volatile boolean isGameOver = false;

    private Semaphore robotSemaphore;
    private final ReentrantLock[][] gridLocks;
    private final boolean[][] isGridLocked;

    private boolean isMovementThreadRunning = false; // Flag to track movement thread

    public MovementHandler(JFXArena arena) {
        this.arena = arena;
        this.robotSemaphore = new Semaphore(1);
        this.gridLocks = new ReentrantLock[arena.getGridWidth()][arena.getGridHeight()];
        this.isGridLocked = new boolean[arena.getGridWidth()][arena.getGridHeight()];

        for (int i = 0; i < arena.getGridWidth(); i++) {
            for (int j = 0; j < arena.getGridHeight(); j++) {
                gridLocks[i][j] = new ReentrantLock();
                isGridLocked[i][j] = false; // Initialize grid cell locking status
            }
        }
    }

    /**
     * Starts the movement processing thread.
     */

    public void start() {
        if (!isMovementThreadRunning) {
            isMovementThreadRunning = true;
            executor.submit(() -> {
                while (!arena.atLeastOneRobotAtCitadel()) { // Check the game-over flag
                    System.out.println("Movement Handler" + arena.atLeastOneRobotAtCitadel());
                    try {
                        RobotMoveCommand moveCommand = moveCommandQueue.take();
                        int gridX = (int) moveCommand.getDestinationX();
                        int gridY = (int) moveCommand.getDestinationY();

                        // Acquire the lock for the target grid cell
                        synchronized (gridLocks[gridX][gridY]) {
                            gridLocks[gridX][gridY].lock();
                        }

                        try {
                            // Acquire the semaphore before moving the robot
                            robotSemaphore.acquire();

                            randomMove(moveCommand.getRobot());
                        } finally {
                            // Release the semaphore after moving the robot
                            robotSemaphore.release();
                            // Release the lock for the target grid cell
                            synchronized (gridLocks[gridX][gridY]) {
                                gridLocks[gridX][gridY].unlock();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                isMovementThreadRunning = false; // Movement thread is no longer running
            });
        }
    }

    /**
     * Adds a robot movement command to the queue.
     *
     * @param moveCommand The RobotMoveCommand to be added to the queue.
     */

    public void addMoveCommand(RobotMoveCommand moveCommand) {
        try {
            moveCommandQueue.put(moveCommand);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generates a random move for the given robot. Vertically, Horizontally or
     * randomly.
     *
     * @param robot The KillerRobot for which a random move is generated.
     */

    public void randomMove(KillerRobot robot) {
        synchronized (robot) {
            if (robotTaskStatus.containsKey(robot) && robotTaskStatus.get(robot)) {
                // A task is already running for this robot
                return;
            }

            robotTaskStatus.put(robot, true);
        }

        // Check if the game is over
        if (isGameOver) {
            synchronized (robot) {
                robotTaskStatus.put(robot, false);
            }
            return;
        }

        TimerTask moveRandomlyTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    double currentX = robot.getRobotX();
                    double currentY = robot.getRobotY();
                    int currentGridX = (int) currentX;
                    int currentGridY = (int) currentY;

                    // Check if the robot has reached the citadel
                    if (currentGridX == arena.getCitadelX() && currentGridY == arena.getCitadelY()) {
                        synchronized (robot) {
                            robotTaskStatus.put(robot, false); // Mark the task as not running
                        }
                        stopGame(); // Call stopGame when the robot reaches the citadel
                        return; // Stop moving when robot reaches citadel
                    }

                    // Determine the direction to move (either horizontally or vertically)
                    boolean moveHorizontally = Math.random() < 0.5;
                    double newX = currentX;
                    double newY = currentY;
                    int newGridX = currentGridX;
                    int newGridY = currentGridY;

                    if (moveHorizontally) {
                        // Move horizontally
                        if (currentX < arena.getCitadelX()) {
                            newX = currentX + 1;
                            newGridX = (int) newX;
                        } else if (currentX > arena.getCitadelX()) {
                            newX = currentX - 1;
                            newGridX = (int) newX;
                        }
                    } else {
                        // Move vertically
                        if (currentY < arena.getCitadelY()) {
                            newY = currentY + 1;
                            newGridY = (int) newY;
                        } else if (currentY > arena.getCitadelY()) {
                            newY = currentY - 1;
                            newGridY = (int) newY;
                        }
                    }

                    // Acquire the lock for the current grid cell
                    synchronized (gridLocks[currentGridX][currentGridY]) {
                        gridLocks[currentGridX][currentGridY].lock();
                    }

                    try {
                        // Acquire the lock for the new grid cell
                        synchronized (gridLocks[newGridX][newGridY]) {
                            gridLocks[newGridX][newGridY].lock();
                        }

                        try {
                            // Check for a valid move
                            if (isValidMove(newX, newY, robot)) {
                                // Animate the movement
                                animateMovement(robot, currentX, currentY, newX, newY);
                            }
                        } finally {
                            // Release the lock for the new grid cell
                            gridLocks[newGridX][newGridY].unlock();
                        }
                    } finally {
                        // Release the lock for the current grid cell
                        gridLocks[currentGridX][currentGridY].unlock();
                    }
                } finally {
                    // Mark the task as not running
                    synchronized (robot) {
                        robotTaskStatus.put(robot, false);
                    }
                }
            }
        };

        // Execute the first moveRandomlyTask with a random initial delay
        int initialDelay = 500 + (int) (Math.random() * 1501); // Random value between 500 and 2000
        robotTimer.schedule(moveRandomlyTask, initialDelay);
    }

    /**
     * Animates the movement of a KillerRobot from its current position to a target
     * position.
     *
     * @param robot  The KillerRobot to be animated.
     * @param startX The starting X-coordinate of the movement.
     * @param startY The starting Y-coordinate of the movement.
     * @param endX   The target X-coordinate of the movement.
     * @param endY   The target Y-coordinate of the movement.
     */

    private void animateMovement(KillerRobot robot, double startX, double startY, double endX, double endY) {

        if (robot == null || arena == null) {
            return; // Add null checks for relevant objects
        }
        // Create a series of intermediate positions for animation
        int animationSteps = 10;
        double animationDuration = 400.0;
        double stepX = (endX - startX) / animationSteps;
        double stepY = (endY - startY) / animationSteps;

        final double[] intermediateX = { startX };
        final double[] intermediateY = { startY };

        Timeline timeline = new Timeline();
        for (int step = 0; step < animationSteps; step++) {
            int currentStep = step;
            KeyFrame keyFrame = new KeyFrame(
                    Duration.millis(animationDuration / animationSteps * (step + 1)),
                    event -> {
                        // Calculate new intermediate position
                        intermediateX[0] += stepX;
                        intermediateY[0] += stepY;

                        // Update robot position
                        robot.setRobotPosition(intermediateX[0], intermediateY[0]);

                        // Redraw the arena
                        arena.requestLayout();

                        // Check if the animation is complete
                        if (currentStep == animationSteps - 1) {
                            robot.setMoving(false); // Animation complete, set robot as not moving
                            // Execute any additional logic after movement completion here
                        }
                    });
            timeline.getKeyFrames().add(keyFrame);
        }

        robot.setMoving(true); // Set robot as moving
        timeline.play();
    }

    /**
     * Checks if a move is valid for the specified coordinates and robot.
     *
     * @param x            The X-coordinate of the move.
     * @param y            The Y-coordinate of the move.
     * @param robotToCheck The KillerRobot to validate the move for.
     * @return True if the move is valid, false otherwise.
     */

    private boolean isValidMove(double x, double y, KillerRobot robotToCheck) {
        int gridX = (int) x;
        int gridY = (int) y;

        // Check if the move is within the arena bounds
        if (gridX < 0 || gridX >= arena.getGridWidth() || gridY < 0 || gridY >= arena.getGridHeight()) {
            return false; // Out of bounds
        }

        // Check if the target grid cell is locked
        if (isGridLocked[gridX][gridY]) {
            return false; // Grid cell is locked
        }

        // Check if another robot is already at the target grid cell
        synchronized (arena.getRobotsInPlay()) {
            for (KillerRobot otherRobot : arena.getRobotsInPlay()) {
                if (!otherRobot.equals(robotToCheck) && otherRobot.isMoving() &&
                        ((int) otherRobot.getRobotX() == gridX) && ((int) otherRobot.getRobotY() == gridY)) {
                    return false; // Collision detected
                }
            }
        }

        // If no collisions or locks were detected, the move is valid
        return true;
    }

    /**
     * Gets the array of grid locks.
     *
     * @return The array of ReentrantLocks for grid cells.
     */

    public ReentrantLock[][] getGridLocks() {
        return gridLocks;
    }

    /**
     * Checks if a grid cell is locked at the specified coordinates.
     *
     * @param gridX The X-coordinate of the grid cell.
     * @param gridY The Y-coordinate of the grid cell.
     * @return True if the grid cell is locked, false otherwise.
     */

    public boolean isGridLocked(int gridX, int gridY) {
        if (gridX >= 0 && gridX < arena.getGridWidth() && gridY >= 0 && gridY < arena.getGridHeight()) {
            return isGridLocked[gridX][gridY];
        }
        return false; // Return false for out-of-bounds grid coordinates
    }

    public void stopGame() {
        System.out.println("Game over!");

        // Stop the timer for random moves
        isGameOver = true;
        // Shutdown the executor service
        executor.shutdown();

        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (executor.isShutdown() && executor.isTerminated()) {
            System.out.println("All threads have been gracefully terminated.");
        } else {
            System.out.println("Threads are still running.");
        }
    }
}
