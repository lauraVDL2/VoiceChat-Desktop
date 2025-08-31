package com.voicechat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerMessageListener {
    private static ServerMessageListener instance;
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final BufferedReader serverIn;

    private ServerMessageListener() {
        this.serverIn = Listener.getServerIn(); // Assuming this is initialized elsewhere
        startListening();
    }

    public static synchronized ServerMessageListener getInstance() {
        if (instance == null) {
            instance = new ServerMessageListener();
        }
        return instance;
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            String line;
            try {
                while ((line = serverIn.readLine()) != null) {
                    messageQueue.offer(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /*public String pollMessage() {
        try {
            return messageQueue.take(); // blocks until a message is available
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }*/

    public String pollMessage() {
        return messageQueue.poll();
        /*try {
            return messageQueue.poll(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }*/
    }
}
