package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MinecraftConsole implements Runnable {

    private final BufferedReader consoleInputReader;

    public MinecraftConsole(InputStream consoleInputStream) {
        consoleInputReader = new BufferedReader(new InputStreamReader(consoleInputStream));
    }

    @Override
    public void run() {
        String line;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if ((line = consoleInputReader.readLine()) == null) break;
                System.out.println(line);
            }
        } catch (IOException ioe) { ioe.printStackTrace(); }
    }
}
