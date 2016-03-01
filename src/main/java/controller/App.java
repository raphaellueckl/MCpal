package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

    public static final String CONFIG_FILENAME = "MCpal.cfg";

    public final Path SOURCE_DIR_PATH;
    public final Path TARGET_DIR_PATH;
    public final String MAX_HEAP_SIZE;
    public final String JAR_NAME;

    public final String START_COMMAND;

    Thread consoleThread;
    private volatile Process serverProcess;

    public static void main(String... args) throws IOException {
//        final String fromPath = App.class.getProtectionDomain().getCodeSource().getLocation().getPath();
//        final Path fromPathPath = Paths.get(fromPath).getParent();
//        final String fromPath = "C:/Users/ralu/Desktop/mc/";
        final String fromPath;
        final String toPath;
        final String maxHeapSize;
        final String jarName;
        if (args.length == 4) {
            fromPath = args[0];
            toPath = args[1];
            maxHeapSize = args[2];
            jarName = args[3];
            writeConfigFile(fromPath, args);
            checkEula(fromPath);
//        } else if (Files.exists(Paths.get(fromPath + CONFIG_FILE))) {
//            final List<String> arguments = Files.readAllLines(Paths.get(fromPath + CONFIG_FILE));
//            if (arguments.size() != 4) throw new RuntimeException("Invalid input parameters");
//            Files.delete(Paths.get(fromPath + CONFIG_FILE));
//            toPath = arguments.get(0);
//            maxHeapSize = arguments.get(1);
//            jarName = arguments.get(2);
        } else {
            throw new IllegalStateException("Invalid Input Parameters. Please start this App file like this:\n" +
                    "java -jar MCpal.jar PATH_TO_BACKUP_FOLDER MAX_RAM_YOU_WANNA_SPEND NAME_OF_MINECRAFT_SERVER_JAR\n" +
                    "Example: java -jar MCpal.jar \"C:\\Users\\Rudolf Ramses\\Minecraft\" 1024 minecraft_server.jar");
        }

        App MCpal = new App(fromPath, toPath, maxHeapSize, jarName);
        MCpal.start();
    }

    private static void checkEula(String fromPath) {
        try {
            Path eulaPath = Paths.get(fromPath + "/" + "eula.txt");
            File eulaFile = eulaPath.toFile();
            //Irgendwie müsste die Eula generiert werden, wenn sie nicht vorhanden ist.
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

    private static void writeConfigFile(String fromPath, String[] args) throws IOException {
        if (!Files.exists(Paths.get(fromPath + "/" + CONFIG_FILENAME)))
            Files.createFile(Paths.get(fromPath + "/" + CONFIG_FILENAME));
        FileWriter fw = new FileWriter(fromPath + "/" + CONFIG_FILENAME);
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
        serverProcess = startMinecraftServer();

        DailyBackupTask dailyTask = new DailyBackupTask(SOURCE_DIR_PATH, TARGET_DIR_PATH);
        final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleWithFixedDelay(dailyTask, 30, 30, TimeUnit.SECONDS);

        final LocalDateTime now = LocalDateTime.now();
        final int year = now.getDayOfYear();
        final Month month = now.getMonth();
        final int dayOfMonth = now.getDayOfMonth();
        final int 
    }

    public Process startMinecraftServer() {
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

    public static void stopMinecraftServer(Process process) {
        runBackupStartCountDown(process);
        try {
            System.out.println("CurrentTime: " + System.currentTimeMillis());
            process.waitFor(10, TimeUnit.SECONDS);
            System.out.println("After the wait: " + System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void runBackupStartCountDown(Process process) {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
        w.println("say Serverbackup begins in 3...");
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
    }

    public class DailyBackupTask implements Runnable {

        private final Path sourceDir;
        private final Path backupDir;

        public DailyBackupTask(Path sourceDir, Path backupDir) {
            this.sourceDir = sourceDir;
            this.backupDir = backupDir;
        }

        @Override
        public void run() {
            try {
                stopMinecraftServer(serverProcess);
                new Thread(() -> {
                    try {
                        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
                        w.write("say Serverbackup begins in 3...");
                        w.flush();
                        Thread.sleep(1000);
                        w.write("say 2...");
                        w.flush();
                        Thread.sleep(1000);
                        w.write("say 1...");
                        w.flush();
                        Thread.sleep(1000);
                        w.write("say GAME OVER!!!!!!!!!!!!!...");
                        w.flush();
                        w.write("stop");
                        w.flush();
                        w.close();
                        serverProcess.waitFor(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e1) {
                        serverProcess.destroy();
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }).start();
                Thread.sleep(2000);

                Backup backupHandler = new Backup(sourceDir, backupDir);
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
        private Path targetDirPath;

        public Backup(Path sourceDirPath, Path targetDirPath) {
            this.sourceDirPath = sourceDirPath;
            this.targetDirPath = targetDirPath;
        }

        @Override
        public Integer call() throws Exception {
            String backupFolderName = String.valueOf(LocalDateTime.now().getYear()) + "_" +
                    (String.valueOf(LocalDateTime.now().getMonth().getValue()).length() == 1 ? "0" + String.valueOf(LocalDateTime.now().getMonth().getValue()) : String.valueOf(LocalDateTime.now().getMonth().getValue())) + "_" +
                    String.valueOf(LocalDateTime.now().getDayOfMonth()) + "_" +
                    String.valueOf(LocalDateTime.now().getNano());
            String path = targetDirPath + "/" + backupFolderName + "/";
            Path path2 = Paths.get(targetDirPath.toString(), backupFolderName);
            targetDirPath = path2;
            return synchronize(sourceDirPath);
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