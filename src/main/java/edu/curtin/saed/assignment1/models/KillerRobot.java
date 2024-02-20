
/*Author : Vishmi Kalansooriya
 * File Name : MovementHandler.java
 * Purpose:  Represents a Killer Robot within the game arena.
 * Last modified on: 11/09/2023
 */
package edu.curtin.saed.assignment1.models;

public class KillerRobot {
    private int robotId;
    private int robotDelay;
    private double x;
    private double y;
    private int cornerIndex;
    private double destinationX;
    private double destinationY;
    private long movementStartTime;
    private double previousX;
    private double previousY;
    private double targetX;
    private double targetY;
    private int lastCornerIndex = -1;
    private boolean moving;

    /**
     * Initializes a new KillerRobot with the given robot ID and delay.
     *
     * @param robotId    The unique identifier for the robot.
     * @param robotDelay The delay before the robot starts moving.
     */

    public KillerRobot(int robotId, int robotDelay) {
        this.robotId = robotId;
        this.robotDelay = robotDelay;

    }

    // Getters and Setters

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public int getRobotDelay() {
        return robotDelay;
    }

    public void setRobotDelay(int robotDelay) {
        this.robotDelay = robotDelay;
    }

    public double getRobotX() {
        return x;
    }

    public double getRobotY() {
        return y;
    }

    public void setRobotPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public int getCornerIndex() {
        return cornerIndex;
    }

    public void setCornerIndex(int cornerIndex) {
        this.cornerIndex = cornerIndex;
    }

    public boolean isMoving() {
        return moving;
    }

    public double getPreviousX() {
        return previousX;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public double getPreviousY() {
        return previousY;
    }

    public double getDestinationX() {
        return destinationX;
    }

    public double getDestinationY() {
        return destinationY;
    }

    public long getMovementStartTime() {
        return movementStartTime;
    }

    public void setLastCornerIndex(int lastCornerIndex) {
        this.lastCornerIndex = lastCornerIndex;
    }

    public int getLastCornerIndex() {
        return lastCornerIndex;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public void setTargetPosition(double targetX, double targetY) {
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public void setPreviousPosition(double x, double y) {
        this.previousX = x;
        this.previousY = y;
    }

}
