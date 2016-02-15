package controller;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;

public class App {

    private final Path sourceDirPath;
    private final Path targetDirPath;

    public App(String sourceDir, String targetDir) {
        this.sourceDirPath = Paths.get(sourceDir);
        this.targetDirPath = Paths.get(targetDir);
    }

    public static void main(String... args) throws IOException {

        final String fromPath;
        final String toPath;

        if (args.length == 2) {
            fromPath = args[0];
            toPath = args[1];
        } else {
            URL workingDirectory = App.class.getProtectionDomain().getCodeSource().getLocation();

            final List<String> arguments = Files.readAllLines(Paths.get(workingDirectory + "config.cfg"));
            if (arguments.size() != 2) throw new RuntimeException("Invalid input parameters");

            fromPath = arguments.get(0);
            toPath = arguments.get(1);
        }

        Backup backup = new Backup(fromPath, toPath);

        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(fromPath));
        processBuilder.command("java", "-jar", "server.jar", "nogui");
        Process process = processBuilder.start();

        MinecraftConsole console = new MinecraftConsole(process.getInputStream());
        new Thread(console).start();


        App app = new App(fromPath, toPath);
        app.startTransfer();

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
                new DailyBackupTask(),
                date.getTime(),
                1000 * 60 * 60 * 24 * 7
        );







    }

    public void startTransfer() {
        synchronize(sourceDirPath);
    }

    private void synchronize(Path currentFolder) {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}