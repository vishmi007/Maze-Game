package edu.curtin.saed.assignment1.arena;

import javafx.scene.canvas.*;
import javafx.scene.control.TextArea;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import edu.curtin.saed.assignment1.controllers.MovementHandler;
import edu.curtin.saed.assignment1.controllers.WallBuilder;
import edu.curtin.saed.assignment1.models.KillerRobot;
import edu.curtin.saed.assignment1.models.RobotMoveCommand;
import edu.curtin.saed.assignment1.models.Wall;

import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A JavaFX GUI element that displays a grid on which you can draw images, text
 * and lines.
 */
public class JFXArena extends Pane {
    // Represents an image to draw, retrieved as a project resource.

    private Image robot1;
    private Image citadel1;
    private Image wall1;
    private Image wallWeaken1;
    private long gameStartTime;

    // The following values are arbitrary, and you may need to modify them according
    // to the
    // requirements of your application.
    private int gridWidth = 9;
    private int gridHeight = 9;
    private double citadelX = 4.0;
    private double citadelY = 4.0;

    private double gridSquareSize; // Auto-calculated
    private Canvas canvas; // Used to provide a 'drawing surface'.
    private MovementHandler movementHandler;
    private List<Wall> builtWalls = new ArrayList<>();

    private List<ArenaListener> listeners = null;
    private List<KillerRobot> robotsInPlay = new ArrayList<>();

    /* default */ double[] cornersX = { 0, 0, gridWidth - 1, gridWidth - 1 };
    /* default */double[] cornersY = { 0, gridHeight - 1, 0, gridHeight - 1 };

    private WallBuilder wallBuilder;
    private static final long WALL_BUILD_DELAY = 2000; // 2000 milliseconds
    private StringProperty logMessageProperty = new SimpleStringProperty("");
    private int robotsDestroyedCount = 0;
    private int robotCounter = 0;
    private boolean isGameOver = false;

    /**
     * Creates a new arena object, loading the robot image and initialising a
     * drawing surface.
     */
    public JFXArena() {
        TextArea logger = new TextArea();
        logger.setEditable(false);
        canvas = new Canvas();
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        movementHandler = new MovementHandler(this);
        initImages();

        Timeline robotInsertionTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1.5), event -> insertKillerRobot()));
        robotInsertionTimeline.setCycleCount(Timeline.INDEFINITE);
        robotInsertionTimeline.play();

        Timeline wallUpdateTimeline = new Timeline(
                new KeyFrame(Duration.millis(WALL_BUILD_DELAY), event -> wallBuilder.updateWallConstruction()));
        wallUpdateTimeline.setCycleCount(Animation.INDEFINITE);
        wallUpdateTimeline.play();
        gameStartTime = System.currentTimeMillis();

    }

    /**
     * Initializes the WallBuilder and registers it.
     */
    public void initialize() {
        wallBuilder = new WallBuilder(robotsInPlay);
        registerWallBuilder(wallBuilder);
    }

    /*
     * Method Name:loadImage
     * Purpose: Loads an image from a file.
     */

    private Image loadImage(String fileName) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new AssertionError("Cannot find image file " + fileName);
            }
            return new Image(is);
        } catch (IOException e) {
            throw new AssertionError("Cannot load image file " + fileName, e);
        }
    }

    /**
     * Initializes the images used in the arena.
     */
    private void initImages() {
        robot1 = loadImage("1554047213.png");
        citadel1 = loadImage("rg1024-isometric-tower.png");
        wall1 = loadImage("181478.png");
        wallWeaken1 = loadImage("181479.png");
    }

    /**
     * Gets a list of robots currently in play.
     */

    public List<KillerRobot> getRobotsInPlay() {
        return robotsInPlay;
    }

    /**
     * Gets the X-coordinate of the citadel.
     */

    public double getCitadelX() {
        return citadelX;
    }

    /**
     * Gets the Y-coordinate of the citadel.
     */

    public double getCitadelY() {
        return citadelY;
    }

    /**
     * Gets the width of the grid.
     */

    public int getGridWidth() {
        return gridWidth;
    }

    /**
     * Gets the height of the grid.
     */

    public int getGridHeight() {
        return gridHeight;
    }

    /**
     * Gets the count of destroyed robots.
     */

    public int getRobotsDestroyedCount() {
        return robotsDestroyedCount;
    }

    /**
     * Gets the game start time.
     */

    public long getGameStartTime() {
        return gameStartTime;
    }

    /**
     * Logs an event message.
     */

    private void logEvent(String message) {
        Platform.runLater(() -> {
            logMessageProperty.set(logMessageProperty.get() + message + "\n");
        });
    }

    /**
     * Gets the log message property.
     */
    public StringProperty logMessageProperty() {
        return logMessageProperty;
    }

    /**
     * Checks if the game is over.
     */

    public boolean getIsGameOver() {
        return isGameOver;
    }

    /*
     * Method to detect where a robot should be placed in the grid randomly.
     */

    public void insertKillerRobot() {
        double x;
        double y;

        List<Integer> chooseCorner = new ArrayList<>(Arrays.asList(0, 1, 2, 3));

        synchronized (robotsInPlay) {
            for (KillerRobot robot : robotsInPlay) {
                int occupiedCorner = checkCornerIndexAvailability(robot.getRobotX(), robot.getRobotY());
                if (occupiedCorner != -1) {
                    chooseCorner.remove(Integer.valueOf(occupiedCorner));
                }
            }
        }
        if (chooseCorner.isEmpty()) {
            return;
        }

        // Shuffling to randomize
        Collections.shuffle(chooseCorner);

        int cornerIndex = chooseCorner.remove(0);
        // Determine the next corner to place the robot
        x = cornersX[cornerIndex];
        y = cornersY[cornerIndex];

        int delay = randomDelay();
        KillerRobot newRobot = new KillerRobot(robotCounter++, delay);
        synchronized (robotsInPlay) {
            robotsInPlay.add(newRobot);
        }

        newRobot.setRobotPosition(x, y);
        logEvent("Robot created at (" + x + "," + y + ")");

        newRobot.setTargetPosition(getCitadelX(), getCitadelY());

        // Create a move command for the new robot and add it to the moveCommandQueue
        RobotMoveCommand moveCommand = new RobotMoveCommand(newRobot, getCitadelX(), getCitadelY());

        // Attempt to acquire the lock for the target grid square
        int gridX = (int) x;
        int gridY = (int) y;
        boolean lockAcquired = false;

        while (!lockAcquired) {
            if (movementHandler.getGridLocks()[gridX][gridY].tryLock()) {
                lockAcquired = true;
            } else {
                // Sleep for a short duration before retrying
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        try {
            movementHandler.addMoveCommand(moveCommand);
        } finally {
            // Release the lock for the target grid cell
            movementHandler.getGridLocks()[gridX][gridY].unlock();
        }

        Platform.runLater(() -> requestLayout());

        if (atLeastOneRobotAtCitadel()) {
            // Stop the game if at least one robot is at the citadel
            movementHandler.stopGame();

        }
    }

    /**
     * Checks if at least one robot is at the citadel.
     */

    public boolean atLeastOneRobotAtCitadel() {
        synchronized (robotsInPlay) {
            for (KillerRobot robot : robotsInPlay) {

                if ((robot.getRobotX() == getCitadelX()) && robot.getRobotY() == getCitadelY()) {
                    isGameOver = true;
                    return true;
                }

            }

        }

        return false;
    }

    /*
     * A method to generate a random delay to place robots between 500s to 2000s.
     */
    private int randomDelay() {
        return ThreadLocalRandom.current().nextInt(500, 2001);
    }

    /**
     * Registers a WallBuilder.
     */

    public void registerWallBuilder(WallBuilder wallBuilder) {
        this.wallBuilder = wallBuilder;
    }

    public void addListener(ArenaListener newListener) {
        if (listeners == null) {
            listeners = new LinkedList<>();
            setOnMouseClicked(event -> {
                int gridX = (int) (event.getX() / gridSquareSize);
                int gridY = (int) (event.getY() / gridSquareSize);

                if (gridX < gridWidth && gridY < gridHeight) {
                    for (ArenaListener listener : listeners) {
                        listener.squareClicked(gridX, gridY);
                    }
                    buildWall(gridX, gridY);
                }
            });
        }
        listeners.add(newListener);
    }

    /**
     * Builds a wall at the specified grid coordinates if no wall is present.
     *
     * @param gridX The X-coordinate of the grid.
     * @param gridY The Y-coordinate of the grid.
     */

    public void buildWall(int gridX, int gridY) {
        // Check if a wall is already present at the clicked location
        synchronized (builtWalls) {
            for (Wall wall : builtWalls) {
                if (wall.getGridX() == gridX && wall.getGridY() == gridY) {
                    return;
                }
            }
        }

        // If there is no wall at the clicked location, build a new wall
        System.out.println("Build wall at (" + gridX + "," + gridY + ")");
        if (wallBuilder != null) {
            // Attempt to build a wall at the specified grid coordinates
            boolean success = wallBuilder.buildWall(gridX, gridY);

            if (success) {
                // Create a new wall and add it to the list of built walls (initially not
                // weakened)
                Wall newWall = new Wall(gridX, gridY, false);
                builtWalls.add(newWall);
                logEvent("Wall built at (" + gridX + "," + gridY + ")");
                // Trigger a redraw
                Platform.runLater(() -> requestLayout());
            }
        }
    }

    /**
     * Removes a wall at the specified grid coordinates.
     *
     * @param gridX The X-coordinate of the grid.
     * @param gridY The Y-coordinate of the grid.
     */

    public void removeWall(int gridX, int gridY) {
        synchronized (builtWalls) {
            // Iterate through the list of built walls and find the one to remove
            Iterator<Wall> iterator = builtWalls.iterator();
            while (iterator.hasNext()) {
                Wall wall = iterator.next();
                if (wall.getGridX() == gridX && wall.getGridY() == gridY) {
                    // Remove the wall from the list
                    iterator.remove();
                    // Trigger a redraw
                    Platform.runLater(() -> requestLayout());
                    break; // Exit the loop once the wall is removed
                }
            }
        }
    }

    /**
     * Checks for collisions between robots and walls, and handles the interactions.
     */

    private void checkRobotWallCollisions() {
        synchronized (robotsInPlay) {
            for (KillerRobot robot : robotsInPlay) {
                int gridX = (int) robot.getRobotX();
                int gridY = (int) robot.getRobotY();

                synchronized (builtWalls) {
                    for (Wall wall : builtWalls) {
                        if (wall.getGridX() == gridX && wall.getGridY() == gridY) {
                            if (wall.isWeakened()) {
                                System.out.println(wall.isWeakened());
                                // Wall is already weakened, remove it
                                logEvent("Wall at (" + gridX + "," + gridY + ") removed");
                                removeWall(gridX, gridY);
                            } else {
                                // Wall is initially built, weaken it
                                wall.weaken();
                                logEvent("Wall at (" + gridX + "," + gridY + ") weakened");
                                System.out.println(wall.isWeakened());
                            }

                            // Destroy the robot
                            robotsInPlay.remove(robot);
                            robotsDestroyedCount++;
                            logEvent("Robot " + robot.getRobotId() + " destroyed");
                            // Trigger a redraw
                            Platform.runLater(() -> requestLayout());
                            return; // Exit the loop once the collision is handled
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks the availability of a corner at the specified coordinates (x, y).
     *
     * @param x The X-coordinate.
     * @param y The Y-coordinate.
     * @return The index of the available corner or -1 if none are available.
     */

    private int checkCornerIndexAvailability(double x, double y) {
        synchronized (robotsInPlay) {
            for (int i = 0; i < cornersX.length; i++) {
                if (cornersX[i] == x && cornersY[i] == y) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * This method is called in order to redraw the screen, either because the user
     * is manipulating
     * the window, OR because you've called 'requestLayout()'.
     *
     * You will need to modify the last part of this method; specifically the
     * sequence of calls to
     * the other 'draw...()' methods. You shouldn't need to modify anything else
     * about it.
     */
    @Override
    public void layoutChildren() {
        super.layoutChildren();
        GraphicsContext gfx = canvas.getGraphicsContext2D();
        gfx.clearRect(0.0, 0.0, canvas.getWidth(), canvas.getHeight());

        // First, calculate how big each grid cell should be, in pixels. (We do need to
        // do this
        // every time we repaint the arena, because the size can change.)
        gridSquareSize = Math.min(
                getWidth() / (double) gridWidth,
                getHeight() / (double) gridHeight);

        double arenaPixelWidth = gridWidth * gridSquareSize;
        double arenaPixelHeight = gridHeight * gridSquareSize;

        // Draw the arena grid lines. This may help for debugging purposes, and just
        // generally
        // to see what's going on.
        gfx.setStroke(Color.DARKGREY);
        gfx.strokeRect(0.0, 0.0, arenaPixelWidth - 1.0, arenaPixelHeight - 1.0); // Outer edge

        for (int gridX = 1; gridX < gridWidth; gridX++) // Internal vertical grid lines
        {
            double x = (double) gridX * gridSquareSize;
            gfx.strokeLine(x, 0.0, x, arenaPixelHeight);
        }

        for (int gridY = 1; gridY < gridHeight; gridY++) // Internal horizontal grid lines
        {
            double y = (double) gridY * gridSquareSize;
            gfx.strokeLine(0.0, y, arenaPixelWidth, y);

        }

        drawImage(gfx, citadel1, getCitadelX(), getCitadelY());

        synchronized (robotsInPlay) {
            for (KillerRobot robot : robotsInPlay) {
                drawImage(gfx, robot1, robot.getRobotX(), robot.getRobotY());
                drawLabel(gfx, "Robot " + robot.getRobotId(), robot.getRobotX(), robot.getRobotY());
            }
        }

        synchronized (robotsInPlay) {
            for (KillerRobot robot : robotsInPlay) {
                movementHandler.randomMove(robot);

            }
        }

        checkRobotWallCollisions();

        synchronized (builtWalls) {
            for (Wall wall : builtWalls) {
                if (wall.isWeakened()) {
                    // Draw a normal wall image
                    drawImage(gfx, wallWeaken1, wall.getGridX(), wall.getGridY());

                } else {
                    // Draw the weaken wall image
                    drawImage(gfx, wall1, wall.getGridX(), wall.getGridY());
                }
            }
        }

    }

    /**
     * Draw an image in a specific grid location. *Only* call this from within
     * layoutChildren().
     *
     * 
     * 
     * Note that the grid location can be fractional, so that (for instance), you
     * can draw an image
     * at location (3.5,4), and it will appear on the boundary between grid cells
     * 
     * 
     * 
     * You shouldn't need to modify this method.
     */
    private void drawImage(GraphicsContext gfx, Image image, double gridX, double gridY) {
        // Get the pixel coordinates representing the centre of where the image is to be
        // drawn.
        double x = (gridX + 0.5) * gridSquareSize;
        double y = (gridY + 0.5) * gridSquareSize;

        // We also need to know how "big" to make the image. The image file has a
        // natural width
        // and height, but that's not necessarily the size we want to draw it on the
        // screen. We
        // do, however, want to preserve its aspect ratio.
        double fullSizePixelWidth = robot1.getWidth();
        double fullSizePixelHeight = robot1.getHeight();

        double displayedPixelWidth, displayedPixelHeight;
        if (fullSizePixelWidth > fullSizePixelHeight) {
            // Here, the image is wider than it is high, so we'll display it such that it's
            // as
            // wide as a full grid cell, and the height will be set to preserve the aspect
            // ratio.
            displayedPixelWidth = gridSquareSize;
            displayedPixelHeight = gridSquareSize * fullSizePixelHeight / fullSizePixelWidth;
        } else {
            // Otherwise, it's the other way around -- full height, and width is set to
            // preserve the aspect ratio.
            displayedPixelHeight = gridSquareSize;
            displayedPixelWidth = gridSquareSize * fullSizePixelWidth / fullSizePixelHeight;
        }

        // Actually put the image on the screen.
        gfx.drawImage(image,
                x - displayedPixelWidth / 2.0, // Top-left pixel coordinates.
                y - displayedPixelHeight / 2.0,
                displayedPixelWidth, // Size of displayed image.
                displayedPixelHeight);
    }

    /**
     * Displays a string of text underneath a specific grid location. *Only* call
     * this from within
     * layoutChildren().
     * 
     * You shouldn't need to modify this method.
     */
    private void drawLabel(GraphicsContext gfx, String label, double gridX, double gridY) {
        gfx.setTextAlign(TextAlignment.CENTER);
        gfx.setTextBaseline(VPos.TOP);
        gfx.setStroke(Color.BLUE);
        gfx.strokeText(label, (gridX + 0.5) * gridSquareSize, (gridY + 1.0) * gridSquareSize);
    }

}
