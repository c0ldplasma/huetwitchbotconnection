package com.plasmarized.huedeepbot.main;

import java.util.LinkedHashMap;
import java.util.Map;


import com.plasmarized.huedeepbot.bridge.BridgeManager;
import com.plasmarized.huedeepbot.customnodes.LogArea;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ViewMain {

    private Stage primaryStage;

    private BorderPane root;

    private ListView<String> lvBridges;

    private LogArea taLog;

    ViewMain(Stage primStage) {
        this.primaryStage = primStage;

        initView();
    }

    private void initView() {
        try {

            taLog = new LogArea();
            taLog.appendLine("Initializing...");
            taLog.setEditable(false);

            root = new BorderPane();
            root.setTop(taLog);

            Scene scene = new Scene(root, 400,400);
            scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

            primaryStage.setTitle("HueDeepbot");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void addBridgeList(LinkedHashMap<String, String> foundBridges) {
        Platform.runLater(() -> {
                ObservableList<String> items = FXCollections.observableArrayList();

                for (Map.Entry<String, String> entry : foundBridges.entrySet()) {
                    String bridgeID = entry.getKey();
                    String bridgeIP = entry.getValue();

                    items.add("Bridge: " + bridgeID + ", IP: " + bridgeIP);
                }

                lvBridges = new ListView<>();
                lvBridges.setItems(items);
                root.setCenter(lvBridges);

                Button btnConnect = new Button("Connect");
                btnConnect.setOnAction((ActionEvent event) -> {
                        int selectedBridge = lvBridges.getSelectionModel().getSelectedIndex();

                        if (selectedBridge >= 0) {
                            BridgeManager.getInstance().connectToBridge(selectedBridge);
                        }
                });
                root.setBottom(btnConnect);
        });
    }

    public void appendLog(String status) {
        Platform.runLater(() -> taLog.appendLine(status));
    }
}
