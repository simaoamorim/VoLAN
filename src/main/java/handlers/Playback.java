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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class Playback extends Thread {
    private SourceDataLine output;
    @SuppressWarnings({"CanBeFinal", "FieldCanBeLocal"})
    private AudioFormat audioFormat;
    private DatagramSocket socket;
    private final Object bufferLock = new Object();
    private byte[] buffer;
    private int buffer_avail;
    public static final int PORT = 56321;

    private class Player extends Thread {
        Player() {
            this.setDaemon(true);
            this.start();
        }
        @Override
        public void run() {
            try {
                while (! interrupted()) {
                    sleep(10);
                    if (buffer_avail > 0) {
                        synchronized (bufferLock) {
                            output.write(buffer, 0, buffer_avail);
                            buffer_avail = 0;
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Playback() throws LineUnavailableException, SocketException {
        setDaemon(true);
        audioFormat = new AudioFormat(
                24000,
                16,
                1,
                true,
                false
        );
        DataLine.Info j = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (AudioSystem.isLineSupported(j)) {
            try {
                output = (SourceDataLine) AudioSystem.getLine(j);
                System.out.println("Line opened successfully");
            } catch (LineUnavailableException e) {
                System.err.println("Could not get line");
            }
        } else {
            System.err.println("Line is not supported");
        }
        System.out.println(output.getFormat().toString());
        output.open();
        System.out.println("Buffer size: " + output.getBufferSize());
        socket = new DatagramSocket(PORT);
        new Playback.Player();
    }

    public void run() {
        byte[] buf = new byte[output.getBufferSize()*32*16/1000];
        int avail;
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        output.start();
        try {
            socket.setSoTimeout(1);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }
        try {
            while (! isInterrupted()) {
                try {
                    socket.receive(packet);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                synchronized (bufferLock) {
                    buffer_avail = packet.getLength();
                    buffer = packet.getData();
                }
//                output.write(buf,0,avail);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.stop();
        socket.close();
    }

}
