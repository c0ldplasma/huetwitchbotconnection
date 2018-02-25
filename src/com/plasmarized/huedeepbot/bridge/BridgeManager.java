package com.plasmarized.huedeepbot.bridge;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;

import com.philips.lighting.hue.sdk.wrapper.HueLog;
import com.philips.lighting.hue.sdk.wrapper.Persistence;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnection;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeConnectionType;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedCallback;
import com.philips.lighting.hue.sdk.wrapper.connection.BridgeStateUpdatedEvent;
import com.philips.lighting.hue.sdk.wrapper.connection.ConnectionEvent;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscovery;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryCallback;
import com.philips.lighting.hue.sdk.wrapper.discovery.BridgeDiscoveryResult;
import com.philips.lighting.hue.sdk.wrapper.domain.Bridge;
import com.philips.lighting.hue.sdk.wrapper.domain.BridgeBuilder;
import com.philips.lighting.hue.sdk.wrapper.domain.DomainType;
import com.philips.lighting.hue.sdk.wrapper.domain.HueError;
import com.philips.lighting.hue.sdk.wrapper.domain.ReturnCode;
import com.philips.lighting.hue.sdk.wrapper.domain.device.Device;
import com.philips.lighting.hue.sdk.wrapper.domain.device.light.LightPoint;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridge;
import com.philips.lighting.hue.sdk.wrapper.knownbridges.KnownBridges;
import com.plasmarized.huedeepbot.main.ViewMain;

public class BridgeManager {

    static {
        // Load the huesdk native library before calling any SDK method
        try {
            System.loadLibrary("huesdk");
        } catch (UnsatisfiedLinkError e) {
            //System.setProperty("java.library.path", "/lib");
            File lib = new File("lib/" + System.mapLibraryName("huesdk"));
            System.out.println(lib.getAbsolutePath());
            System.load(lib.getAbsolutePath());
        }
    }

    private BridgeDiscovery bridgeDiscovery;
    private List<BridgeDiscoveryResult> bridgeDiscoveryResults;

    private Bridge bridge;

    private LightsManager lights;

    private ViewMain view;

    private boolean tryedUPNP = false;

    /**
     * Only instance of this class (Singleton)
     */
    private static BridgeManager instance;

    private BridgeManager () {

        File persistence = new File("persistence/");
        System.out.println(persistence.getAbsolutePath());
        Persistence.setStorageLocation(persistence.getAbsolutePath(), "HueDeepbot");
        HueLog.setConsoleLogLevel(HueLog.LogLevel.INFO);


        String bridgeIp = getLastUsedBridgeIp();
        if (bridgeIp == null) {
            startBridgeDiscovery();
        } else {
            connectToBridge(bridgeIp);
        }
    }

    public static synchronized BridgeManager getInstance() {
        if (BridgeManager.instance == null) {
            BridgeManager.instance = new BridgeManager();
        }
        return BridgeManager.instance;
    }

    public void init(ViewMain view) {
        this.view = view;
    }


    /**
     * Use the KnownBridges API to retrieve the last connected bridge
     * @return Ip address of the last connected bridge, or null
     */
    private String getLastUsedBridgeIp() {
        List<KnownBridge> bridges = KnownBridges.getAll();

        if (bridges.isEmpty()) {
            return null;
        }

        //return Collections.max(bridges, (KnownBridge a, KnownBridge b) -> a.getLastConnected().compareTo(b.getLastConnected())).getIpAddress();
        return Collections.max(bridges, (KnownBridge a, KnownBridge b) -> Comparator.comparing(KnownBridge::getLastConnected).compare(a, b)).getIpAddress();
    }

    /**
     * Start the bridge discovery search
     * Read the documentation on meethue for an explanation of the bridge discovery options
     */
    private void startBridgeDiscovery() {
        disconnectFromBridge();
        bridgeDiscovery = new BridgeDiscovery();
        // ALL Include [UPNP, IPSCAN, NUPNP] but in some nets UPNP and NUPNP is not working properly
        bridgeDiscovery.search(BridgeDiscovery.BridgeDiscoveryOption.IPSCAN, bridgeDiscoveryCallback);
        //updateUI(UIState.BridgeDiscoveryRunning, "Scanning the network for hue bridges...");
        System.out.println("Scanning the network for hue bridges...");
    }

    public void connectToBridge(int bridgeIndex) {
        connectToBridge(bridgeDiscoveryResults.get(bridgeIndex).getIP());
    }
    /**
     * Use the BridgeBuilder to create a bridge instance and connect to it
     */
    private void connectToBridge(String bridgeIp) {
        stopBridgeDiscovery();
        disconnectFromBridge();

        bridge = new BridgeBuilder("app name", "device name")
                .setIpAddress(bridgeIp)
                .setConnectionType(BridgeConnectionType.LOCAL)
                .setBridgeConnectionCallback(bridgeConnectionCallback)
                .addBridgeStateUpdatedCallback(bridgeStateUpdatedCallback)
                .build();

        bridge.connect();

        System.out.println("Bridge IP: " + bridgeIp);
        System.out.println("Connecting to bridge...");
    }

    /**
     * Stops the bridge discovery if it is still running
     */
    private void stopBridgeDiscovery() {
        if (bridgeDiscovery != null) {
            bridgeDiscovery.stop();
            bridgeDiscovery = null;
        }
    }

    /**
     * Disconnect a bridge
     * The hue SDK supports multiple bridge connections at the same time,
     * but for the purposes of this demo we only connect to one bridge at a time.
     */
    private void disconnectFromBridge() {
        if (bridge != null) {
            bridge.disconnect();
            bridge = null;
        }
    }

    /**
     * The callback that receives the results of the bridge discovery
     */
    private BridgeDiscoveryCallback bridgeDiscoveryCallback = new BridgeDiscoveryCallback() {
        @Override
        public void onFinished(final List<BridgeDiscoveryResult> results, final ReturnCode returnCode) {


            if (returnCode == ReturnCode.SUCCESS) {
                //bridgeDiscoveryListView.setAdapter(new BridgeDiscoveryResultAdapter(getApplicationContext(), results));
                bridgeDiscoveryResults = results;

                //updateUI(UIState.BridgeDiscoveryResults, "Found " + results.size() + " bridge(s) in the network.");
                System.out.println("Found " + results.size() + " bridge(s) in the network.");
                //System.out.println("Connecting to the first in the list: " + results.get(0).getUniqueID());
                //connectToBridge(results.get(0).getIP());
                if (results.size() < 1) {
                    if (!tryedUPNP) {
                        view.appendLog("No Bridge found with IPScan! Trying UPNP an NUPNP... (Can take up to 30 seconds)");
                        bridgeDiscovery.search(BridgeDiscovery.BridgeDiscoveryOption.UPNP_AND_NUPNP, bridgeDiscoveryCallback);
                        tryedUPNP = true;
                    } else {
                        view.appendLog("No Bridge found!");
                    }
                } else {
                    LinkedHashMap<String, String> foundBridges = new LinkedHashMap<>();
                    for (BridgeDiscoveryResult result : results) {
                        foundBridges.put(result.getUniqueID(), result.getIP());
                    }
                    view.appendLog("Select one of the following bridges.");
                    view.addBridgeList(foundBridges);
                }
            } else if (returnCode == ReturnCode.STOPPED) {
                //Log.i(TAG, "Bridge discovery stopped.");
                System.out.println("Bridge discovery stopped.");
            } else {
                //updateUI(UIState.Idle, "Error doing bridge discovery: " + returnCode);
                System.out.println("Error doing bridge discovery: " + returnCode);
            }
            // Set to null to prevent stopBridgeDiscovery from stopping it
            bridgeDiscovery = null;
        }
    };

    /**
     * The callback that receives bridge connection events
     */
    private BridgeConnectionCallback bridgeConnectionCallback = new BridgeConnectionCallback() {
        @Override
        public void onConnectionEvent(BridgeConnection bridgeConnection, ConnectionEvent connectionEvent) {
            //Log.i(TAG, "Connection event: " + connectionEvent);

            switch (connectionEvent) {
                case LINK_BUTTON_NOT_PRESSED:
                    view.appendLog("Press the link button to authenticate.");
                    break;

                case COULD_NOT_CONNECT:
                    view.appendLog("Could not connect. Search bridges...");
                    startBridgeDiscovery();
                    break;

                case CONNECTION_LOST:
                    view.appendLog("Connection lost. Attempting to reconnect.");
                    break;

                case CONNECTION_RESTORED:
                    view.appendLog("Connection restored.");
                    break;

                case DISCONNECTED:
                    view.appendLog("You disconnected.");
                    break;

                default:
                    break;
            }
        }

        @Override
        public void onConnectionError(BridgeConnection bridgeConnection, List<HueError> list) {
            for (HueError error : list) {
                view.appendLog("Connection error: " + error.toString());
            }
        }
    };

    /**
     * The callback the receives bridge state update events
     */
    private BridgeStateUpdatedCallback bridgeStateUpdatedCallback = new BridgeStateUpdatedCallback() {
        @Override
        public void onBridgeStateUpdated(Bridge bridge, BridgeStateUpdatedEvent bridgeStateUpdatedEvent) {
            //Log.i(TAG, "Bridge state updated event: " + bridgeStateUpdatedEvent);
            System.out.println("Bridge state updated event: " + bridgeStateUpdatedEvent);

            switch (bridgeStateUpdatedEvent) {
                case INITIALIZED:
                    // The bridge state was fully initialized for the first time.
                    // It is now safe to perform operations on the bridge state.

                    List<LightPoint> li = (List<LightPoint>)(List<? extends Device>)bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT);
                    lights = new LightsManager(li);
                    //lights.executeSequence("Blitz");

                    view.appendLog("Connected to Bridge: " + bridge.getIdentifier());
                    view.appendLog("Found " + li.size() + " lights connected to this bridge.");
                    break;

                case LIGHTS_AND_GROUPS:
                    view.appendLog("Light updated!");
                    updateLights();
                    break;

                default:
                    break;
            }
        }
    };

    public void executeSequence(String sequenceName, String threadPoolName) {
        lights.executeSequence(sequenceName, threadPoolName);
    }

    private void updateLights() {
        List<LightPoint> li = (List<LightPoint>)(List<? extends Device>)bridge.getBridgeState().getDevices(DomainType.LIGHT_POINT);
        lights.setLights(li);
    }

    public Map<String, ExecutorService> getQueues() {
        return lights!=null?lights.getQueues():null;
    }
}
