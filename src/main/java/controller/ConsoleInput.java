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
		} else {

			if (!sProcess.equals(App.serverProcess)) {
				sProcess = App.serverProcess;
				consolePrinter = new PrintWriter(new OutputStreamWriter(App.serverProcess.getOutputStream()));
			}
			consolePrinter.println(msg);
			consolePrinter.flush();
		}
	}
}
