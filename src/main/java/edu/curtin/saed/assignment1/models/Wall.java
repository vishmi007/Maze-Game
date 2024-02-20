
/*Author : Vishmi Kalansooriya
 * File Name : Wall.java
 * Purpose:  Represents a wall within the game arena.
 * Last modified on: 11/09/2023
 */
package edu.curtin.saed.assignment1.models;

public class Wall {
    private int gridX;
    private int gridY;
    private boolean isBuilt;
    private int robotImpacts;
    private boolean weakened;

    @SuppressWarnings("PMD.UnusedFormalParameter")
    /*
     * I get a PMD warning saying unused parameter weakened, but I am actually
     * taking it to use and I am calling the cunstructor in both the
     * JFX Arena class and WallBuilder class, this boolean value suggests whether
     * the wall is weakned at that point or not! So I had to supress this warning
     */
    public Wall(int gridX, int gridY, boolean weakened) {
        this.gridX = gridX;
        this.gridY = gridY;
        this.isBuilt = true;
        this.weakened = false;
        this.robotImpacts = 0;

    }

    // getters

    public int getGridX() {
        return gridX;
    }

    public int getGridY() {
        return gridY;
    }

    /**
     * Checks if the wall is built.
     *
     * @return True if the wall is built, false otherwise.
     */

    public boolean isBuilt() {
        return isBuilt;
    }

    /**
     * Builds the wall, marking it as constructed.
     */

    public void build() {
        isBuilt = true; // Build the wall
    }

    /**
     * Weakens the wall, marking it as weakened.
     */

    public void weaken() {
        weakened = true; // Mark the wall as weakened
    }

    /**
     * Checks if the wall is weakened.
     *
     * @return True if the wall is weakened, false otherwise.
     */

    public boolean isWeakened() {
        return weakened;
    }

    /**
     * Handles the impact of a robot on the wall.
     * If the wall is built and hasn't reached the maximum allowed robot impacts,
     * the number of robot impacts is increased. After the second impact, the wall
     * is destroyed.
     */
    public void robotImpact() {
        // Handle the impact of a robot on the wall
        if (isBuilt && robotImpacts < 2) {
            // Increase the number of robot impacts
            robotImpacts++;

            if (robotImpacts == 2) {
                // Wall is destroyed after the second robot's impact
                destroyWall();
            }
        }
    }

    /**
     * Destroys the wall, marking it as not built.
     */

    private void destroyWall() {
        isBuilt = false; // Destroy the wall
    }

}
