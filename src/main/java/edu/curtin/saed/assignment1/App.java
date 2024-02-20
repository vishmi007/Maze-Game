package edu.curtin.saed.assignment1;

import edu.curtin.saed.assignment1.arena.JFXArena;
import edu.curtin.saed.assignment1.controllers.WallBuilder;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class App extends Application {
    public static void main(String[] args) {
        launch();
    }

    private JFXArena arena;
    private WallBuilder wallBuilder;
    private Label scoreLabel;
    private Label wallCommandsLabel;
    private Label robotsDestroyedLabel;
    private int lastScore = 0;

    /* default */ boolean wallBuilt = false;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Vishmi's Robot Game!");
        arena = new JFXArena();
        wallBuilder = new WallBuilder(arena.getRobotsInPlay()); // Pass the list of KillerRobots from the arena
        arena.addListener((x, y) -> {

            if (x == y) {
                wallBuilt = false;
            } else {
                wallBuilt = wallBuilder.buildWall(x, y);
            }

        });
        arena.registerWallBuilder(wallBuilder);

        ToolBar toolbar = new ToolBar();

        // Create labels for score, wall commands, and robots destroyed
        scoreLabel = new Label("Score: 0");
        wallCommandsLabel = new Label("Wall Commands: 0");
        robotsDestroyedLabel = new Label("Robots Destroyed: 0");

        toolbar.getItems().addAll(scoreLabel, new Separator(), wallCommandsLabel, new Separator(),
                robotsDestroyedLabel);

        TextArea logger = new TextArea();

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(arena, logger);
        arena.setMinWidth(300.0);

        BorderPane contentPane = new BorderPane();
        contentPane.setTop(toolbar);
        contentPane.setCenter(splitPane);

        Scene scene = new Scene(contentPane, 800, 800);
        stage.setScene(scene);
        stage.show();

        // Bind the TextArea to the log message property
        logger.textProperty().bind(arena.logMessageProperty());

        // Start the wall construction animation timer
        AnimationTimer wallConstructionTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                wallBuilder.updateWallConstruction();
                updateLabels(); // Update labels when the wall construction timer runs
            }
        };
        wallConstructionTimer.start();
    }

    // Calculate the player's score based on the provided rules
    private int calculateScore() {
        int totalScore = 0;

        if (!arena.getIsGameOver()) {
            // Calculate score based on the time elapsed (10 points per second)
            int timeElapsed = (int) (System.currentTimeMillis() - arena.getGameStartTime()) / 1000;
            int timeScore = timeElapsed * 10;

            // Calculate score based on the number of robots destroyed (100 points per
            // robot)
            int robotsDestroyed = arena.getRobotsDestroyedCount();
            int robotScore = robotsDestroyed * 100;

            // Calculate the total score
            totalScore = timeScore + robotScore;
        }

        return totalScore;
    }

    // Update labels for score, wall commands, and robots destroyed
    private void updateLabels() {

        // Check if the game is over
        if (arena.atLeastOneRobotAtCitadel()) {
            // The game is over, display the last calculated score
            scoreLabel.setText("Score: " + lastScore);
            wallCommandsLabel.setText("Wall Commands: 0");
            robotsDestroyedLabel.setText("Robots Destroyed: 0");
        } else {
            // The game is not over, update labels with real-time data
            int score = calculateScore();
            lastScore = score; // Update the lastScore variable
            scoreLabel.setText("Score: " + score);

            // Update the wall commands label with the current number of queued-up
            // wall-building commands
            int wallCommands = wallBuilder.getWallCommandsCount();
            wallCommandsLabel.setText("Wall Commands: " + wallCommands);

            // Update the robots destroyed label with the current count
            int robotsDestroyed = arena.getRobotsDestroyedCount();
            robotsDestroyedLabel.setText("Robots Destroyed: " + robotsDestroyed);
        }

    }

}
