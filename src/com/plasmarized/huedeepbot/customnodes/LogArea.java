package com.plasmarized.huedeepbot.customnodes;

import java.util.Calendar;

import javafx.scene.control.TextArea;

public class LogArea extends TextArea {
    public void appendLine(String message) {
        Calendar time = Calendar.getInstance();
        int h = time.get(Calendar.HOUR_OF_DAY);
        int m = time.get(Calendar.MINUTE);
        int s = time.get(Calendar.SECOND);
        this.appendText("[" + h + ":" + m + ":" + s + "] " + message + " \n");
    }
}
