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
            server.closeServer();

        });

        bridges = BridgeManager.getInstance();
        bridges.init(view);

        try {
            server = new DeepbotApiServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        launch(args);
    }
}
