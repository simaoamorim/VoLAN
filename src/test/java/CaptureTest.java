/*
 * Copyright (c) 2020 Simão Amorim
 *
 * This program is free software, distributed under the terms of the MIT license.
 * You can find the license file in /LICENSE.
 *
 */

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.sound.sampled.LineUnavailableException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class CaptureTest {
    private Capture capture;
    private Playback playback;

    @BeforeTest
    public void init() throws LineUnavailableException, SocketException, UnknownHostException {
        capture = new Capture("localhost");
        playback = new Playback();
    }

    @Test
    public void testPrintMixers() {
        capture.printMixers();
    }

    @Test
    public void testLoopback() throws InterruptedException {
        capture.start();
        playback.start();
        Thread.sleep(5000);
        capture.interrupt();
        playback.interrupt();
    }

    @AfterTest
    public void end() throws InterruptedException {
        capture.interrupt();
        capture.cleanup();
        capture.join();
        playback.interrupt();
        playback.join();
    }

    @Test
    public void testPrintLineControls() {
        capture.printLineControls();
    }
}