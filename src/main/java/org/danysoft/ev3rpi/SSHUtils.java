package org.danysoft.ev3rpi;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class SSHUtils {

	private SSHClient ssh;

	public SSHUtils(String ip, String user, String password) {
		ssh = connect(ip, user, password);
	}

	@SuppressWarnings("resource")
	private static SSHClient connect(String ip, String user, String password) {
		final SSHClient ssh = new SSHClient();
		ssh.addHostKeyVerifier(new PromiscuousVerifier());
		while (!ssh.isConnected()) {
			try {
				ssh.connect(ip);
			} catch (Exception e) {
				System.out.println("Retrying SSH connection with " + ip + "...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace(System.err);
				}
			}
		}
		while (!ssh.isAuthenticated()) {
			try {
				ssh.authPassword(user, password);
			} catch (Exception e) {
				if (!ssh.isConnected()) {
					throw new IllegalStateException("SSH Connection dropped!");
				}
				System.out.println("SSH authentication problem: " + e.getMessage() + " RETRY!");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace(System.err);
				}
			}
		}
		System.out.println("SSH connection established");
		return ssh;
	}

	public void exec(String command) throws Exception {
		exec(ssh, command, false);
	}

	public void exec(String command, boolean debug) throws Exception {
		exec(ssh, command, debug);
	}

	private void exec(SSHClient ssh, String command, boolean debug) throws Exception {
		Session session = ssh.startSession();
		if (debug) {
			command += " 2>&1";
		}
		session.allocateDefaultPTY();
		Command cmd = session.exec(command);
		String output = IOUtils.readFully(cmd.getInputStream()).toString();
		cmd.join();
		cmd.close();
		session.close();
		if (debug) {
			System.out.println(output);
		}
	}

}
