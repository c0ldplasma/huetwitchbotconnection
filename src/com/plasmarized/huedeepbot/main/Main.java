package com.plasmarized.huedeepbot.main;

import java.io.IOException;


import com.plasmarized.huedeepbot.api.DeepbotApiServer;
import com.plasmarized.huedeepbot.bridge.BridgeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class Main extends Application {

    private BridgeManager bridges;

    private ViewMain view;

    private DeepbotApiServer server;

    @Override
    public void start(Stage primaryStage) {
        view = new ViewMain(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Stage is closing");
            Platform.runLater(() -> view.appendLog("Shutdown Api Server..."));
            try {
                server.closeServer();
            } catch (NullPointerException e) {
                System.out.println("Could not close server because its not running.");
            }
        });

        bridges = BridgeManager.getInstance();
        bridges.init(view);

        try {
            server = new DeepbotApiServer();
        } catch (IOException e) {
            view.appendLog("Port already in use! HueDeepbot already running?");
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}
