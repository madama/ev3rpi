package org.danysoft.ev3rpi;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.danysoft.ev3rpi.AudioUtils.AudioRecorder;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;

public class RobotUI extends JFrame {

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		SwingUtilities.invokeLater(() -> {
			RobotUI ui = new RobotUI();
			ui.setVisible(true);
		});
	}

	private Properties properties = new Properties();

	private JPanel panel;
	private JButton rec;
	private JTabbedPane tabbedPane;
	private JPanel tabPanel1;
	private JTextArea commandLog;
	private JPanel tabPanel2;
	private JTextArea log;

	private OpenCVUtils cvUtils;
	private CamUtils camUtils;
	private AudioUtils audioUtils;

	private AudioRecorder recorder;
	private AudioInputStream audioIS;

	private LexWrapper lex;

	public RobotUI() {
		init();
		draw();
	}

	private void init() {
		cvUtils = new OpenCVUtils();
		camUtils = new CamUtils();
		audioUtils = new AudioUtils();
		try {
			properties.load(new FileInputStream("ev3rpi.properties"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		String accessKey = properties.getProperty("AWS_ACCESS_KEY");
		String secretKey = properties.getProperty("AWS_SECRET_KEY");
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		lex = new LexWrapper(new AmazonLexRuntimeClient(awsCredentials), "LegoRPI", "EveRPI");
	}

	private void draw() {
		setTitle("EV3 RPI");
		setLocationRelativeTo(null);
		panel = new JPanel();

		GridBagLayout gbPanel = new GridBagLayout();
		GridBagConstraints gbcPanel = new GridBagConstraints();
		panel.setLayout(gbPanel);

		rec = new JButton("REC");
		gbcPanel.gridx = 0;
		gbcPanel.gridy = 0;
		gbcPanel.gridwidth = 1;
		gbcPanel.gridheight = 1;
		gbcPanel.fill = GridBagConstraints.BOTH;
		gbcPanel.weightx = 1;
		gbcPanel.weighty = 0;
		gbcPanel.anchor = GridBagConstraints.NORTH;
		gbPanel.setConstraints(rec, gbcPanel);
		rec.setMnemonic(KeyEvent.VK_R);
		rec.addActionListener(new RecActionListener());
		panel.add(rec);

		tabbedPane = new JTabbedPane();
		tabPanel1 = new JPanel();
		GridBagLayout gbPanel1 = new GridBagLayout();
		GridBagConstraints gbcPanel1 = new GridBagConstraints();
		tabPanel1.setLayout(gbPanel1);
		commandLog = new JTextArea(2, 10);
		JScrollPane scpCommandLog = new JScrollPane(commandLog);
		gbcPanel1.gridx = 0;
		gbcPanel1.gridy = 0;
		gbcPanel1.gridwidth = 18;
		gbcPanel1.gridheight = 12;
		gbcPanel1.fill = GridBagConstraints.BOTH;
		gbcPanel1.weightx = 1;
		gbcPanel1.weighty = 1;
		gbcPanel1.anchor = GridBagConstraints.NORTH;
		gbPanel1.setConstraints(scpCommandLog, gbcPanel1);
		tabPanel1.add(scpCommandLog);
		tabbedPane.addTab("Command", tabPanel1);
		tabPanel2 = new JPanel();
		GridBagLayout gbPanel2 = new GridBagLayout();
		GridBagConstraints gbcPanel2 = new GridBagConstraints();
		tabPanel2.setLayout(gbPanel2);
		log = new JTextArea(2, 10);
		JScrollPane scpLog = new JScrollPane(log);
		gbcPanel2.gridx = 0;
		gbcPanel2.gridy = 0;
		gbcPanel2.gridwidth = 18;
		gbcPanel2.gridheight = 12;
		gbcPanel2.fill = GridBagConstraints.BOTH;
		gbcPanel2.weightx = 1;
		gbcPanel2.weighty = 1;
		gbcPanel2.anchor = GridBagConstraints.NORTH;
		gbPanel2.setConstraints(scpLog, gbcPanel2);
		tabPanel2.add(scpLog);
		tabbedPane.addTab("Log", tabPanel2);
		gbcPanel.gridx = 0;
		gbcPanel.gridy = 1;
		gbcPanel.gridwidth = 1;
		gbcPanel.gridheight = 1;
		gbcPanel.fill = GridBagConstraints.BOTH;
		gbcPanel.weightx = 1;
		gbcPanel.weighty = 1;
		gbcPanel.anchor = GridBagConstraints.NORTH;
		gbPanel.setConstraints(tabbedPane, gbcPanel);
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_C);
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_L);
		panel.add(tabbedPane);

		setContentPane(panel);
		pack();
		setSize(600, 400);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		appendLog("UI Complete!");
	}

	private void appendCommandLog(String message) {
		commandLog.append(message);
		commandLog.append("\n");
	}

	private void appendLog(String message) {
		log.append(message);
		log.append("\n");
	}

	private class RecActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (recorder == null) {
				((JButton)e.getSource()).setText("RECORDING...");
				appendLog("Start Audio Recording...");
				recorder = audioUtils.startRecording();
			} else {
				recorder.stop();
				while (!recorder.isDone()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException err) {
						appendLog("ERROR: " + err.getMessage());
					}
				}
				audioIS = recorder.getAudioInputStream();
				appendLog("Audio Recording Complete! " + recorder.getDuration());
				recorder = null;
				String lexOutput = lex.sendAudio(audioIS);
				appendCommandLog(lexOutput);
			}
		}
	}

}
