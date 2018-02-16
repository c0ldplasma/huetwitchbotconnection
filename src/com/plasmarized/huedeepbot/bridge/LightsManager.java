package com.plasmarized.huedeepbot.bridge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeResponseCallback;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightConfiguration;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightState;
import com.philips.lighting.hue.sdk.wrapper.utilities.HueColor;

public class LightsManager {

    private List<LightPoint> lights;

    private JSONObject sequences;

    LightsManager(List<LightPoint> lights) {
        this.lights = lights;

        String sequencesString = "";
        File f = new File("sequences.json");
        try (Scanner sc = new Scanner(f)) {
            sequencesString = sc.useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            f.getParentFile().mkdirs();
            try {
                f.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        JSONObject json = new JSONObject(sequencesString);
        sequences = json.getJSONObject("sequences");
    }

    public void executeSequence(String sequenceName) {
        Thread execution = new Thread("Test") {

            public void run() {
                JSONArray actions;
                try {
                    actions = sequences.getJSONArray(sequenceName);
                } catch (JSONException e) {
                    System.out.println("Sequence " + sequenceName + " not found. Doing nothing...");
                    return;
                }

                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);

                    String actionType = action.getString("type");
                    if (actionType.equals("sleep")) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(action.getLong("duration"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (actionType.equals("light")) {
                        changeLight(action.getInt("light-id"), action.getInt("r"), action.getInt("g"), action.getInt("b"));
                    }
                }
            }
        };
        System.out.println(execution.getName());
        execution.start();
    }

    /**
     *
     * @param lightId position in lights list
     * @param r red from 0-255
     * @param g green from 0-255
     * @param b blue from 0-255
     */
    private void changeLight(int lightId, int r, int g, int b) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        HueColor color = new HueColor(
                new HueColor.RGB(r, g, b),
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());

        LightState lightState = new LightState();
        lightState.setXY(color.getXY().x, color.getXY().y);

        lights.get(lightId).updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
            @Override
            public void handleCallback(Bridge bridge, ReturnCode returnCode, List responses, List errors) {
                //if (returnCode == ReturnCode.SUCCESS) {
                    // ...
                //} else {
                    // ...
                //}
            }
        });
    }
}
