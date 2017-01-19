package org.danysoft.ev3rpi;

import java.io.InputStream;
import java.util.Scanner;

public class EV3DevUtils {

	private SSHUtils sshUtils;
	public boolean debug = false;

	public EV3DevUtils(String ip) {
		sshUtils = new SSHUtils(ip);
		execCommand("init");
	}

	public void execCommand(String command) {
		try {
			sshUtils.exec(loadCommand(command), debug);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private String loadCommand(String command) {
		InputStream is = getClass().getResourceAsStream("ev3dev_" + command + ".ssh");
		Scanner scanner = new Scanner(is);
		String fullCommand = scanner.next();
		scanner.close();
		return fullCommand;
	}

}
