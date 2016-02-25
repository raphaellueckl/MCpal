package controller;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class App {

    public static final String CONFIG_FILE = "MCpal.cfg";

    public final Path SOURCE_DIR_PATH;
    public final Path TARGET_DIR_PATH;
    public final String MAX_HEAP_SIZE;
    public final String JAR_NAME;

    public final String START_COMMAND;

    Thread consoleThread;
    private volatile Process serverProcess;

    public static void main(String... args) throws IOException {
        final String fromPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        //final String fromPath = "C:/Users/rapha/Desktop/mc/";
        final String toPath;
        final String maxHeapSize;
        final String jarName;

        if (args.length == 3) {
            toPath = args[0];
            maxHeapSize = args[1];
            jarName = args[2];
            writeConfigFile(fromPath, args);
        } else if (Files.exists(Paths.get(fromPath + CONFIG_FILE))) {
            final List<String> arguments = Files.readAllLines(Paths.get(fromPath + CONFIG_FILE));
            if (arguments.size() != 3) throw new RuntimeException("Invalid input parameters");
            Files.delete(Paths.get(fromPath + CONFIG_FILE));
            toPath = arguments.get(0);
            maxHeapSize = arguments.get(1);
            jarName = arguments.get(2);
        } else {
            throw new IllegalStateException("Invalid Input Parameters. Please start this App file like this:\n" +
                    "java -jar MCpal.jar PATH_TO_BACKUP_FOLDER MAX_RAM_YOU_WANNA_SPEND NAME_OF_MINECRAFT_SERVER_JAR\n" +
                    "Example: java -jar MCpal.jar \"C:\\Users\\Rudolf Ramses\\Minecraft\" 1024 minecraft_server.jar");
        }

        App MCpal = new App(fromPath, toPath, maxHeapSize, jarName);
        MCpal.start();
    }

    private static void writeConfigFile(String fromPath, String[] args) throws IOException {
        Files.createFile(Paths.get(fromPath + CONFIG_FILE));
        FileWriter fw = new FileWriter(fromPath + CONFIG_FILE);
        for (String parameter : args) {
            fw.write(parameter + System.getProperty("line.separator"));
        }
        fw.flush();
        fw.close();
    }


    public App(String sourceDir, String targetDir, String maxHeapSize, String jarName) {
        this.MAX_HEAP_SIZE = maxHeapSize;
        this.JAR_NAME = jarName;
        this.SOURCE_DIR_PATH = Paths.get(sourceDir);
        this.TARGET_DIR_PATH = Paths.get(targetDir);

        START_COMMAND = "java -jar -Xms" + MAX_HEAP_SIZE + "m" +
                " -Xmx" + MAX_HEAP_SIZE + "m " + JAR_NAME + " nogui";
    }

    private void start() throws IOException {
        Backup backupTask = new Backup(SOURCE_DIR_PATH, TARGET_DIR_PATH);

        serverProcess = startMinecraftServer();

        DailyBackupTask dailyTask = new DailyBackupTask(backupTask);

        Timer timer = new Timer();
        Calendar date = Calendar.getInstance();
        date.set(
                Calendar.DAY_OF_WEEK,
                Calendar.SUNDAY
        );
        date.set(Calendar.HOUR, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        // Schedule to run every Sunday in midnight
        timer.schedule(
                dailyTask,
                date.getTime(),
                //1000 * 60 * 60 * 24 * 7
                1000 * 60 * 1 //1 minute
        );
    }

    public Process startMinecraftServer() {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder("java", "-jar", "-Xms" + MAX_HEAP_SIZE + "m",
                    "-Xmx" + MAX_HEAP_SIZE + "m", JAR_NAME , "nogui");
            processBuilder.directory(SOURCE_DIR_PATH.toFile());
            final Process process = processBuilder.start();

            consoleThread.interrupt();
            consoleThread = new Thread(new MinecraftConsole(process.getInputStream()));
            consoleThread.start();

            return process;
        } catch (IOException ioe) { return null; }
    }

    public static void stopMinecraftServer(Process process) {
        PrintWriter w = new PrintWriter(process.getOutputStream());
        w.write("stop");
        w.flush();
        w.close();
        try { process.waitFor(10, TimeUnit.SECONDS); } catch (InterruptedException e) { e.printStackTrace(); }
        process.destroy();
    }

    public class DailyBackupTask extends TimerTask {

        private final Backup backupHandler;

        public DailyBackupTask(Backup backupTask) {
            this.backupHandler = backupTask;
        }

        @Override
        public void run() {
            try {
                stopMinecraftServer(serverProcess);
                Thread.sleep(2000);

                FutureTask<Integer> futureTask = new FutureTask<>(backupHandler);
                new Thread(futureTask).start();
                Integer result = futureTask.get();

                Thread.sleep(2000);
                serverProcess = startMinecraftServer();

            } catch (Exception e) { e.printStackTrace(); }
        }
    }


    public class Backup implements Callable<Integer> {

        private final Path sourceDirPath;
        private final Path targetDirPath;

        public Backup(Path sourceDirPath, Path targetDirPath) {
            this.sourceDirPath = sourceDirPath;
            this.targetDirPath = targetDirPath;
        }

        private int synchronize(Path currentFolder) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(currentFolder);
                for (final Path currentElement : directoryStream) {
                    if (Files.isDirectory(currentElement)) synchronize(currentElement);
                    else if (Files.isRegularFile(currentElement)) {
                        final Path relativePath = sourceDirPath.relativize(currentElement);
                        Path targetPath = targetDirPath.resolve(relativePath);
                        if (!Files.exists(targetPath))
                            Files.createDirectories(targetPath.getParent());
                        Files.copy(currentElement, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                return 0;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        @Override
        public Integer call() throws Exception {
            return synchronize(sourceDirPath);
        }
    }

    public class MinecraftConsole implements Runnable {

        private final BufferedReader consoleInputReader;

        public MinecraftConsole(InputStream consoleInputStream) {
            consoleInputReader = new BufferedReader(new InputStreamReader(consoleInputStream));
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = consoleInputReader.readLine()) != null) { System.out.println(line); }
            } catch (IOException ioe) { ioe.printStackTrace(); }
        }
    }
}