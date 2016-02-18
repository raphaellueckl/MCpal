package controller;

import java.io.BufferedWriter;
import java.util.TimerTask;
import java.util.concurrent.FutureTask;

public class DailyBackupTask extends TimerTask {

    private final Backup backupHandler;
    private final BufferedWriter consoleWriter;

    public DailyBackupTask(Backup backupTask, BufferedWriter consoleWriter) {
        this.backupHandler = backupTask;
        this.consoleWriter = consoleWriter;
    }

    @Override
    public void run() {
        try {
            consoleWriter.write(Constants.STOP_COMMAND);
            Thread.sleep(2000);



            FutureTask<Integer> futureTask = new FutureTask<>(backupHandler);
            new Thread(futureTask).start();
            Integer result = futureTask.get();

            consoleWriter.write(Constants.START_COMMAND);

        } catch (Exception e) { e.printStackTrace(); }
    }
}
