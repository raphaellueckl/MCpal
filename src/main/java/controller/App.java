package controller;

import java.io.*;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;

public class App {

    private final Path sourceDirPath;
    private final Path targetDirPath;

    public App(String sourceDir, String targetDir) {
        this.sourceDirPath = Paths.get(sourceDir);
        this.targetDirPath = Paths.get(targetDir);
    }

    public static void main(String... args) throws IOException {

        String fromPath;
        String toPath;
        if (args.length == 2) {
            fromPath = args[0];
            toPath = args[1];
        } else {
            URL workingDirectory = App.class.getProtectionDomain().getCodeSource().getLocation();

            RandomAccessFile aFile = new RandomAccessFile(
                    workingDirectory + "config.cfg", "r");
            FileChannel inChannel = aFile.getChannel();
            long fileSize = inChannel.size();
            ByteBuffer buffer = ByteBuffer.allocate((int) fileSize);
            inChannel.read(buffer);
            //buffer.rewind();
            buffer.flip();
            for (int i = 0; i < fileSize; i++)
            {
                System.out.print((char) buffer.get());
            }
            inChannel.close();
            aFile.close();
        }



        final ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File("C:/Users/Raphael/Desktop/New folder (4)/"));
        processBuilder.command("java", "-jar", "server.jar");
        Process process = processBuilder.start();

        MinecraftConsole console = new MinecraftConsole(process.getInputStream());
        new Thread(console).start();

        fromPath = "C:\\Users\\Raphael\\Desktop\\Folder 1";
        toPath = "C:\\Users\\Raphael\\Desktop\\Folder 2\\destinationfolder";

        Backup backup = new Backup(fromPath, toPath);

        App app = new App(fromPath, toPath);
        app.startTransfer();

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