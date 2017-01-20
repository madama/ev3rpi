package org.danysoft.ev3rpi;

import org.opencv.core.Mat;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class CamUtils {

	private VideoCapture cam;
	private int skipFrames = 0;

	public CamUtils() {
		cam = new VideoCapture(0);
		while (!cam.isOpened()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean capture(String file) {
		Mat frame = new Mat();
		cam.open(0);
		if (skipFrames != 0) {
			for (int i=0; i<skipFrames; i++) {
				cam.grab();
			}
		}
		cam.read(frame);
		cam.release();
		return Highgui.imwrite(file, frame);
	}

	public void setSkipFrames(int skipFrames) {
		this.skipFrames = skipFrames;
	}

}
