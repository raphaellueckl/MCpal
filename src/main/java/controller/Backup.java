package controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.Callable;

public class Backup implements Callable<Integer> {

    private final Path sourceDirPath;
    private final Path targetDirPath;

    public Backup(String sourceDirPath, String targetDirPath) {
        this.sourceDirPath = Paths.get(sourceDirPath);
        this.targetDirPath = Paths.get(targetDirPath);
    }

    public int startTransfer() {
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

    @Override
    public Integer call() throws Exception {
        return synchronize(sourceDirPath);
    }
}
