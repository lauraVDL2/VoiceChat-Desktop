package com.voicechat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerReader {
    private final BlockingQueue<ImageView> avatarQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ServerResponse> serverResponses = new LinkedBlockingQueue<>();
    private final DataInputStream dataInputStream;
    private volatile boolean running = true; // Flag to control thread execution
    private Thread readerThread; // Reference to the thread for stopping

    public ServerReader(DataInputStream dataInputStream) {
        this.dataInputStream = dataInputStream;
    }

    public void startReading() {
        readerThread = new Thread(() -> {
            try {
                while (running) {
                    //Crucial:  Check for null dataInputStream.  If the Listener closes the connection, this will avoid a NullPointerException.
                    if (dataInputStream == null) {
                        System.err.println("Data Input Stream is null. Exiting reader thread.");
                        return; // Exit the thread if the stream is null
                    }
                    Object object = readTargetResponse();
                    if (object instanceof ImageView) {
                        ImageView imageView = (ImageView) object;
                        if (imageView != null) {
                            avatarQueue.put(imageView); // Put the VBox into the queue
                        } else {
                            //Handle the case where no avatar is read (e.g., end of stream or error)
                            System.err.println("No avatar to read, or an error occurred.");
                        }
                    }
                    else if (object instanceof ServerResponse) {
                        ServerResponse serverResponse = (ServerResponse) object;
                        if (serverResponse != null) {
                            serverResponses.put(serverResponse);
                        } else {
                            //Handle the case where no avatar is read (e.g., end of stream or error)
                            System.err.println("No response to read, or an error occurred.");
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.err.println("Reader thread interrupted: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error reading avatar data: " + e.getMessage());
            }
        });
        readerThread.start();
    }

    public ImageView getAvatar() throws InterruptedException {
        return avatarQueue.take(); // Blocks until an avatar is available
    }

    public ServerResponse getServerResponse() throws InterruptedException {
        return serverResponses.take();
    }

    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt(); // Interrupt to unblock any blocking calls
        }
    }

    private Object readTargetResponse() throws IOException {

        if (StringUtils.equals(dataInputStream.readUTF(), "IMAGE_RESPONSE")) {
            ImageView avatar = new ImageView();

            int size = dataInputStream.readInt();

            if (size > 0) {
                byte[] imageBytes = new byte[size];
                dataInputStream.readFully(imageBytes);
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(imageBytes, ServerResponse.class);
                byte[] avatarBase64 = response.getBinaryPayload();
                //Crucial: Handling potential exceptions.
                try {
                    Image image = new Image(new ByteArrayInputStream(avatarBase64));
                    avatar.setImage(image);
                    System.out.println("Avatar read successfully.");
                    return avatar;
                } catch (Exception e) {
                    System.err.println("Error creating Image from bytes: " + e.getMessage());
                    return null; // Indicate failure to create image
                }
            } else {
                System.out.println("No avatar data received.");
                return null;
            }
        } else {
            int size = dataInputStream.readInt();

            if (size > 0) {
                byte[] bytes = new byte[size];
                dataInputStream.readFully(bytes);
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(bytes, ServerResponse.class);
                System.out.println(response.getServerResponseMessage());
                //new String(response.getBinaryPayload(), StandardCharsets.UTF_8);
                return response;
            }
        }
        return null;
    }
}
