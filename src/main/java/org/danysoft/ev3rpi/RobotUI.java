package org.danysoft.ev3rpi;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
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

	private JToggleButton rec;
	private JComboBox<String> mic;
	private JTextArea commandLog;
	private JTextArea log;
	private JLabel snapshot;

	private OpenCVUtils cvUtils;
	private CamUtils camUtils;
	private AudioUtils audioUtils;
	private EV3DevUtils ev3DevUtils;

	private AudioRecorder recorder;
	private AudioInputStream audioIS;
	private Mixer mixer;
	private Map<String, String> mixers = new HashMap<String, String>();

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
			String usr = properties.getProperty("ev3dev.usr");
			String pwd = properties.getProperty("ev3dev.pwd");
			ev3DevUtils = new EV3DevUtils(ip, usr, pwd);
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
		setSize(1200, 530);
		setMinimumSize(getSize());
		setMaximumSize(getSize());
		setPreferredSize(getSize());
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		// REC
		rec = new JToggleButton("REC");
		rec.setText("Rec");
		rec.setPreferredSize(new Dimension(120, 25));
		rec.setMnemonic(KeyEvent.VK_R);
		rec.addActionListener(new RecActionListener());

		// MIC
		mic = new JComboBox<String>();
		Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
		for (Mixer.Info info : mixerInfos) {
			Mixer m = AudioSystem.getMixer(info);
			Line.Info[] lineInfos = m.getTargetLineInfo();
			if (lineInfos.length > 0 && lineInfos[0].getLineClass().equals(TargetDataLine.class)) {
				mixers.put(info.getName(), info.toString());
			}
		}
		mic.setModel(new DefaultComboBoxModel<String>(mixers.values().toArray(new String[] {})));
		mic.addActionListener(setInput);
		mic.setPreferredSize(new Dimension(387, 25));

		// LOGS
		JTabbedPane logs = new JTabbedPane();
		JPanel commandPanel = new JPanel();
		JScrollPane scpCommandLog = new JScrollPane();
		commandLog = new JTextArea();
		commandLog.setColumns(20);
		commandLog.setRows(5);

		scpCommandLog.setViewportView(commandLog);
		GroupLayout commandPanelLayout = new GroupLayout(commandPanel);
		commandPanel.setLayout(commandPanelLayout);
		commandPanelLayout.setHorizontalGroup(
				commandPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scpCommandLog));
		commandPanelLayout.setVerticalGroup(commandPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(scpCommandLog, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		logs.addTab("Command", commandPanel);

		JPanel logPanel = new JPanel();
		JScrollPane scpLog = new JScrollPane();
		log = new JTextArea();
		log.setColumns(20);
		log.setRows(5);

		scpLog.setViewportView(log);
		GroupLayout logPanelLayout = new GroupLayout(logPanel);
		logPanel.setLayout(logPanelLayout);
		logPanelLayout.setHorizontalGroup(
				logPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scpLog));
		logPanelLayout.setVerticalGroup(logPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(scpLog, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		logs.addTab("Log", logPanel);

		logs.setMnemonicAt(0, KeyEvent.VK_C);
		logs.setMnemonicAt(1, KeyEvent.VK_L);

		// SNAPSHOT
		JPanel imagePanel = new JPanel();
		imagePanel.setMaximumSize(new Dimension(640, 480));
		imagePanel.setPreferredSize(new Dimension(640, 480));
		imagePanel.setSize(640, 480);
		snapshot = new JLabel(new ImageIcon(getClass().getResource("/splashscreen.png")));
		imagePanel.add(snapshot);

		GroupLayout imagePanelLayout = new GroupLayout(imagePanel);
		imagePanel.setLayout(imagePanelLayout);
		imagePanelLayout.setHorizontalGroup(imagePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(snapshot, GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE));
		imagePanelLayout.setVerticalGroup(imagePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addComponent(snapshot, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

		// LAYOUT
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addComponent(rec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(mic,
												GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE))
								.addComponent(logs))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(imagePanel, GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE).addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout
				.createSequentialGroup().addContainerGap()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout
						.createSequentialGroup()
						.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
								.addComponent(rec, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addComponent(mic, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(logs))
						.addComponent(imagePanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				.addContainerGap()));

		pack();
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
		@SuppressWarnings("unchecked")
		public void actionPerformed(ActionEvent event) {
			String selected = ((JComboBox<String>)event.getSource()).getSelectedItem().toString();
			for (Mixer.Info info : AudioSystem.getMixerInfo()) {
				if (selected.equals(info.toString())) {
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
					if (command.startsWith("label")) {
						Image image = takePicture();
						List<Label> labels = rekognition.detectLabels(image);
						StringBuffer labelsText = new StringBuffer("In this photo I can recognize: ");
						for (Label l : labels) {
							if (l.getConfidence().intValue() > threshold) {
								labelsText.append(l.getName()).append(", ");
							}
						}
						talk(labelsText.toString());
					} else if (command.startsWith("face")) {
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
					String command = lexOutput.substring(15).trim().replace(" ", "_");
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

	private Image takePicture() {
		String fileName = "capture.png";
		camUtils.capture(fileName);
		Image image = new Image();
		try {
			image.setBytes(ByteBuffer.wrap(Files.readAllBytes(Paths.get(fileName))));
			snapshot.setIcon(new ImageIcon(fileName));
		} catch (IOException e1) {
			e1.printStackTrace(System.err);
		}
		return image;
	}
}
