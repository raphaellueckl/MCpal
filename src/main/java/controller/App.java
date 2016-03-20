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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * Additional ideas:
 * - Count the players on the server. If 0, it's unecessary to wait 10 seconds before stopping it (for instance)
 */
public class App {

    public static final String CONFIG_FILENAME = "MCpal.cfg";
    public static final String MCPAL_TAG = "#MCpal: ";
    public static Path SOURCE_DIR_PATH;

    public static Path TARGET_DIR_PATH;
    public static String MAX_HEAP_SIZE;
    public static String JAR_NAME;
    public static Path WORLD_NAME;

    private static List<String> ADDITIONAL_COMMANDS_AFTER_BACKUP;

    public final String START_COMMAND;
    private static Thread consoleThread;
    private static Thread consoleWriterThread;
    public static volatile Process serverProcess;

    public static void main(String... args) throws IOException, URISyntaxException {

        Path fromPath = Paths.get(App.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();

        final String toPath;
        final String maxHeapSize;
        final String jarName;
        final Path worldName;
        final List<String> additionalPluginsToRunAfterBackup = new ArrayList<>();

        if (args.length == 0 && Files.exists(fromPath.resolve(CONFIG_FILENAME))) {
            final List<String> arguments = Files.readAllLines(fromPath.resolve(CONFIG_FILENAME));
            if (arguments.size() < 3) {
                Files.delete(fromPath.resolve(CONFIG_FILENAME));
                throwInvalidStartParametersException();
            }
            Files.delete(Paths.get(fromPath + CONFIG_FILENAME));
            toPath = arguments.get(0);
            maxHeapSize = arguments.get(1);
            jarName = arguments.get(2);
        } else if (args.length != 0) {
            toPath = args[0];
            maxHeapSize = args[1];
            jarName = args[2];
            for (int i=3; i<args.length; ++i) {
                additionalPluginsToRunAfterBackup.add(args[i]);
            }
            writeConfigFile(fromPath, args);
        } else {
            throwInvalidStartParametersException();
            //TODO This is a workaround to keep the variables final. Doesn't work without since exception was outsourced.
            toPath = null;
            maxHeapSize = null;
            jarName = null;
        }


        if (!Files.exists(fromPath)) throw new IllegalArgumentException("Couldn't find the Minecraft server file." +
                "Make sure that put this program into your Minecraft server directory.");

        worldName = searchWorldName(fromPath);

        checkEula(fromPath);

        consoleWriterThread = new Thread(ConsoleInput::new);
        consoleWriterThread.start();
        
        App MCpal = new App(fromPath, toPath, maxHeapSize, jarName, worldName, additionalPluginsToRunAfterBackup);
        MCpal.start();
    }

    private static Path searchWorldName(Path fromPath) {
        try {
            DirectoryStream<Path> dirStream = Files.newDirectoryStream(fromPath);
            for (Path currentElement : dirStream) {
                if (Files.isDirectory(currentElement) && couldThisDirectoryPossibleBeTheWorldFolder(currentElement)) {
                    return currentElement.getFileName();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean couldThisDirectoryPossibleBeTheWorldFolder(Path currentElement) {
        Path levelDotDatFile = currentElement.resolve("level.dat");
        return Files.exists(levelDotDatFile);

    }

    private static void throwInvalidStartParametersException() {
        throw new IllegalStateException("Invalid Input Parameters. Please start this App file like this:\n" +
                "java -jar MCpal.jar PATH_TO_BACKUP_FOLDER MAX_RAM_YOU_WANNA_SPEND NAME_OF_MINECRAFT_SERVER_JAR\n" +
                "Example: java -jar MCpal.jar \"C:\\Users\\Rudolf Ramses\\Minecraft\" 1024 minecraft_server.jar");
    }

    private static void checkEula(Path fromPath) {
        try {
            Path eulaPath = fromPath.resolve("eula.txt");
            File eulaFile = eulaPath.toFile();
            // TODO Irgendwie müsste die Eula generiert werden, wenn sie nicht vorhanden ist.
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
            } else throw new IllegalStateException(MCPAL_TAG + "Please restart this program. The EULA wasn't " +
                    "available at startup, but now it is fine. :)");
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

    public App(Path fromPath, String targetDir, String maxHeapSize, String jarName, Path worldName, List<String> additionalThingsToRun) {
        MAX_HEAP_SIZE = maxHeapSize;
        JAR_NAME = jarName;
        SOURCE_DIR_PATH = fromPath;
        TARGET_DIR_PATH = Paths.get(targetDir);
        WORLD_NAME = worldName;
        ADDITIONAL_COMMANDS_AFTER_BACKUP = additionalThingsToRun;

        START_COMMAND = "java -jar -Xms" + MAX_HEAP_SIZE + "m" +
                " -Xmx" + MAX_HEAP_SIZE + "m " + JAR_NAME + " nogui";

        ADDITIONAL_COMMANDS_AFTER_BACKUP.replaceAll(command -> command.replace("{1}", WORLD_NAME.toString()));

        System.out.println("***********************");
        System.out.println("Path of the server:   " + SOURCE_DIR_PATH);
        System.out.println("Path for the backups: " + TARGET_DIR_PATH);
        System.out.println("Server-Jar name:      " + JAR_NAME);
        System.out.println("World-Name:           " + WORLD_NAME);
        System.out.println("                      " + WORLD_NAME);
        System.out.print  ("Additional commands:  ");
        ADDITIONAL_COMMANDS_AFTER_BACKUP.forEach(c -> System.out.println("                      " + c));
        System.out.println("***********************");
    }

    private void start() throws IOException {
        serverProcess = startMinecraftServer();

        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        int oneDayInSeconds = 86400;
        int secondsUntil4Am = calculateTimeInSecondsTo4AM();
        service.scheduleWithFixedDelay(App::backupServer, secondsUntil4Am, oneDayInSeconds, TimeUnit.SECONDS);
    }

    private int calculateTimeInSecondsTo4AM() {
        final LocalDateTime now = LocalDateTime.now();
        LocalDateTime secondsTo4AM = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 3, 0);
        if (secondsTo4AM.isBefore(now)) secondsTo4AM = secondsTo4AM.plusDays(1);
        System.out.println(MCPAL_TAG + "Time until Backup starts: " + ChronoUnit.HOURS.between(now, secondsTo4AM) + ":" + ChronoUnit.MINUTES.between(now, secondsTo4AM) % 60);
        return (int) ChronoUnit.SECONDS.between(now, secondsTo4AM);
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

            if (consoleWriterThread != null) consoleThread.interrupt();
            consoleWriterThread = new Thread(ConsoleInput::new);
            consoleWriterThread.start();

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
            process.waitFor(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            process.destroy();
        }
    }

    public static synchronized void backupServer() {
        try {
            App.stopMinecraftServer(App.serverProcess, "[Server backup]");

            Thread.sleep(2000);

            Files.createDirectories(SOURCE_DIR_PATH);
            Backup backupHandler = new Backup(SOURCE_DIR_PATH, TARGET_DIR_PATH);
            FutureTask<String> futureTask = new FutureTask<>(backupHandler);
            new Thread(futureTask).start();
            String backupStorePath = futureTask.get();

            List<String> commandListClone = new ArrayList<>(ADDITIONAL_COMMANDS_AFTER_BACKUP);
            commandListClone.replaceAll(command -> command.replace("{2}", backupStorePath));
            for (String command : commandListClone) {
                final ProcessBuilder processBuilder = new ProcessBuilder(command);
                new Thread(() -> {
                    try {
                        processBuilder.start();
                    } catch (IOException e) {
                        System.out.println(MCPAL_TAG + "The following command failed: " + command);
                    }
                }).start();
            }

            Thread.sleep(2000);
            serverProcess = startMinecraftServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}