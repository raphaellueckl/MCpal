package controller;

import java.io.BufferedWriter;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.FutureTask;

public class DailyBackupTask extends TimerTask {

    private final Backup backupHandler;
    private final BufferedWriter consoleWriter;
    private final String startParameter;
    private final String STOP_COMMAND = "stop";

    public DailyBackupTask(Backup backupTask, BufferedWriter consoleWriter, String startParameters) {
        this.backupHandler = backupTask;
        this.consoleWriter = consoleWriter;
        this.startParameter = startParameters;
    }

    @Override
    public void run() {
        try {
            consoleWriter.write(STOP_COMMAND);
            Thread.sleep(2000);

            FutureTask<Integer> futureTask = new FutureTask<>(backupHandler);
            new Thread(futureTask).start();
            Integer result = futureTask.get();

            Thread.sleep(2000);
            consoleWriter.write(startParameter);

        } catch (Exception e) { e.printStackTrace(); }
    }
}
