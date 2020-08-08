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
    private final DatagramSocket socket;
    public static final int PORT = 56321;
    FloatControl gain;

    public Playback(AudioFormat audioFormat) throws SocketException {
        DataLine.Info j = new DataLine.Info(SourceDataLine.class, audioFormat);
        if (AudioSystem.isLineSupported(j)) {
            try {
                output = (SourceDataLine) AudioSystem.getLine(j);
                output.open();
                if (output.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    gain = (FloatControl)
                            output.getControl(FloatControl.Type.MASTER_GAIN);
                    gain.setValue((float) 6.0);
                }
                else
                    gain = null;
                System.out.println("Line opened successfully");
            } catch (LineUnavailableException e) {
                System.err.println("Could not get line");
            }
        } else {
            System.err.println("Line is not supported");
        }
        System.out.println(output.getFormat().toString());
        System.out.println("Buffer size: " + output.getBufferSize());
        socket = new DatagramSocket(PORT);
    }

    public void printLineControls() {
        System.out.println("Available controls for " + output.getLineInfo().getLineClass().getName());
        for (Control ctrl: output.getControls()) {
            System.out.println(" " + ctrl);
        }
        System.out.println(String.format("Gain is supported: %s", output.isControlSupported(FloatControl.Type.MASTER_GAIN)));
        System.out.println(String.format("Level: %.1f", output.getLevel()));
    }

    public void run() {
        byte[] buf = new byte[output.getBufferSize()*output.getFormat().getFrameSize()];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        output.start();
        try {
            socket.setSoTimeout(100);
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
                int buffer_avail = packet.getLength();
                byte[] buffer = packet.getData();

                output.write(buffer, 0, buffer_avail);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        output.stop();
        output.close();
        socket.close();
    }

}
