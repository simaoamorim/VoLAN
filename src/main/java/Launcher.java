/*
 * Copyright (c) 2020 Simão Amorim
 *
 * This program is free software, distributed under the terms of the MIT license.
 * You can find the license file in /LICENSE.
 *
 */

import handlers.Capture;
import handlers.Playback;

import javax.sound.sampled.LineUnavailableException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Launcher extends Thread {
    private Capture capture;
    private Playback playback;

    public Launcher(String[] args) throws SocketException, UnknownHostException, LineUnavailableException {
        Runtime.getRuntime().addShutdownHook(new Launcher.Shutdown());
        capture = new Capture(args[0]);
        playback = new Playback();
        capture.start();
        playback.start();
    }

    private class Shutdown extends Thread {
        public void run() {
            capture.interrupt();
            playback.interrupt();
            capture.cleanup();
            try {
                capture.join();
                playback.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Launcher <Remote IP Address>");
            return;
        }
        try {
            new Launcher(args);
            while (! interrupted()) Thread.sleep(10000);
        } catch (LineUnavailableException | SocketException | UnknownHostException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
