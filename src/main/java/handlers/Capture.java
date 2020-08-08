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
    private TargetDataLine dataLine;
    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private AudioFormat audioFormat;
    private DatagramSocket socket;
    private InetAddress destination;
    private int avail, read;

    public Capture(String remote_hostname, AudioFormat audioFormat) throws LineUnavailableException, SocketException, UnknownHostException {
        setDaemon(true);
        this.audioFormat = audioFormat;
        DataLine.Info i = new DataLine.Info(TargetDataLine.class, this.audioFormat);
        if (AudioSystem.isLineSupported(i)) {
            try {
                dataLine = (TargetDataLine) AudioSystem.getLine(i);
                System.out.println("Line opened successfully");
            } catch (LineUnavailableException e) {
                System.err.println("Could not get line");
            }
        } else {
            System.err.println("Line is not supported");
        }
        System.out.println(dataLine.getFormat().toString());
        dataLine.open();
        System.out.println("Buffer size: " + dataLine.getBufferSize());
        destination = InetAddress.getByName(remote_hostname);
        socket = new DatagramSocket();
        dataLine.start();
    }

    public void printMixers() {
        System.out.println("System mixers: " + AudioSystem.getTargetLineInfo(dataLine.getLineInfo()).length);
        System.out.println("Input line info:");
        System.out.println(dataLine.getLineInfo());
    }

    public void printLineControls() {
        System.out.println("Available controls for " + dataLine.getLineInfo().getLineClass().getName());
        for (Control ctrl: dataLine.getControls()) {
            System.out.println(" " + ctrl);
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[dataLine.getBufferSize()*audioFormat.getFrameSize()];
        try {
            dataLine.flush();
            while (!interrupted()) {
                sleep(10);
                avail = dataLine.available();
                Arrays.fill(buf, (byte) 0);
                read = dataLine.read(buf, 0, Math.min(avail, buf.length));
                if (avail > buf.length) { dataLine.flush(); }
                socket.send(new DatagramPacket(buf, read, destination, Playback.PORT));
            }
        } catch (InterruptedException ignored) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        cleanup();
    }

    public void cleanup() {
        dataLine.stop();
        dataLine.flush();
        dataLine.close();
        socket.close();
    }

}
