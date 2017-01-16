package org.danysoft.ev3rpi;

import java.io.FileInputStream;
import java.util.Properties;

import jcurses.event.WindowEvent;
import jcurses.event.WindowListener;
import jcurses.widgets.BorderPanel;
import jcurses.widgets.GridLayoutManager;
import jcurses.widgets.Window;

public class RobotUI extends Window implements WindowListener {

	public static void main(String[] args) {
		Window ui = new RobotUI(28, 20);
		ui.addListener((WindowListener) ui);
		ui.show();
	}

	private String propertiesPath = "ev3rpi.properties";
	private Properties properties = new Properties();
	private OpenCVUtils ocv;
	private CamUtils cam;

	public RobotUI(int width, int height) {
		super(width, height, true, "EV3 RPI");
		try {
			init();
		} catch (Exception e) {
			e.printStackTrace();
		}
		BorderPanel bp = new BorderPanel();
		GridLayoutManager manager = new GridLayoutManager(2, 5);
		bp.setLayoutManager(manager);
	}

	private void init() throws Exception {
		ocv = new OpenCVUtils();
		cam = new CamUtils();
		properties.load(new FileInputStream(propertiesPath));
		if (properties.containsKey("webcam.skipframes")) {
			cam.setSkipFrames(Integer.valueOf(properties.getProperty("webcam.skipframes")));
		}
	}

	@Override
	public void windowChanged(WindowEvent event) {
		if (event.getType() == WindowEvent.CLOSING) {
			event.getSourceWindow().close();
		}
	}

}
