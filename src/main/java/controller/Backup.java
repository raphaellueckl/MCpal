package controller;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;

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
                (String.valueOf(LocalDateTime.now().getMonth().getValue()).length() == 1 ? 
                		"0" + String.valueOf(LocalDateTime.now().getMonth().getValue()) : 
                			String.valueOf(LocalDateTime.now().getMonth().getValue())) + "_" +
                String.valueOf(LocalDateTime.now().getDayOfMonth()) + "_" +
                String.valueOf(LocalDateTime.now().getNano()).replaceAll("0", "");
        targetDirPath = targetDirPath.resolve(backupFolderName);
        return sync(sourceDirPath);
    }

    private int sync(Path currentFolder) {
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(currentFolder);
            for (final Path currentElement : directoryStream) {
                if (Files.isDirectory(currentElement)) sync(currentElement);
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
