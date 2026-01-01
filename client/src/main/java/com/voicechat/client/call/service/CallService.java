package com.voicechat.client.call.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicechat.client.Listener;
import com.voicechat.client.common.UserSession;
import de.maxhenkel.opus4j.OpusDecoder;
import de.maxhenkel.opus4j.OpusEncoder;
import de.maxhenkel.opus4j.UnknownPlatformException;
import org.shared.JsonMapper;
import org.shared.Message;
import org.shared.MessageType;
import org.shared.ServerResponse;
import org.shared.pojo.Voice;
import org.shared.pojo.VoiceChatEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CallService {

    public ServerResponse connect(VoiceChatEvent voiceChatEvent) throws JsonProcessingException, InterruptedException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        String json = objectMapper.writeValueAsString(voiceChatEvent);
        Message message = new Message(MessageType.MEETING_CONNECT, json);
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();

        synchronized (serverOut) {
            System.out.println("connect["+objectMapper.writeValueAsString(message)+"]");
            serverOut.println(objectMapper.writeValueAsString(message));
            serverOut.flush();
        }

        return Listener.getServerReader().getServerResponseByCorrelationId(correlationId);
    }

    // Inside your method
    public void talk(Voice voice) throws IOException, InterruptedException, UnknownPlatformException {
        /*OpusEncoder encoder = new OpusEncoder(16000, 1, OpusEncoder.Application.VOIP);

        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        byte[] pcmData = voice.getAudio();
        byte[] opusData = encodeOpus(pcmData, encoder);

        int chunkSize = 1024*4; // 8KB chunks for transmission
        int totalSize = opusData.length;
        int offset = 0;*/
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Message message = new Message();
        message.setMessageType(MessageType.IS_TALKING);
        //byte[] bytes = compress(chunk);
        message.setBinaryPayload(objectMapper.writeValueAsBytes(voice));
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);
        PrintWriter serverOut = Listener.getServerOut();
        serverOut.println(objectMapper.writeValueAsString(message));

        /*while (offset < totalSize) {
            int length = Math.min(chunkSize, totalSize - offset);
            byte[] chunk = Arrays.copyOfRange(opusData, offset, offset + length);

            Message message = new Message();
            message.setMessageType(MessageType.IS_TALKING);
            //byte[] bytes = compress(chunk);
            voice.setAudio(chunk);
            message.setBinaryPayload(objectMapper.writeValueAsBytes(voice));
            String correlationId = UUID.randomUUID().toString();
            message.setCorrelationId(correlationId);
            PrintWriter serverOut = Listener.getServerOut();
            serverOut.println(objectMapper.writeValueAsString(message));
            //message.setBinaryPayload(Base64.getEncoder().encode(chunk));
            *//*System.out.println("Audio bytes talk: " +
                    Arrays.toString(Arrays.copyOfRange(opusData, offset, offset + length)));*//*

            *//*OutputStream out = Listener.getSocket().getOutputStream();
            /*out.write(objectMapper.writeValueAsBytes(message));
            out.flush();*//*
            offset += length;

            Thread.sleep(10);
        }*/
       // encoder.resetState();
        //encoder.close();
    }



    public byte[] encodeOpus(byte[] pcmData, OpusEncoder encoder) throws IOException {
        int frameSize = 160; // 20ms frame at 16kHz
        int samples = pcmData.length / 2; // total samples
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ByteBuffer byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);

        for (int offset = 0; offset + frameSize <= samples; offset += frameSize) {
            short[] pcmSamples = new short[frameSize];
            for (int i = 0; i < frameSize; i++) {
                pcmSamples[i] = byteBuffer.getShort((offset + i) * 2);
            }
            byte[] encoded = encoder.encode(pcmSamples);
            outputStream.write(encoded);
        }

        return outputStream.toByteArray();
    }

    public byte[] decodeOpus(byte[] encodedData, OpusDecoder decoder) throws IOException {
        int frameSizeBytes = 160;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int offset = 0;
        while (offset < encodedData.length) {
            // Read one frame at a time based on known frame size
            if (offset + frameSizeBytes > encodedData.length) {
                break; // Incomplete frame at end
            }
            byte[] frame = Arrays.copyOfRange(encodedData, offset, offset + frameSizeBytes);
            offset += frameSizeBytes;

            short[] decodedPcm = decoder.decode(frame, false);
            // Convert short[] to byte[] (little endian)
            ByteBuffer pcmBuffer = ByteBuffer.allocate(decodedPcm.length * 2).order(ByteOrder.LITTLE_ENDIAN);
            pcmBuffer.asShortBuffer().put(decodedPcm);
            outputStream.write(pcmBuffer.array());
        }

        return outputStream.toByteArray();
    }

    /*public byte[] decodeOpus(byte[] data, OpusDecoder decoder) throws IOException, UnknownPlatformException {
        decoder.setFrameSize(960);
        short[] decoded = decoder.decode(data);
        byte[] byteBuffer = new byte[decoded.length * 2]; // 2 bytes per short
        for (int i = 0; i < decoded.length; i++) {
            // Little-endian byte order
            byteBuffer[2 * i] = (byte) (decoded[i] & 0xff);
            byteBuffer[2 * i + 1] = (byte) ((decoded[i] >> 8) & 0xff);
        }
        return byteBuffer;
    }*/

    // Compress byte array
    public byte[] compress(byte[] data) throws IOException {
        // Create a Deflater with best compression level
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish(); // Indicate we're ready to compress

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];

            // Compress data into buffer
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }
            deflater.end(); // Free resources
            return baos.toByteArray(); // Return compressed data
        }
    }

    // Decompress byte array
    public byte[] decompress(byte[] compressedData) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];

            // Decompress data into buffer
            while (!inflater.finished()) {
                int count;
                try {
                    count = inflater.inflate(buffer);
                } catch (Exception e) {
                    throw new IOException("Failed to inflate data", e);
                }
                if (count == 0 && inflater.needsInput()) {
                    break; // Prevent infinite loop if data is corrupted
                }
                baos.write(buffer, 0, count);
            }
            inflater.end(); // Free resources
            byte[] decompressed = baos.toByteArray();
            if (decompressed.length % 2 != 0) {
                // Option 1: Trim the last byte
                decompressed = Arrays.copyOf(decompressed, decompressed.length - 1);
                // Option 2: Handle the partial frame as needed
            }
            return decompressed; // Return decompressed data
        }
    }
}
