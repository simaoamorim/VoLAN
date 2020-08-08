/*
 * Copyright (c) 2020 Simão Amorim
 *
 * This program is free software, distributed under the terms of the MIT license.
 * You can find the license file in /LICENSE.md.
 *
 */

package handlers;
import javax.sound.sampled.*;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Capture extends Thread {
    private TargetDataLine input;
    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private AudioFormat audioFormat;
    private DatagramSocket socket;
    private InetAddress destination;

    public Capture(String remote_hostname, AudioFormat audioFormat) throws LineUnavailableException, SocketException, UnknownHostException {
        setDaemon(true);
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
        byte[] buf = new byte[input.getBufferSize()*input.getFormat().getFrameSize()];
        int avail;
        int read;
        input.start();
        try {
            input.flush();
            while (!interrupted()) {
                sleep(10);
                avail = input.available();
                Arrays.fill(buf, (byte) 0);
                read = input.read(buf, 0, Math.min(avail, buf.length));
                if (avail > buf.length) { input.flush(); }
                socket.send(new DatagramPacket(buf, read, destination, Playback.PORT));
            }
        } catch (InterruptedException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        input.stop();
        input.flush();
        input.close();
        socket.close();
    }

    public void cleanup() {
        input.stop();
        input.drain();
        input.close();
    }

}
