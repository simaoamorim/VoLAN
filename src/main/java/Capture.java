/*
 * Copyright (c) 2020 Simão Amorim
 *
 * This program is free software, distributed under the terms of the MIT license.
 * You can find the license file in /LICENSE.
 *
 */

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;

public class Capture extends Thread {
    private TargetDataLine input;
    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private AudioFormat audioFormat;
    private DatagramSocket socket;
    private InetAddress destination;

    public Capture(String remote_hostname) throws LineUnavailableException, SocketException, UnknownHostException {
        setDaemon(true);
        audioFormat = new AudioFormat(
                24000,
                16,
                1,
                true,
                false
        );
        DataLine.Info i = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (AudioSystem.isLineSupported(i)) {
            try {
                input = (TargetDataLine) AudioSystem.getLine(i);
                System.out.println("Line opened successfully");
            } catch (LineUnavailableException e) {
                System.err.println("Could not get line");
            }
        } else {
            System.err.println("Line is not supported");
        }
        System.out.println(input.getFormat().toString());
        input.open();
        System.out.println("Buffer size: " + input.getBufferSize());
        destination = InetAddress.getByName(remote_hostname);
        socket = new DatagramSocket();
    }

    public void printMixers() {
        System.out.println("System mixers: " + AudioSystem.getTargetLineInfo(input.getLineInfo()).length);
        System.out.println("Input line info:");
        System.out.println(input.getLineInfo());
    }

    public void printLineControls() {
        System.out.println("Available controls for " + input.getLineInfo().getLineClass().getName());
        for (Control ctrl: input.getControls()) {
            System.out.println(" " + ctrl);
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[input.getBufferSize()*32*16/1000];
        int avail;
        int read ;
        input.start();
        try {
            input.flush();
            while (!interrupted()) {
                sleep(32);
                avail = input.available();
                if (avail > 0) {
                    read = input.read(buf, 0, avail);
                    System.out.println("Read " + read + " bytes");
                    socket.send(new DatagramPacket(buf, read, destination, Playback.PORT));
                }
            }
        } catch (InterruptedException | IOException ignored) {}
        input.stop();
        socket.close();
    }

    public void cleanup() {
        input.stop();
        input.drain();
        input.close();
    }

}
