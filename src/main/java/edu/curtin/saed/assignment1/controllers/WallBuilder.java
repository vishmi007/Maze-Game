/*Author : Vishmi Kalansooriya
 * File Name : WallBuilder.java
 * Purpose: Manages the construction and state of walls within the game arena.
 * Walls can be built and removed based on certain conditions.
 * Last modified on: 11/09/2023
 */
package edu.curtin.saed.assignment1.controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import edu.curtin.saed.assignment1.models.KillerRobot;
import edu.curtin.saed.assignment1.models.Wall;

public class WallBuilder {
    private static final int MAX_WALLS = 10;
    private static final long WALL_BUILD_DELAY = 2000; // 2000 milliseconds

    private BlockingQueue<WallPosition> wallQueue;
    private List<Wall> builtWalls;
    private int fortressWallCount = 0;
    private long lastWallBuildTime;
    private List<KillerRobot> robotsInPlay;

    public WallBuilder(List<KillerRobot> robotsInPlay) {
        this.wallQueue = new LinkedBlockingQueue<>();
        this.builtWalls = new ArrayList<>();
        this.lastWallBuildTime = 0;
        this.robotsInPlay = robotsInPlay;

    }

    /**
     * Attempts to build a wall at the specified grid coordinates.
     *
     * @param gridX The X-coordinate of the grid.
     * @param gridY The Y-coordinate of the grid.
     * @return true if the wall was successfully added to the construction queue,
     *         false otherwise.
     */

    public boolean buildWall(int gridX, int gridY) {
        if (builtWalls.size() >= MAX_WALLS) {
            // Maximum wall limit reached, ignore the command
            return false;
        }

        synchronized (builtWalls) {
            if (!isSquareOccupied(gridX, gridY) && fortressWallCount < 10) {
                // Check if the grid square is not already occupied by a wall or a robot
                wallQueue.add(new WallPosition(gridX, gridY));
                fortressWallCount++;
                return true;
            }
        }

        return false;
    }

    /**
     * Updates the construction of walls based on the contents of the wall
     * construction queue.
     * Checks if conditions are met for building walls and constructs walls at valid
     * positions.
     */

    public void updateWallConstruction() {

        Iterator<WallPosition> iterator = wallQueue.iterator();

        while (iterator.hasNext()) {
            WallPosition position = iterator.next();

            // Check if we can build a wall at this position based on the conditions
            if (canBuildWall() && !isSquareOccupied(position.getGridX(), position.getGridY())) {
                // Build the wall at this position
                Wall wall = new Wall(position.getGridX(), position.getGridY(), true);
                wall.build();

                builtWalls.add(wall);

                // Remove from the queue
                iterator.remove();
            }
        }

    }

    /**
     * Returns the number of wall construction commands currently in the queue.
     *
     * @return The number of wall construction commands in the queue.
     */

    public int getWallCommandsCount() {
        return wallQueue.size();
    }

    /**
     * Checks whether it's currently possible to build a wall based on maximum wall
     * limits
     * and time since the last wall construction.
     *
     * @return true if a wall can be built, false otherwise.
     */

    private boolean canBuildWall() {
        // Check if the maximum wall limit is reached and if enough time has passed
        // since the last wall construction
        return builtWalls.size() < MAX_WALLS && System.currentTimeMillis() - lastWallBuildTime >= WALL_BUILD_DELAY;
    }

    /**
     * Checks whether a specified grid square is occupied by a wall or a robot.
     *
     * @param gridX The X-coordinate of the grid square.
     * @param gridY The Y-coordinate of the grid square.
     * @return true if the square is occupied, false otherwise.
     */

    private boolean isSquareOccupied(int gridX, int gridY) {
        synchronized (builtWalls) {
            // Check if the square is occupied by a wall
            Optional<Wall> wallToDestroy = builtWalls.stream()
                    .filter(wall -> wall.getGridX() == gridX && wall.getGridY() == gridY)
                    .findFirst();

            if (wallToDestroy.isPresent()) {
                if (wallToDestroy.get().isWeakened()) {
                    builtWalls.remove(wallToDestroy.get());
                    fortressWallCount--;
                } else {
                    wallToDestroy.get().weaken();
                }
                return true; // The square is occupied by a wall
            }

            // Check if the square is occupied by a robot
            boolean robotOccupied = robotsInPlay.stream()
                    .anyMatch(robot -> robot.getRobotX() == gridX && robot.getRobotY() == gridY);

            // Return true if either a wall or a robot occupies the square
            return wallToDestroy.isPresent() || robotOccupied;
        }

    }

    /**
     * Removes a specified wall from the list of constructed walls.
     *
     * @param wallToRemove The wall to be removed.
     */

    public void removeWall(Wall wallToRemove) {
        synchronized (builtWalls) {
            builtWalls.remove(wallToRemove);
            fortressWallCount--;
        }
    }

    /**
     * Returns the list of walls that have been constructed and are present in the
     * game arena.
     *
     * @return The list of constructed walls.
     */

    public List<Wall> getBuiltWalls() {
        return builtWalls;
    }

    /**
     * Represents the position of a wall in terms of grid coordinates (X and Y).
     */

    private static class WallPosition {
        private final int gridX;
        private final int gridY;

        public WallPosition(int gridX, int gridY) {
            this.gridX = gridX;
            this.gridY = gridY;

        }

        /**
         * Gets the X-coordinate of the wall's position in the grid.
         *
         * @return The X-coordinate.
         */

        public int getGridX() {
            return gridX;
        }

        /**
         * Gets the Y-coordinate of the wall's position in the grid.
         *
         * @return The Y-coordinate.
         */

        public int getGridY() {
            return gridY;
        }

    }
}
