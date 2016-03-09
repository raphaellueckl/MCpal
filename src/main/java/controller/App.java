package controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

	// Remember to adapt the fromPath to point to your server-directory if set to true. 
    private static final boolean TEST_MODE = false;

    public static final String CONFIG_FILENAME = "MCpal.cfg";

    public static Path SOURCE_DIR_PATH;
    public static Path TARGET_DIR_PATH;
    public static String MAX_HEAP_SIZE;
    public static String JAR_NAME;

    public final String START_COMMAND;

    private static Thread consoleThread;
    public static volatile Process serverProcess;

    public static void main(String... args) throws IOException, URISyntaxException {
    	
    	Path fromPath;
        if (TEST_MODE) fromPath = Paths.get("C:/Users/Raphael/Desktop/mc");
        else fromPath = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

        if (!Files.exists(fromPath)) throw new IllegalArgumentException("Couldn't find the Minecraft server file." +
                "Make sure that it is within the same folder as this program.");

        final String toPath;
        final String maxHeapSize;
        final String jarName;
        if (args.length == 3) {
            toPath = args[0];
            maxHeapSize = args[1];
            jarName = args[2];
            writeConfigFile(fromPath, args);
        } else if (Files.exists(fromPath.resolve(CONFIG_FILENAME)) && !TEST_MODE) {
            final List<String> arguments = Files.readAllLines(Paths.get(fromPath + CONFIG_FILENAME));
            if (arguments.size() != 3) Files.delete(fromPath.resolve(CONFIG_FILENAME));
            Files.delete(Paths.get(fromPath + CONFIG_FILENAME));
            toPath = arguments.get(0);
            maxHeapSize = arguments.get(1);
            jarName = arguments.get(2);
        } else {
            throw new IllegalStateException("Invalid Input Parameters. Please start this App file like this:\n" +
                    "java -jar MCpal.jar PATH_TO_BACKUP_FOLDER MAX_RAM_YOU_WANNA_SPEND NAME_OF_MINECRAFT_SERVER_JAR\n" +
                    "Example: java -jar MCpal.jar \"C:\\Users\\Rudolf Ramses\\Minecraft\" 1024 minecraft_server.jar");
        }

        //TODO If !jar.exists throw exception
        
        checkEula(fromPath);

        new Thread(new ConsoleInput()).start();
        
        App MCpal = new App(fromPath, toPath, maxHeapSize, jarName);
        MCpal.start();
    }

    private static void checkEula(Path fromPath) {
        try {
            Path eulaPath = fromPath.resolve("eula.txt");
            File eulaFile = eulaPath.toFile();
            // TODO Irgendwie m�sste die Eula generiert werden, wenn sie nicht vorhanden ist.
            if (Files.exists(eulaPath)) {
                List<String> readAllLines = Files.readAllLines(eulaPath);
                final StringBuilder sb = new StringBuilder();
                readAllLines.forEach(line -> sb.append(line + System.getProperty("line.separator")));
                String eula = sb.toString();
                if (eula.contains("eula=false")) {
                    eula = eula.replace("eula=false", "eula=true");
                    FileWriter fw = new FileWriter(eulaFile);
                    fw.write(eula);
                    fw.flush();
                    fw.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeConfigFile(Path fromPath, String[] args) throws IOException {
        if (!Files.exists(fromPath.resolve(CONFIG_FILENAME)))
            Files.createFile(Paths.get(fromPath + "/" + CONFIG_FILENAME));
        FileWriter fw = new FileWriter(fromPath + "/" + CONFIG_FILENAME);
        for (String parameter : args) {
            fw.write(parameter + System.getProperty("line.separator"));
        }
        fw.flush();
        fw.close();
    }

    public App(Path fromPath, String targetDir, String maxHeapSize, String jarName) {
        MAX_HEAP_SIZE = maxHeapSize;
        JAR_NAME = jarName;
        SOURCE_DIR_PATH = fromPath;
        TARGET_DIR_PATH = Paths.get(targetDir);

        START_COMMAND = "java -jar -Xms" + MAX_HEAP_SIZE + "m" +
                " -Xmx" + MAX_HEAP_SIZE + "m " + JAR_NAME + " nogui";
    }

    private void start() throws IOException {
        serverProcess = startMinecraftServer();

        DailyBackupTask dailyTask = new DailyBackupTask(SOURCE_DIR_PATH, TARGET_DIR_PATH);
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        int oneDayInSeconds = 86400;
        int secondsUntil2Am = calculateTimeInSecondsTo2AM();
        service.scheduleWithFixedDelay(dailyTask, secondsUntil2Am, oneDayInSeconds, TimeUnit.SECONDS);
    }

    private int calculateTimeInSecondsTo2AM() {
        final LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsTo2AM = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 3, 0);
        if (secondsTo2AM.isBefore(now)) secondsTo2AM = secondsTo2AM.plusDays(1); 
        System.out.println("Time until Backup starts: " + ChronoUnit.HOURS.between(now, secondsTo2AM) + ":" + ChronoUnit.MINUTES.between(now, secondsTo2AM) % 60);
        return (int) ChronoUnit.SECONDS.between(now, secondsTo2AM);
    }

    public static Process startMinecraftServer() {
        Process process = null;
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "-Xms" + MAX_HEAP_SIZE + "m",
                    "-Xmx" + MAX_HEAP_SIZE + "m", JAR_NAME , "nogui");
            processBuilder.directory(SOURCE_DIR_PATH.toFile());
            process = processBuilder.start();

            if (consoleThread != null) consoleThread.interrupt();
            consoleThread = new Thread(new MinecraftConsole(process.getInputStream()));
            consoleThread.start();

        } catch (IOException ioe) { ioe.printStackTrace(); }
        return process;
    }

    public static void stopMinecraftServer(Process process, String reason) {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        w.println("say " + reason + " begins in 10...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 9...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 8...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 7...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 6...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 5...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 4...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 3...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 2...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say 1...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        w.println("say GAME OVER!!!!!!!!!!!!!...");
        w.flush();
        w.println("stop");
        w.flush();
        //w.close();
        try {
            System.out.println("CurrentTime: " + System.currentTimeMillis());
            process.waitFor(10, TimeUnit.SECONDS);
            System.out.println("After the wait: " + System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            process.destroy();
        }
    }

}