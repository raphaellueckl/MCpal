package controller;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class ConsoleInput implements Runnable {
	
	private Process sProcess;
	private PrintWriter consolePrinter;

	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		this.sProcess = App.serverProcess;
		consolePrinter = new PrintWriter(new OutputStreamWriter(App.serverProcess.getOutputStream()));
		String msg;
		while (!Thread.currentThread().isInterrupted()) {
			msg = scan.nextLine();
			System.out.println("Received command string");
			handleCommandMessage(msg);
		}
		System.out.println("ConsoleWriter dies.");
	}

	private void handleCommandMessage(String msg) {
		if (msg.equals("stop")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]", true);
		} else if (msg.equals("istop")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]", false);
		} else if (msg.startsWith("stop")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]", true);
		} else if (msg.equals("start")) {
			App.startMinecraftServer();
		} else if (msg.equals("backup")) {
			App.backupServer();
		} else if (msg.equals("help")) {
			printToConsole("Here is a list of MCpal's commands you can use here on the console:\n" +
					"stop       Shutdown the server\n" +
					"start      Start the server\n" +
					"backup     Shutdown, backup, restart the server\n\n" +
					"for further information about the addidtional commands, type \"help-com\"");
		} else if (msg.equals("help-com")) {
			printToConsole(
					"You can add additional calls for programs after the usual arguments. What does this mean?\n" +
							"You start MCpal like this: java -jar MCpal, TARGET_DIR_PATH RAM_SIZE SERVER_JAR_NAME\n" +
							"If you want to create a map out of your backup, you can download \"Minecraft Overviewer\"\n" +
							"and run this program externally like this:\n" +
							"java -jar MCpal, [TARGET_DIR_PATH] [RAM_SIZE] [SERVER_JAR_NAME] \"PATH_TO_OVERVIEWER_EXE\" {1} {2}\"\n" +
							"{1} will be replaced with the name of your world automatically\n" +
							"{2} will be replaced with the path to the newly created backup that MCpal did.\n" +
							"The overviewer will run in a separate thread and your minecraft server will restart\n" +
							"right after the update.");
		} else {
			printToConsole(msg);
		}
	}

	private void printToConsole(String msg) {
		if (!sProcess.equals(App.serverProcess)) {
			consolePrinter = new PrintWriter(App.serverProcess.getOutputStream());
			sProcess = App.serverProcess;
		}
		consolePrinter.println(msg);
		consolePrinter.flush();
	}
}
