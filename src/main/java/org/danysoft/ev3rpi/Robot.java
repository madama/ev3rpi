package org.danysoft.ev3rpi;

import java.awt.EventQueue;

import javax.swing.JFrame;

public class Robot extends JFrame {

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			Robot ui = new Robot();
			ui.setVisible(true);
		});
	}

	public Robot() {
		start();
	}

	public void start() {
		setTitle("EV3 RPI");
		setSize(300, 200);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

}
