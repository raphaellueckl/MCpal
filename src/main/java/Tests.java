import model.Variables;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static model.Variables.MCPAL_TAG;

public class Tests {

    public static void main(String[] args) throws Exception {
//        final String commandListClone = "python /home/raphael/Desktop/hello.py";
        final String commandListClone = "C:/Python27/python C:\\Users\\rapha\\Desktop\\hello.py";
        final List<String> parametersOfCommand = Arrays.asList(commandListClone.split(" "));

        final ProcessBuilder processBuilder = new ProcessBuilder(parametersOfCommand);
        processBuilder.environment().put(Variables.ENVIRONMENT_VARIABLE_CURRENT_BACKUP_DIR_PATH, "/home/raphael");
        try {
//            processBuilder.redirectOutput(new File("/home/raphael/Desktop/output.txt"));
//            processBuilder.redirectInput(new File("/home/raphael/Desktop/output.txt"));
//            processBuilder.redirectError(new File("/home/raphael/Desktop/output.txt"));
            Process start = processBuilder.start();
            BufferedReader b = new BufferedReader(new InputStreamReader(new BufferedInputStream(start.getInputStream())));
            String a;
            while ((a = b.readLine()) != null) {
                System.out.println(a);
            }
//            System.out.println(start.exitValue());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
