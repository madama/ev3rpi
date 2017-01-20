package org.danysoft.ev3rpi;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.danysoft.ev3rpi.AudioUtils.AudioRecorder;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.VoiceId;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Emotion;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;

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
	private EV3DevUtils ev3DevUtils;

	private AudioRecorder recorder;
	private AudioInputStream audioIS;
	private Mixer mixer;

	private LexWrapper lex;
	private PollyWrapper polly;
	private RekognitionWrapper rekognition;

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
		if (properties.containsKey("ev3dev.address")) {
			String ip = properties.getProperty("ev3dev.address");
			ev3DevUtils = new EV3DevUtils(ip);
		}
		String accessKey = properties.getProperty("AWS_ACCESS_KEY");
		String secretKey = properties.getProperty("AWS_SECRET_KEY");
		BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
		com.amazonaws.android.auth.BasicAWSCredentials lexCredentials = new com.amazonaws.android.auth.BasicAWSCredentials(accessKey, secretKey);
		lex = new LexWrapper(new AmazonLexRuntimeClient(lexCredentials), "LegoRPI", "EveRPI");
		polly = new PollyWrapper(new AmazonPollyClient(awsCredentials), VoiceId.Brian);
		rekognition = new RekognitionWrapper(new AmazonRekognitionClient(awsCredentials));
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

		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
		ButtonGroup group = new ButtonGroup();
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info info : mixerInfos) {
			Mixer m = AudioSystem.getMixer(info);
			Line.Info[] lineInfos = m.getTargetLineInfo();
			if (lineInfos.length > 0 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
				JRadioButton button = new JRadioButton();
				button.setText(info.getName());
				button.setActionCommand(info.toString());
				button.addActionListener(setInput);
				buttonPanel.add(button);
				group.add(button);
			}
		}
		panel.add(buttonPanel);

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
		appendLog(message);
	}

	private void appendLog(String message) {
		log.append(message);
		log.append("\n");
	}

	private ActionListener setInput = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			for (Mixer.Info info : AudioSystem.getMixerInfo()) {
				if (arg0.getActionCommand().equals(info.toString())) {
					Mixer newValue = AudioSystem.getMixer(info);
					mixer = newValue;
					break;
				}
			}
		}
	};

	private class RecActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			if (recorder == null) {
				((JButton)event.getSource()).setText("RECORDING...");
				appendLog("Start Audio Recording...");
				recorder = audioUtils.startRecording(mixer);
			} else {
				recorder.stop();
				while (!recorder.isDone()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException err) {
						appendLog("ERROR: " + err.getMessage());
					}
				}
				((JButton)event.getSource()).setText("REC");
				audioIS = recorder.getAudioInputStream();
				appendLog("Audio Recording Complete! " + recorder.getDuration());
				recorder = null;
				audioUtils.saveAudio("fromMic.wav", audioIS);
				appendLog("Send audio to Lex");
				String lexOutput = lex.sendAudio(audioIS);
				appendCommandLog(lexOutput);
				if (lexOutput.startsWith("COMMAND: rekognition")) {
					int threshold = 50;
					String command = lexOutput.substring(20).trim();
					if (command.equals("labels")) {
						camUtils.capture("capture.png");
						Image image = new Image();
						try {
							image.setBytes(ByteBuffer.wrap(Files.readAllBytes(Paths.get("capture.png"))));
						} catch (IOException e1) {
							e1.printStackTrace(System.err);
						}
						List<Label> labels = rekognition.detectLabels(image);
						StringBuffer labelsText = new StringBuffer("In this photo I can recognize: ");
						for (Label l : labels) {
							if (l.getConfidence().intValue() > threshold) {
								labelsText.append(l.getName()).append(", ");
							}
						}
						talk(labelsText.toString());
					} else if (command.equals("face")) {
						camUtils.capture("capture.png");
						Image image = new Image();
						try {
							image.setBytes(ByteBuffer.wrap(Files.readAllBytes(Paths.get("capture.png"))));
						} catch (IOException e1) {
							e1.printStackTrace(System.err);
						}
						List<FaceDetail> faces = rekognition.detectFaces(image);
						StringBuffer facesText = new StringBuffer("In this face I can recognize: ");
						for (FaceDetail f : faces) {
							facesText.append("face! ");
							if (f.getBeard().getConfidence().intValue() > threshold && f.getBeard().isValue()) {
								facesText.append("beard, ");
							}
							List<Emotion> emotions = f.getEmotions();
							for (Emotion e : emotions) {
								if (e.getConfidence().intValue() > threshold) {
									facesText.append(e.getType()).append(", ");
								}
							}
							if (f.getEyeglasses().getConfidence().intValue() > threshold && f.getEyeglasses().isValue()) {
								facesText.append("eyeglasses, ");
							}
							if (f.getEyesOpen().getConfidence().intValue() > threshold && f.getEyesOpen().isValue()) {
								facesText.append("eyes open, ");
							}
							if (f.getGender().getConfidence().intValue() > threshold) {
								facesText.append(f.getGender().getValue()).append(", ");
							}
							if (f.getMouthOpen().getConfidence().intValue() > threshold && f.getMouthOpen().isValue()) {
								facesText.append("mouth open, ");
							}
							if (f.getMustache().getConfidence().intValue() > threshold && f.getMustache().isValue()) {
								facesText.append("mustache, ");
							}
							if (f.getSmile().getConfidence().intValue() > threshold && f.getSmile().isValue()) {
								facesText.append("smile, ");
							}
							if (f.getSunglasses().getConfidence().intValue() > threshold && f.getSunglasses().isValue()) {
								facesText.append("sunglasses, ");
							}
						}
						talk(facesText.toString());
					} else if (command.equals("SearchFacesByImage")) {
						
					}
				} else if (lexOutput.startsWith("COMMAND: ev3dev")) {
					String command = lexOutput.substring(15).trim();
					ev3DevUtils.execCommand(command);
				} else {
					talk(lexOutput);
				}
			}
		}
	}

	private void talk(String text) {
		appendLog("Send text to Polly: " + text);
		InputStream tts = polly.tts(text);
		audioUtils.saveAudio("fromPolly.wav", tts);
		audioUtils.playAudio("fromPolly.wav");
	}

}
