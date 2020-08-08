/*
 * Copyright (c) 2020 Simão Amorim
 *
 * This program is free software, distributed under the terms of the MIT license.
 * You can find the license file in /LICENSE.md.
 *
 */

import handlers.Capture;
import handlers.Playback;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.Semaphore;

public class Launcher extends Thread {
    private Capture capture;
    private Playback playback;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private Semaphore exitSemaphore = new Semaphore(0);
    private Thread shutdownHook = new Thread(this::exit);
    private ActionListener sysTrayListener = new Launcher.Listener();
    private Timer timer = new Timer();

    public Launcher(String[] args, AudioFormat audioFormat) throws SocketException, UnknownHostException, LineUnavailableException {
        capture = new Capture(args[0], audioFormat);
        playback = new Playback(audioFormat);
        timer.scheduleAtFixedRate(capture, 0, 100);
        playback.start();
    }

    private void exit() {
        capture.cancel();
        playback.interrupt();
        try {
            capture.cleanup();
            playback.join();
            systemTray.remove(trayIcon);
            exitSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    private class Listener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String command = e.getActionCommand();
            switch (command) {
                case "exit":
                    exitSemaphore.release();
                    break;
                default: break;
            }
        }
    }

    private class ConsoleHandler extends Thread {
         public void run() {
             Scanner scanner = new Scanner(System.in);
             String input;
             do {
                 input = scanner.nextLine();
             } while (! input.toLowerCase().equals("exit"));
             exitSemaphore.release();
         }
    }

    @Override
    public void run() {
        try {
            if (SystemTray.isSupported()) {
                systemTray = SystemTray.getSystemTray();
                trayIcon = new TrayIcon(
                        ImageIO.read(
                                Launcher.class.getResource(
                                        "headset_light.png"
                                )
                        ),
                        "VoLAN"
                );
                trayIcon.setImageAutoSize(true);
                final PopupMenu popupMenu = new PopupMenu();
                final MenuItem exitBtn = new MenuItem("Exit");
                exitBtn.addActionListener(sysTrayListener);
                exitBtn.setActionCommand("exit");
                popupMenu.add(exitBtn);
                trayIcon.setPopupMenu(popupMenu);
                systemTray.add(trayIcon);
            }
            new ConsoleHandler().start();
            exitSemaphore.acquire();
            shutdownHook.start();
            shutdownHook.join();
        } catch (IOException | AWTException | InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Launcher <Remote IP Address>");
            return;
        }
        try {
            AudioFormat audioFormat = new AudioFormat(
                24000,
                16,
                1,
                true,
                false
            );
            Launcher launcher = new Launcher(args, audioFormat);
            launcher.start();
            launcher.join();
            System.exit(0);
        } catch (LineUnavailableException | SocketException | UnknownHostException | InterruptedException e) {
            e.printStackTrace(System.err);
        }
    }
}
