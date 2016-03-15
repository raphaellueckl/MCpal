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
		while (true) {
			String msg = scan.nextLine();
			handleCommandMessage(msg);
		}

	}

	private void handleCommandMessage(String msg) {
		if (msg.startsWith("stop ")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]");
		} else if (msg.equals("stop")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]");
		} else if (msg.equals("start")) {
			App.startMinecraftServer();
		} else if (msg.equals("backup")) {
			App.backupServer();
		} else if (msg.equals("help")) {
			printToConsole("Here is a list of MCpal's commands you can use here on the console:\n" +
					"stop       Shutdown the server\n" +
					"start      Start the server\n" +
					"backup     Shutdown, backup, restart the server");
		} else {
			printToConsole(msg);
		}
	}

	private void printToConsole(String msg) {
		if (!sProcess.equals(App.serverProcess)) {
			sProcess = App.serverProcess;
			consolePrinter = new PrintWriter(new OutputStreamWriter(App.serverProcess.getOutputStream()));
		}
		consolePrinter.println(msg);
		consolePrinter.flush();
	}
}
