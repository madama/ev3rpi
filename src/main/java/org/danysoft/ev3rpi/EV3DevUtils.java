package org.danysoft.ev3rpi;

import java.io.IOException;
import java.io.InputStream;

import net.schmizz.sshj.common.IOUtils;

public class EV3DevUtils {

	private SSHUtils sshUtils;
	public boolean debug = true;

	public EV3DevUtils(String ip, String username, String password) {
		sshUtils = new SSHUtils(ip, username, password);
		execCommand("init");
	}

	public void execCommand(String command) {
		//System.out.println("EV3DEV: Exex command " + command);
		try {
			sshUtils.exec(loadCommand(command), debug);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private String loadCommand(String command) {
		InputStream is = getClass().getResourceAsStream("ev3dev_" + command + ".ssh");
		String fullCommand = "";
		try {
			fullCommand = IOUtils.readFully(is).toString();
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
		//System.out.println(fullCommand);
		return fullCommand;
	}

}
