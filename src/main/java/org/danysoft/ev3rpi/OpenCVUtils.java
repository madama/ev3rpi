package org.danysoft.ev3rpi;

import org.opencv.core.Core;

public class OpenCVUtils {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

}
