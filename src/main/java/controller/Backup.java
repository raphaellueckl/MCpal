package controller;

import java.io.IOException;
import java.nio.file.*;

public class Backup {

    private final Path sourceDirPath;
    private final Path targetDirPath;

    public Backup(String sourceDirPath, String targetDirPath) {
        this.sourceDirPath = Paths.get(sourceDirPath);
        this.targetDirPath = Paths.get(targetDirPath);
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
