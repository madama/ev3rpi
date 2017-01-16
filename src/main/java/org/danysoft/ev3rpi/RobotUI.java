package org.danysoft.ev3rpi;

import java.io.FileInputStream;
import java.util.Properties;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.BasicWindow;
import com.googlecode.lanterna.gui2.Button;
import com.googlecode.lanterna.gui2.DefaultWindowManager;
import com.googlecode.lanterna.gui2.EmptySpace;
import com.googlecode.lanterna.gui2.GridLayout;
import com.googlecode.lanterna.gui2.Label;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.gui2.Panel;
import com.googlecode.lanterna.gui2.TextBox;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

public class RobotUI {

	public static void main(String[] args) {
		RobotUI ui = new RobotUI();
	}

	private String propertiesPath = "ev3rpi.properties";
	private Properties properties = new Properties();
	private OpenCVUtils ocv;
	private CamUtils cam;

	public RobotUI() {
		try {
			init();

			// Setup terminal and screen layers
			Terminal terminal = new DefaultTerminalFactory().createTerminal();
			Screen screen = new TerminalScreen(terminal);
			screen.startScreen();

			// Create panel to hold components
			Panel panel = new Panel();
			panel.setLayoutManager(new GridLayout(2));

			panel.addComponent(new Label("Forename"));
			panel.addComponent(new TextBox());

			panel.addComponent(new Label("Surname"));
			panel.addComponent(new TextBox());

			panel.addComponent(new EmptySpace(new TerminalSize(0, 0))); // Empty
																		// space
																		// underneath
																		// labels
			panel.addComponent(new Button("Submit"));

			// Create window to hold the panel
			BasicWindow window = new BasicWindow();
			window.setComponent(panel);

			// Create gui and start gui
			MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(),
					new EmptySpace(TextColor.ANSI.BLUE));
			gui.addWindowAndWait(window);
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	private void init() throws Exception {
		ocv = new OpenCVUtils();
		cam = new CamUtils();
		properties.load(new FileInputStream(propertiesPath));
		if (properties.containsKey("webcam.skipframes")) {
			cam.setSkipFrames(Integer.valueOf(properties.getProperty("webcam.skipframes")));
		}
	}

}
