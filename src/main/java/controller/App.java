package controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static model.Variables.MCPAL_TAG;

/**
 * Additional ideas:
 * - Count the players on the server. If 0, it's unnecessary to wait 10 seconds before stopping it (for instance)
 * - Custom parameters with default values
 * - Fix the console input
 */
public class App {


    public static volatile boolean isServerRunning = false;

    public final String START_COMMAND;

    public static Path SOURCE_DIR_PATH;
    public static Path TARGET_DIR_PATH;
    public static String MAX_HEAP_SIZE;
    public static String JAR_NAME;
    public static Path WORLD_NAME;

    private static List<String> ADDITIONAL_COMMANDS_AFTER_BACKUP;
    private static Thread consoleThread;
    private static Thread consoleWriterThread;
    public static volatile Process serverProcess;

    public static void main(String... args) throws IOException, URISyntaxException {

    }








    public App(Path fromPath, String targetDir, String maxHeapSize, String jarName, Path worldName, List<String> additionalThingsToRun) {
        MAX_HEAP_SIZE = maxHeapSize;
        JAR_NAME = jarName;
        SOURCE_DIR_PATH = fromPath;
        TARGET_DIR_PATH = Paths.get(targetDir);
        WORLD_NAME = worldName;
        ADDITIONAL_COMMANDS_AFTER_BACKUP = additionalThingsToRun;

        START_COMMAND = "java -jar -Xms" + MAX_HEAP_SIZE + "m" +
                " -Xmx" + MAX_HEAP_SIZE + "m " + JAR_NAME + " nogui";

        if (WORLD_NAME != null) {
            ADDITIONAL_COMMANDS_AFTER_BACKUP.replaceAll(command -> command.replace("{1}", WORLD_NAME.toString()));
        }

        System.out.println("***********************");
        System.out.println("Path of the server:   " + SOURCE_DIR_PATH);
        System.out.println("Path for the backups: " + TARGET_DIR_PATH);
        System.out.println("Server-Jar name:      " + JAR_NAME);
        System.out.println("World-Name:           " + WORLD_NAME);
        System.out.println("Additional commands:  ");
        ADDITIONAL_COMMANDS_AFTER_BACKUP.forEach(c -> System.out.println("                      " + c));
        System.out.println("***********************");
    }

    public void start() throws IOException {
        startMinecraftServer();

        consoleWriterThread = new Thread(new ConsoleInput());
        consoleWriterThread.start();

        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        int oneDayInSeconds = 86400;
        int secondsUntil4Am = calculateTimeInSecondsTo4AM();
        service.scheduleWithFixedDelay(App::backupServer, secondsUntil4Am, oneDayInSeconds, TimeUnit.SECONDS);
    }

    private int calculateTimeInSecondsTo4AM() {
        final LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsTo4AM = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 4, 0);
        if (secondsTo4AM.isBefore(now)) secondsTo4AM = secondsTo4AM.plusDays(1);
        System.out.println(MCPAL_TAG + "Time until Backup starts: " + twoDigitFormat(String.valueOf(ChronoUnit.HOURS.between(now, secondsTo4AM))) +
                ":" + twoDigitFormat(String.valueOf(ChronoUnit.MINUTES.between(now, secondsTo4AM) % 60)) + " h");
        return (int) ChronoUnit.SECONDS.between(now, secondsTo4AM);
    }

    private static String twoDigitFormat(String term) {
        return term.length() < 2 ? "0" + term : term;
    }

    public static void startMinecraftServer() {

        if (isServerRunning) {
            System.out.println(MCPAL_TAG + "Server is already running, please stop it first using the \"stop\"-command");
        } else {

            Process process = null;
            try {
                final ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "-Xms" + MAX_HEAP_SIZE + "m",
                        "-Xmx" + MAX_HEAP_SIZE + "m", JAR_NAME, "nogui");
                processBuilder.directory(SOURCE_DIR_PATH.toFile());
                process = processBuilder.start();

                if (consoleThread != null) consoleThread.interrupt();
                consoleThread = new Thread(new MinecraftConsole(process.getInputStream()));
                consoleThread.start();

                isServerRunning = true;

            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                serverProcess = process;
            }

        }
    }

    public static void stopMinecraftServer(Process process, String reason, boolean runWithCountdown) {
        if (isServerRunning) {
            PrintWriter consoleWriter = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
            if (runWithCountdown) printCountDown(consoleWriter, reason);

            consoleWriter.println("stop");
            consoleWriter.flush();
            //w.close();
            try {
                process.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                process.destroy();
                isServerRunning = false;
            }
        } else {
            System.out.println(MCPAL_TAG + "Nothing to stop. Server is not active at the moment.");
        }
    }

    private static void printCountDown(PrintWriter w, String reason) {
        w.println("say " + reason + " begins in 10...");
        w.flush();
        try {Thread.sleep(1000);} catch (InterruptedException e) {}
        for (int i=9; i>0; --i) {
            w.println("say " + i + "...");
            w.flush();
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
        }
        w.println("say GAME OVER!!!!!!!!!!!!!");
        w.flush();
        try {Thread.sleep(200);} catch (InterruptedException e) {}
    }

    public static synchronized void backupServer() {
        try {
            App.stopMinecraftServer(App.serverProcess, "[Server backup]", true);

            Thread.sleep(2000);

            Files.createDirectories(SOURCE_DIR_PATH);
            Backup backupHandler = new Backup(SOURCE_DIR_PATH, TARGET_DIR_PATH);
            FutureTask<String> futureTask = new FutureTask<>(backupHandler);
            new Thread(futureTask).start();
            String backupStorePath = futureTask.get();

            List<String> commandListClone = new ArrayList<>(ADDITIONAL_COMMANDS_AFTER_BACKUP);
            commandListClone.replaceAll(command -> command.replace("{2}", backupStorePath));
            for (String command : commandListClone) {
                final List<String> parametersOfCommand = Arrays.asList(command.split(" "));
                final ProcessBuilder processBuilder = new ProcessBuilder(parametersOfCommand);
                new Thread(() -> {
                    try {
                        processBuilder.start();
                        System.out.println(MCPAL_TAG + "Process successful: " + parametersOfCommand.get(0));
                    } catch (IOException e) {
                        System.out.println(MCPAL_TAG + "The following process failed: " + command);
                        e.printStackTrace();
                    }
                }).start();
            }

            Thread.sleep(2000);
            startMinecraftServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}