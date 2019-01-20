/*
 * Decompiled with CFR 0_132.
 *
 * Could not load the following classes:
 *  java.lang.invoke.StringConcatFactory
 *  javafx.application.Platform
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 *  javafx.event.ActionEvent
 *  javafx.event.Event
 *  javafx.event.EventHandler
 *  javafx.scene.Node
 *  javafx.scene.Parent
 *  javafx.scene.Scene
 *  javafx.scene.control.Button
 *  javafx.scene.control.ListView
 *  javafx.scene.control.MultipleSelectionModel
 *  javafx.scene.image.Image
 *  javafx.scene.layout.BorderPane
 *  javafx.stage.Stage
 */
package com.plasmarized.huedeepbot.main;

import java.util.LinkedHashMap;
import java.util.Map;
import com.plasmarized.huedeepbot.bridge.BridgeManager;
import com.plasmarized.huedeepbot.customnodes.LogArea;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class ViewMain {
    private Stage primaryStage;
    private BorderPane root;
    private ListView<String> lvBridges;
    private LogArea taLog;

    ViewMain(Stage primStage) {
        this.primaryStage = primStage;
        this.initView();
    }

    private void initView() {
        try {
            this.taLog = new LogArea();
            this.taLog.appendLine("Initializing...");
            this.taLog.setEditable(false);
            this.root = new BorderPane();
            this.root.setTop(this.taLog);
            Scene scene = new Scene(this.root, 650.0, 400.0);
            //scene.getStylesheets().add(this.getClass().getResource("application.css").toExternalForm());
            this.primaryStage.setTitle("HueDeepbot");
            this.primaryStage.getIcons().addAll(new Image("file:icons/icon.png"), new Image("file:icons/icon16.png"));
            this.primaryStage.setScene(scene);
            this.primaryStage.show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBridgeList(LinkedHashMap<String, String> foundBridges) {
        Platform.runLater(() -> {
            ObservableList items = FXCollections.observableArrayList();
            for (Map.Entry entry : foundBridges.entrySet()) {
                String bridgeID = (String)entry.getKey();
                String bridgeIP = (String)entry.getValue();
                items.add("Bridge: " + bridgeID + ", IP: " + bridgeIP);
            }
            this.lvBridges = new ListView<>();
            this.lvBridges.setItems(items);
            this.root.setCenter(this.lvBridges);
            Button btnConnect = new Button("Connect");
            btnConnect.setOnAction(event -> {
                int selectedBridge = this.lvBridges.getSelectionModel().getSelectedIndex();
                if (selectedBridge >= 0) {
                    BridgeManager.getInstance().connectToBridge(selectedBridge);
                }
            });
            this.root.setBottom(btnConnect);
        });
    }

    public void appendLog(String status) {
        Platform.runLater(() -> this.taLog.appendLine(status));
    }
}

