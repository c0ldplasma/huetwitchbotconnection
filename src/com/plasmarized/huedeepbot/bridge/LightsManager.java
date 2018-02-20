package com.plasmarized.huedeepbot.bridge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import com.philips.lighting.hue.sdk.wrapper.domain.clip.DoublePair;
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
        if(!(f.exists() && !f.isDirectory())) {
            try (PrintWriter writer = new PrintWriter(f)) {
                writer.println("{");
                writer.println("\"sequences\":{");
                writer.println("}");
                writer.println("}");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        try (Scanner sc = new Scanner(f)) {
            while (sc.hasNext()) {
                sequencesString += sc.next();
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }

        JSONObject json = new JSONObject(sequencesString);
        sequences = json.getJSONObject("sequences");
    }

    /**
     * Executes a sequence defined in sequences.json
     * @param sequenceName defined in sequences.json
     */
    public void executeSequence(String sequenceName) {
        Thread execution = new Thread(sequenceName) {

            private List<DoublePair> oldXYs;
            private double[] oldBrightnesses;

            public void run() {

                //Store current light values for later reset operations
                oldXYs = new ArrayList<>();
                oldBrightnesses = new double[lights.size()];
                for (int i = 0; i < lights.size(); i++) {
                    oldXYs.add(lights.get(i).getLightState().getXY());
                    oldBrightnesses[i] = lights.get(i).getLightState().getBrightness();
                }


                JSONArray actions;
                try {
                    actions = sequences.getJSONArray(sequenceName);
                } catch (JSONException e) {
                    System.out.println("Sequence " + sequenceName + " not found. Doing nothing...");
                    return;
                }

                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);
                    executeAction(action);
                }
            }

            void executeAction(JSONObject action) {
                String actionType = action.getString("type");
                if (actionType.equals("sleep")) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(action.getLong("duration"));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else if (actionType.equals("light")) {
                    int transTime = action.getInt("transitionTime");
                    boolean on = action.getInt("r") != 0
                            || action.getInt("g") != 0
                            || action.getInt("b") != 0;

                    if (action.getInt("light-id") == -1) { //All lights
                        for (int lightIndex = 0; lightIndex < lights.size(); lightIndex++) {
                            HueColor color;
                            if (action.getInt("r") == -1) {
                                color = createHueColor(lightIndex, oldXYs.get(lightIndex).getValue1(), oldXYs.get(lightIndex).getValue2(), oldBrightnesses[lightIndex]);
                            } else {
                                color = createHueColor(lightIndex, action.getInt("r"), action.getInt("g"), action.getInt("b"));
                            }
                            changeLight(lightIndex, color, transTime, on);
                        }
                    } else {                                   //One specific light
                        int lightID = action.getInt("light-id");
                        if (action.getInt("r") == -1) {
                            changeLight(lightID, createHueColor(lightID, oldXYs.get(lightID).getValue1(), oldXYs.get(lightID).getValue2(), oldBrightnesses[lightID]), transTime, on);
                        } else {
                            changeLight(lightID, createHueColor(lightID, action.getInt("r"), action.getInt("g"), action.getInt("b")), transTime, on);
                        }
                    }
                }
            }
        };
        System.out.println(execution.getName());
        execution.start();
    }

    private HueColor createHueColor(int lightId, double x, double y, double brightness) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        return new HueColor(
                new HueColor.XY(x, y),
                brightness,
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
    }

    private HueColor createHueColor(int lightId, int r, int g, int b) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        return new HueColor(
                new HueColor.RGB(r, g, b),
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
    }

    /**
     *
     * @param lightId position in lights list or -1 for all lights
     * @param color color created by createHueColor method
     */
    private void changeLight(int lightId, HueColor color, int transTime, boolean on) {

            LightState lightState = new LightState();
            lightState.setOn(on);
            lightState.setTransitionTime(transTime);
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

    public void setLights(List<LightPoint> lights) {
        this.lights = lights;
    }
}
