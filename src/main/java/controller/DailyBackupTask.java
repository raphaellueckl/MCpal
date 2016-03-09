package controller;

import java.nio.file.Path;
import java.util.concurrent.FutureTask;

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
            App.stopMinecraftServer(App.serverProcess, "[Server backup]");

            Thread.sleep(2000);
            Backup backupHandler = new Backup(sourceDir, backupDir);
            FutureTask<Integer> futureTask = new FutureTask<>(backupHandler);
            new Thread(futureTask).start();
            //TODO result can be used for error handling.
            Integer result = futureTask.get();

            Thread.sleep(2000);
            App.serverProcess = App.startMinecraftServer();

        } catch (Exception e) { e.printStackTrace(); }
    }
}