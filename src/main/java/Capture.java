import javax.sound.sampled.*;

public class Capture extends Thread {
    protected TargetDataLine input;
    protected SourceDataLine output;
    @SuppressWarnings("CanBeFinal")
    private AudioFormat audioFormat;

    public Capture() throws LineUnavailableException {
        setDaemon(true);
        audioFormat = new AudioFormat(
                20000,
                16,
                1,
                true,
                false
        );
        DataLine.Info i = new DataLine.Info(TargetDataLine.class, audioFormat);
        DataLine.Info j = new DataLine.Info(SourceDataLine.class, audioFormat);
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
        System.out.println(input.getFormat().toString());
        input.open();
        System.out.println(output.getFormat().toString());
        output.open();
        System.out.println("Buffer size: " + input.getBufferSize());
    }

    public void printMixers() {
        System.out.println("System mixers: " + AudioSystem.getTargetLineInfo(input.getLineInfo()).length);
        System.out.println("Input line info:");
        System.out.println(input.getLineInfo());
    }

    public void printLineControls() {
        for (Line line: new Line[]{input,output}) {printLineControls(line);}
    }

    public void printLineControls(Line line) {
        System.out.println("Available controls for " + line.getLineInfo().getLineClass().getName());
        for (Control ctrl: line.getControls()) {
            System.out.println(" " + ctrl);
        }
    }

    @Override
    public void run() {
        byte[] buf = new byte[input.getBufferSize()];
        int avail;
        int read ;
        int wrote;
        input.start();
        output.start();
        try {
            input.flush();
            while (!interrupted()) {
                sleep(10);
                avail = input.available();
                if (avail > 0) {
                    read = input.read(buf, 0, avail);
                    System.out.println("Looping " + read + " bytes");
                    wrote = output.write(buf, 0, read);
                    System.out.println("Outputted " + wrote + " bytes");
                }
            }
        } catch (InterruptedException ignored) {}
        input.stop();
        output.stop();
    }

    public void cleanup() {
        input.stop();
        output.stop();
        input.drain();
        input.close();
        output.flush();
        output.drain();
        output.close();
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }


}
