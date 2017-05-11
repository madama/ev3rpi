package org.danysoft.ev3rpi;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.sound.sampled.AudioInputStream;

import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;

public class AutoRun implements Runnable {

	private boolean isRunning = false;
	private RobotUI ui;

	public AutoRun(RobotUI rui) {
		ui = rui;
	}

	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void run() {
		isRunning = true;
		while (isRunning) {
			try {
				// RANDOM MOVEMENT
				Random generator = new Random();
				int rand = generator.nextInt(2);
				if (rand < 1) {
					ui.ev3DevUtils.execCommand("turn_left");
				} else {
					ui.ev3DevUtils.execCommand("turn_right");
				}
				rand = generator.nextInt(3);
				if (rand < 1) {
					ui.ev3DevUtils.execCommand("walk");
				} else if (rand < 2) {
					ui.ev3DevUtils.execCommand("run");
				}
				// TAKE PICTURE
				Image image = ui.takePicture();
				// LABELS
				ui.recognizeLabels(image, 50);
				// FACES
				List<FaceDetail> faces = ui.recognizeFaces(image, 50);
				// IF SINGLE FACE TRY TO RECOGNIZE NAME
				if (faces.size() == 1) {
					Map<String, String> face = ui.recognizeFace(image);
					if (face.containsKey("faceName")) {
						String recognized = "Hi " + face.get("faceName") + "!";
						ui.talk(recognized);
					} else {
						ui.talk("What's your name?");
						ui.startAudioRecording();
						Thread.sleep(3000);
						AudioInputStream audio = ui.stopAudioRecording();
						String name = ui.lex.getTranscriptedText(audio);
						ui.saveFaceName(face.get("faceId"), name);
						String recognized = "Good, " + name + ", see you next time!";
						ui.talk(recognized);
					}
				}
				rand = generator.nextInt(3);
				if (rand < 1) {
					ui.ev3DevUtils.execCommand("shake");
				}
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		isRunning = false;
	}

}
