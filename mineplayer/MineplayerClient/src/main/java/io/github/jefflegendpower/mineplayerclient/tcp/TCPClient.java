package io.github.jefflegendpower.mineplayerclient.tcp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.jefflegendpower.mineplayerclient.env.Environment;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.jefflegendpower.mineplayerclient.client.MineplayerClient.getEnv;
import static io.github.jefflegendpower.mineplayerclient.client.MineplayerClient.setEnv;

public class TCPClient {
    private String serverAddress;
    private int serverPort;
    private Socket clientSocket;
    private PrintWriter out;
    private DataInputStream in;

    private AtomicBoolean running = new AtomicBoolean(true);

    public void start(String address, int port, boolean test) {
        serverAddress = address;
        serverPort = port;

        while (running.get()) {
            try {
                clientSocket = new Socket(serverAddress, serverPort);
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new DataInputStream(clientSocket.getInputStream());
                System.out.println("Connected to server");

                String initMessage = readMessage(in);
                if (!test) {
                    JsonObject init = new Gson().fromJson(initMessage, JsonObject.class);
                    String context = init.get("context").getAsString().toUpperCase(Locale.ROOT);
                    out.println(context);
                    switch (context) {
                        case "ENV" -> envHandler();
                        case "HUMAN" -> humanHandler();
                    }
                } else {
                    System.out.println("Testing...");
                    System.out.println(initMessage);
                    System.out.println("Disconnecting...");
                    disconnect();
                    break;
                }
            } catch (IOException e) {
                System.out.println("Failed to connect to the server. Retrying...");
                try {
                    Thread.sleep(2000); // Wait for 2 seconds before retrying
                } catch (InterruptedException ignored) {}
            }
        }
    }

    private void envHandler() throws IOException {
        String initMessage = readMessage(in);
        System.out.println("Init message received");

        setEnv(new Environment(out, clientSocket.getOutputStream(), this::disconnect));

        boolean initialized = getEnv().handleMessage(initMessage);
        if (!initialized) {
            System.out.println("Failed to initialize");
            disconnect();
            return;
        }

        while (running.get() && clientSocket.isConnected()) {
            try {
                String input = readMessage(in);
                if (input != null) {
                    getEnv().handleMessage(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

        disconnect();
    }

    private void humanHandler() throws IOException {
        String initMessage = readMessage(in);
        System.out.println("Init message received");
    }

    public void stop() {
        running.set(false);
    }

    private String readMessage(DataInputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        baos.write(buffer, 0, in.read(buffer));

        return baos.toString(StandardCharsets.UTF_8);
    }

    public void disconnect() {
        try {
            stop();
            in.close();
            out.close();
            clientSocket.close();
            setEnv(null);
        } catch (IOException ignored) {
        }
    }
}