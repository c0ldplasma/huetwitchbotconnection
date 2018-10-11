package com.plasmarized.huedeepbot.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.plasmarized.huedeepbot.bridge.BridgeManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class DeepbotApiServer {

    private HttpServer server;

    private final int PORT = 8000;

    private final String resp;

    public DeepbotApiServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();

        File f = new File("response.txt");
        StringBuilder sequencesString = new StringBuilder();
        try {
            try (Scanner sc = new Scanner(f).useDelimiter("'");){
                while (sc.hasNext()) {
                    sequencesString.append(sc.next());
                }
            }
        }
        catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        this.resp = sequencesString.toString();
    }

    public void closeServer() {
        server.stop(1);
    }

    /**
     * Handles API calls of format: http://localhost:<port>/api...
     */
    class MyHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange call) throws IOException {
            System.out.println(call.getRequestURI());
            call.sendResponseHeaders(200, resp.length());
            OutputStream os = call.getResponseBody();
            os.write(resp.getBytes());
            os.close();

            Map<String, String> result = queryToMap(call.getRequestURI().getQuery());
            BridgeManager.getInstance().executeSequence(result.get("sequence"), result.get("thread"));
        }
    }

    private Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
