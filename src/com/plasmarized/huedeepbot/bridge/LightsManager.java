package com.plasmarized.huedeepbot.bridge;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateCacheType;
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

    private Map<String, ExecutorService> queues = new HashMap<>();

    private List<DoublePair> oldXYs;
    private int[] oldBrightnesses;

    LightsManager(List<LightPoint> lights) {
        this.lights = lights;

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

        StringBuilder sequencesString = new StringBuilder();
        try (Scanner sc = new Scanner(f)) {

            while (sc.hasNext()) {
                sequencesString.append(sc.next());
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
    public void executeSequence(String sequenceName, String threadPoolName) {

        final String tPoolName = (threadPoolName==null) ? "default":threadPoolName;

        Runnable task = new Runnable() {

            public void run() {

                System.out.println("Start execution: " + tPoolName);

                try {
                    TimeUnit.MILLISECONDS.sleep(500);  // Give last executed sequence 100ms time to return to old light state
                } catch (InterruptedException e) {
                    System.out.println("Sleep Interrupted! Program probably got shut down...");
                }

                BridgeManager.getInstance().getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().performOneHeartbeat(BridgeStateCacheType.LIGHTS_AND_GROUPS);
                BridgeManager.getInstance().getBridge().getBridgeConnection(BridgeConnectionType.LOCAL).getHeartbeatManager().performOneHeartbeat(BridgeStateCacheType.SCENES);

                try {
                    TimeUnit.MILLISECONDS.sleep(500);  // Give last executed sequence 100ms time to return to old light state
                } catch (InterruptedException e) {
                    System.out.println("Sleep Interrupted! Program probably got shut down...");
                }
                BridgeManager.getInstance().updateLights();

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
                        System.out.println("Sleep Interrupted! Program probably got shut down...");
                    }
                } else if (actionType.equals("light")) {
                    int lightID = action.getInt("light-id");

                    int transTime = action.getInt("transitionTime");

                    boolean isRGB = action.has("r");

                    int r = 0, g = 0, b = 0;
                    double h = 0, s = 0, v = 0;
                    if (isRGB) {
                        r = action.getInt("r");
                        g = action.getInt("g");
                        b = action.getInt("b");
                    } else {
                        h = action.getDouble("h")/360;
                        s = action.getDouble("s");
                        v = action.getDouble("v");
                    }

                    if (lightID == -1) { //All lights
                        for (int lightIndex = 0; lightIndex < lights.size(); lightIndex++) {
                            if (isRGB) {
                                changeLight(lightIndex, r, g, b, transTime);
                            } else {
                                changeLight(lightIndex, h, s, v, transTime);
                            }
                        }
                    } else { //One specific light
                        if (isRGB) {
                            changeLight(lightID, r, g, b, transTime);
                        } else {
                            changeLight(lightID, h, s, v, transTime);
                        }
                    }
                }
            }

            /**
             * @param lightID position in lights list or -1 for all lights
             * @param r red -1 - 255
             * @param g green -1 - 255
             * @param b blue -1 - 255
             */
            private void changeLight(int lightID, int r, int g, int b, int transTime) {

                HueColor color;
                boolean on;

                if (r == -1) {
                    color = getOldColor(lightID);
                    on = color.getXY().x != 0 || color.getXY().y != 0 || color.getBrightness() != 0;
                } else {
                    color = createHueColorRGB(lightID, r, g, b);
                    on = r != 0 || g != 0 || b != 0;
                }

                LightState lightState = new LightState();
                lightState.setOn(on);
                lightState.setTransitionTime(transTime);
                lightState.setXY(color.getXY().x, color.getXY().y);

                lights.get(lightID).updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
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
            /**
             * @param lightID position in lights list or -1 for all lights
             * @param h hue 0-360
             * @param s saturation 0-100 (%)
             * @param v value 0-100 (%)
             */
            private void changeLight(int lightID, double h, double s, double v, int transTime) {

                HueColor color;
                boolean on;

                if (v == -1) {
                    color = getOldColor(lightID);
                    on = color.getXY().x != 0 || color.getXY().y != 0 || color.getBrightness() != 0;
                } else {
                    System.out.println("h: " + h + " s: " + s + " v: " + v);
                    color = createHueColorHSV(lightID, h, s, v);
                    System.out.println("h: " + color.getHSV().h + " s: " + color.getHSV().s + " v: " + color.getHSV().v);
                    System.out.println("r: " + color.getRGB().r + " g: " + color.getRGB().g + " b: " + color.getRGB().b);
                    on = v != 0;
                }

                LightState lightState = new LightState();
                lightState.setOn(on);
                lightState.setTransitionTime(transTime);
                lightState.setXY(color.getXY().x, color.getXY().y);
                lightState.setBrightness((int)(color.getBrightness()*255));

                lights.get(lightID).updateState(lightState, BridgeConnectionType.LOCAL, new BridgeResponseCallback() {
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

            private HueColor getOldColor(int lightID) {
                double oldX = oldXYs.get(lightID).getValue1();
                double oldY = oldXYs.get(lightID).getValue2();
                int oldB = oldBrightnesses[lightID];
                return createHueColorXYB(lightID, oldX, oldY, oldB);
            }
        };

        if (queues.get(tPoolName) == null) {
            queues.put(tPoolName, Executors.newSingleThreadExecutor());
        }
        queues.get(tPoolName).submit(task);
    }

    private HueColor createHueColorXYB(int lightId, double x, double y, int brightness) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        return new HueColor(
                new HueColor.XY(x, y),
                ((double)brightness)/255,
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
    }

    private HueColor createHueColorRGB(int lightId, int r, int g, int b) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        return new HueColor(
                new HueColor.RGB(r, g, b),
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
    }

    private HueColor createHueColorHSV(int lightId, double h, double s, double v) {
        LightConfiguration lightConfiguration = lights.get(lightId).getLightConfiguration();

        return new HueColor(
                new HueColor.HSV(h, s, v),
                lightConfiguration.getModelIdentifier(),
                lightConfiguration.getSwVersion());
    }


    public void setLights(List<LightPoint> lights) {
        this.lights = lights;
        this.oldXYs = new ArrayList<DoublePair>();
        this.oldBrightnesses = new int[lights.size()];
        System.out.println("ALTE WERTE");
        System.out.println("--------------------");
        for (int i = 0; i < lights.size(); ++i) {
            this.oldXYs.add(lights.get(i).getLightState().getXY());
            System.out.println(lights.get(i).getLightState().getXY().getValue1());
            this.oldBrightnesses[i] = lights.get(i).getLightState().getBrightness();
        }
    }

    public Map<String, ExecutorService> getQueues() {
        return queues;
    }
}
