import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import javax.sound.sampled.LineUnavailableException;

public class CaptureTest {
    private Capture capture;

    @BeforeTest
    public void init() throws LineUnavailableException {
        capture = new Capture();
    }

    @Test
    public void testPrintMixers() {
        capture.printMixers();
    }

    @Ignore
    @Test
    public void testLoopback() throws InterruptedException {
        capture.start();
        Thread.sleep(5000);
        capture.interrupt();
    }

    @AfterTest
    public void end() throws InterruptedException {
        capture.interrupt();
        capture.cleanup();
        capture.join();
    }

    @Test
    public void testPrintLineControls() {
        capture.printLineControls();
    }
}