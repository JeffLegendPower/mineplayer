package io.github.jefflegendpower.mineplayerclient.env;

import io.github.jefflegendpower.mineplayerclient.client.MineplayerClient;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.jefflegendpower.mineplayerclient.client.MineplayerClient.*;

public class EnvTCPServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private DataInputStream in;

    private AtomicBoolean running = new AtomicBoolean(true);

    public void start(int port, boolean test) throws IOException {
        serverSocket = new ServerSocket(port);

        while (running.get()) {
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new DataInputStream(clientSocket.getInputStream());
            System.out.println("Client connected");

            while (true) {
                if (clientSocket.getInputStream().available() <= 0) {
                    System.out.println("Not available");
                } else {
                    System.out.println("available" + clientSocket.getInputStream().available());
                    break;
                }
            }
            String initMessage = readMessage(in);
            System.out.println("Init message received");

            // This will block until fully initialized
            if (!test) {
                setEnv(new Environment(out, clientSocket.getOutputStream(), this::disconnect));

                boolean initialized = getEnv().handleMessage(initMessage);
                if (!initialized) {
                    System.out.println("Failed to initialize environment");
                    disconnect();
                    continue;
                }
                while (running.get() && clientSocket.isConnected()) {
                    try {
                        String input = readMessage(in);
                        if (input != null) {
                            getEnv().handleMessage(input);
                        }
                    } catch (IOException e) {
                        break;
                    }
                }

                disconnect();
            } else {
                System.out.println("Testing...");
                System.out.println(initMessage);
                System.out.println("Disconnecting...");
                disconnect();
                break;
            }
        }
    }

    public void stop() {
        running.set(false);
    }

    private String readMessage(DataInputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        baos.write(buffer, 0 , in.read(buffer));

        return baos.toString(StandardCharsets.UTF_8);
    }

    public void disconnect() {
        try {
            in.close();
            out.close();
            clientSocket.close();
//            serverSocket.close(); TODO will remove this because I think it's causing the issue of unable to reconnect
            setEnv(null);
        } catch (IOException ignored) {
        }
    }
}
