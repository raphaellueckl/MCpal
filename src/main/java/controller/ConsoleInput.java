package controller;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class ConsoleInput implements Runnable {
	
	Process sProcess;
	
	@Override
	public void run() {
		Scanner scan = new Scanner(System.in);
		this.sProcess = App.serverProcess;
		PrintWriter w = new PrintWriter(new OutputStreamWriter(App.serverProcess.getOutputStream()));
		while (true) {
			String msg = scan.nextLine();
			handleCommandMessage(msg);
			if (!sProcess.equals(App.serverProcess)) {
				sProcess = App.serverProcess;
				w = new PrintWriter(new OutputStreamWriter(App.serverProcess.getOutputStream()));
			}
			w.println(msg);
			w.flush();
		}
	}

	private void handleCommandMessage(String msg) {
		if (msg.startsWith("stop ")) {
			App.stopMinecraftServer(App.serverProcess, "[Server stop]");
		} else if (msg.equals("start")) {
			App.startMinecraftServer();
		}
	}
}
