package org.danysoft.ev3rpi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import com.amazonaws.services.sqs.model.Message;

public class SQSReader implements Runnable {

	private RobotUI ui;

	public SQSReader(RobotUI rui) {
		ui = rui;
	}

	@Override
	public void run() {
		while (true) {
			List<Message> messages = ui.sqs.readMessages();
			System.out.println("Trying to read from SQS");
			for (Message m : messages) {
				System.out.println("Message found: " + m.getBody());
				if (m.getBody().equals("StartAutoDiscovery")) {
					for(ActionListener a: ui.auto.getActionListeners()) {
						a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
					}
				} else if (m.getBody().equals("StopAutoDiscovery")) {
					for(ActionListener a: ui.auto.getActionListeners()) {
						a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null) {});
					}
				}
				ui.sqs.deleteMessage(m.getReceiptHandle());
			}
		}
	}

}
