/*Author : Vishmi Kalansooriya
 * File Name : RobotMoveCommand.java
 * Purpose:  Represents a move command for a Killer Robot within the game arena.
 * Last modified on: 11/09/2023
 */

package edu.curtin.saed.assignment1.models;

public class RobotMoveCommand {
    private final KillerRobot robot;
    private final double destinationX;
    private final double destinationY;
    private int delay;

    /**
     * Initializes a new RobotMoveCommand with the specified robot and destination
     * coordinates.
     *
     * @param robot        The KillerRobot to which the command is applied.
     * @param destinationX The X-coordinate of the destination.
     * @param destinationY The Y-coordinate of the destination.
     */
    public RobotMoveCommand(KillerRobot robot, double destinationX, double destinationY) {
        this.robot = robot;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
    }
    // getters and setters

    public KillerRobot getRobot() {
        return robot;
    }

    public double getDestinationX() {
        return destinationX;
    }

    public double getDestinationY() {
        return destinationY;
    }

    public int getDelay() {
        return delay;
    }

}
