package org.danysoft.ev3rpi;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.imageio.ImageIO;
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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lexruntime.AmazonLexRuntime;
import com.amazonaws.services.lexruntime.AmazonLexRuntimeClient;
import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClient;
import com.amazonaws.services.polly.model.VoiceId;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Emotion;
import com.amazonaws.services.rekognition.model.Face;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.util.StringInputStream;

import net.schmizz.sshj.common.IOUtils;

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
	protected JToggleButton auto;
	private JComboBox<String> mic;
	private JTextArea commandLog;
	private JTextArea log;
	private JLabel snapshot;

	private OpenCVUtils cvUtils;
	private CamUtils camUtils;
	private AudioUtils audioUtils;
	protected EV3DevUtils ev3DevUtils;

	private AudioRecorder recorder;
	private Mixer mixer;
	private Map<String, String> mixers = new HashMap<String, String>();
	private String collectionFaces;
	private String facesBucket;
	private AutoRun autorun;

	protected LexWrapper lex;
	protected PollyWrapper polly;
	protected RekognitionWrapper rekognition;
	protected S3Wrapper s3;
	protected SQSWrapper sqs;

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
		AWSCredentialsProvider awsCredentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
		AmazonLexRuntime lexClient = AmazonLexRuntimeClient.builder().withRegion(Regions.US_EAST_1).withCredentials(awsCredentials).build();
		lex = new LexWrapper(lexClient, "LegoRPI", "EveRPI", "awsdemo");;
		AmazonPolly pollyClient = AmazonPollyClient.builder().withRegion(Regions.EU_WEST_1).withCredentials(awsCredentials).build();
		polly = new PollyWrapper(pollyClient, VoiceId.Brian);
		AmazonRekognition rekognitionClient = AmazonRekognitionClient.builder().withRegion(Regions.EU_WEST_1).withCredentials(awsCredentials).build();
		rekognition = new RekognitionWrapper(rekognitionClient);
		AmazonS3 s3Client = AmazonS3Client.builder().withRegion(Regions.EU_WEST_1).withCredentials(awsCredentials).build();
		s3 = new S3Wrapper(s3Client);
		AmazonSQS sqsClient = AmazonSQSClient.builder().withRegion(Regions.EU_WEST_1).withCredentials(awsCredentials).build();
		sqs = new SQSWrapper(sqsClient, "EV3RPIAlexa");
		collectionFaces = properties.getProperty("rekognition.collection");
		facesBucket = properties.getProperty("rekognition.faces.bucket");
		//rekognition.deleteCollection(collectionFaces);
		//rekognition.createCollection(collectionFaces);
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
		rec.setPreferredSize(new Dimension(70, 25));
		rec.setMnemonic(KeyEvent.VK_R);
		rec.addActionListener(new RecActionListener());

		// Auto
		auto = new JToggleButton("Auto");
		auto.setText("Auto");
		auto.setPreferredSize(new Dimension(70, 25));
		auto.setMnemonic(KeyEvent.VK_A);
		auto.addActionListener(new AutoActionListener());
		autorun = new AutoRun(this);
		new Thread(new SQSReader(this)).start();

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
		log.setLineWrap(true);

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
										.addComponent(auto, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
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
								.addComponent(auto, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
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

	protected void appendLog(String message) {
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
				startAudioRecording();
			} else {
				AudioInputStream audioIS = stopAudioRecording();
				audioUtils.saveAudio("fromMic.wav", audioIS);
				appendLog("Send audio to Lex");
				String lexOutput = lex.sendAudio(audioIS);
				appendCommandLog(lexOutput);
				if (lexOutput.startsWith("COMMAND: rekognition")) {
					int threshold = 50;
					String command = lexOutput.substring(20).trim();
					if (command.contains("label")) {
						Image image = takePicture();
						recognizeLabels(image, threshold);
					} else if (command.contains("face")) {
						Image image = takePicture();
						recognizeFaces(image, threshold);
					} else if (command.equals("SearchFacesByImage")) {
						Image image = takePicture();
						Map<String, String> face = recognizeFace(image);
						if (face.containsKey("faceName")) {
							String recognized = "Hi " + face.get("faceName") + "!";
							talk(recognized);
						} else {
							String out = lex.sendText("COMMAND Ask Name");
							if (out.equals("Your Face ID?")) {
								out = lex.sendText(face.get("faceId"));
								if (out.equals("What's your name?")) {
									talk(out);
								}
							}
						}
					} else if (command.startsWith("associate ")) {
						String faceID = command.split(" ")[1];
						String userName = command.substring(10 + faceID.length() + 1);
						saveFaceName(faceID, userName);
						String recognized = "Good, " + userName + ", see you next time!";
						talk(recognized);
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

	private Thread autoRunThread;
	private class AutoActionListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			if (!autorun.isRunning()) {
				autoRunThread = new Thread(autorun);
				autoRunThread.start();
			} else {
				auto.setSelected(false);
				autorun.stop();
				try {
					autoRunThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected List<Label> recognizeLabels(Image image, int threshold) {
		List<Label> labels = rekognition.detectLabels(image);
		StringBuffer labelsText = new StringBuffer("In this image I can recognize: ");
		for (Label l : labels) {
			if (l.getConfidence().intValue() > threshold) {
				labelsText.append(l.getName()).append(", ");
			}
		}
		talk(labelsText.toString());
		return labels;
	}

	protected List<FaceDetail> recognizeFaces(Image image, int threshold) {
		List<FaceDetail> faces = rekognition.detectFaces(image);
		StringBuffer facesText = new StringBuffer("In this photo I can recognize ");
		for (FaceDetail f : faces) {
			facesText.append(" a face with: ");
			if (f.getBeard().getConfidence().intValue() > threshold && f.getBeard().isValue()) {
				facesText.append("beard, ");
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
			List<Emotion> emotions = f.getEmotions();
			for (Emotion e : emotions) {
				if (e.getConfidence().intValue() > threshold) {
					facesText.append(e.getType()).append(", ");
				}
			}
			facesText.append(". ");
		}
		if (faces.size() > 0) {
			talk(facesText.toString());
		}
		return faces;
	}

	protected Map<String, String> recognizeFace(Image image) {
		List<FaceMatch> matches = rekognition.searchFacesByImage(collectionFaces, image);
		Map<String, Integer> occurrencies = new HashMap<String, Integer>();
		for (FaceMatch fm : matches) {
			String fId = getFaceID(fm.getFace());
			if (s3.exist(facesBucket, fId)) {
				String faceName = null;
				S3Object s3Obj = s3.get(facesBucket, fId);
				try {
					faceName = IOUtils.readFully(s3Obj.getObjectContent()).toString();
				} catch (IOException e) {
					e.printStackTrace(System.err);
				}
				Integer occ = occurrencies.get(faceName);
				if (occ == null) {
					occurrencies.put(faceName, 1);
				} else {
					occurrencies.put(faceName, occ.intValue() + 1);
				}
			}
		}
		String faceId = "";
		String faceName = "";
		List<FaceRecord> faceRecords = rekognition.indexFaces(collectionFaces, image);
		for (FaceRecord f : faceRecords) {
			faceId = getFaceID(f.getFace());
			break; //TODO: Handle more faces
		}
		if (!occurrencies.isEmpty()) {
			int occ = 0;
			for (String key : occurrencies.keySet()) {
				if (occurrencies.get(key).intValue() > occ) {
					faceName = key;
				}
			}
			saveFaceName(faceId, faceName);
		}
		Map<String, String> result = new HashMap<String, String>();
		result.put("faceId", faceId);
		if (faceName.length() > 0) {
			result.put("faceName", faceName);
		}
		return result;
	}

	protected InputStream talk(String text) {
		return talk(text, true);
	}

	private InputStream talk(String text, boolean play) {
		appendLog("Send text to Polly: " + text);
		InputStream tts = polly.tts(text);
		audioUtils.saveAudio("fromPolly.wav", tts);
		if (play) {
			audioUtils.playAudio("fromPolly.wav");
		}
		return tts;
	}

	protected Image takePicture() {
		String fileName = "capture.png";
		camUtils.capture(fileName);
		Image image = new Image();
		try {
			image.setBytes(ByteBuffer.wrap(Files.readAllBytes(Paths.get(fileName))));
			BufferedImage bufImg = ImageIO.read(new File(fileName));
			snapshot.setIcon(new ImageIcon(bufImg));
		} catch (IOException e1) {
			e1.printStackTrace(System.err);
		}
		return image;
	}

	protected void saveFaceName(String faceId, String faceName) {
		try {
			s3.put(new StringInputStream(faceName), facesBucket, faceId);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace(System.err);
		}
	}

	private String getFaceID(Face face) {
		return face.getFaceId().replaceAll("-", "_");
	}

	protected AudioRecorder startAudioRecording() {
		appendLog("Start Audio Recording...");
		recorder = audioUtils.startRecording(mixer);
		return recorder;
	}

	protected AudioInputStream stopAudioRecording() {
		recorder.stop();
		while (!recorder.isDone()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException err) {
				appendLog("ERROR: " + err.getMessage());
			}
		}
		AudioInputStream audioIS = recorder.getAudioInputStream();
		appendLog("Audio Recording Complete! " + recorder.getDuration());
		recorder = null;
		return audioIS;
	}

}
