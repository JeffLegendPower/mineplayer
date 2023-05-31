package io.github.jefflegendpower.mineplayerclient.human;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.io.PrintWriter;

public class HumanEnvironment {

    private PrintWriter out;
    private OutputStream outputStream;
    private Runnable disconnectTCP;

    public HumanEnvironment(PrintWriter out, OutputStream outputStream, Runnable disconnectTCP) {
        this.out = out;
        this.outputStream = outputStream;
        this.disconnectTCP = disconnectTCP;
    }

    public void start(String startMessage) {
        JsonObject start = new Gson().fromJson(startMessage, JsonObject.class);


    }
}
