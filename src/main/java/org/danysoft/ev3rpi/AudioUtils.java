package org.danysoft.ev3rpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


public class AudioUtils {

	public AudioInputStream recordAudio(long time) {
		AudioRecorder rec = new AudioRecorder(getAudioFormat());
		rec.start();
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		rec.stop();
		while (!rec.isDone()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return rec.getAudioInputStream();
	}

	public AudioRecorder startRecording() {
		AudioRecorder rec = new AudioRecorder(getAudioFormat());
		rec.start();
		return rec;
	}

	public AudioRecorder startRecording(Mixer mixer) {
		AudioRecorder rec = new AudioRecorder(mixer, getAudioFormat());
		rec.start();
		return rec;
	}

	public void playAudio(String fileName) {
		try {
			Process p = Runtime.getRuntime().exec("aplay -t raw -c 1 -r 16000 -f S16_LE " + fileName);
			p.waitFor();
			//playAudio(new FileInputStream(fileName));
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void playAudio(InputStream is) {
		try {
			AudioFormat af = getAudioFormat();
			DataLine.Info info = new DataLine.Info(SourceDataLine.class, af);
			SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
			line.open(af);
			line.start();
			int nBytesRead = 0;
			byte[] abData = new byte[128000];
			while (nBytesRead != -1) {
				try {
					nBytesRead = is.read(abData, 0, abData.length);
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (nBytesRead >= 0) {
					line.write(abData, 0, nBytesRead);
				}
			}
		} catch (Exception e) {
			System.err.println("Cannot play audio file! " + e.getMessage());
		}
	}

	public void printMixerInfo() {
		try {
			Mixer.Info[] mi = AudioSystem.getMixerInfo();
			for (Mixer.Info info : mi) {
				System.out.println("Info: " + info);
				Mixer mix = AudioSystem.getMixer(info);
				System.out.println("Mixer: " + mix);
				Line.Info[] sl = mix.getSourceLineInfo();
				for (Line.Info info2 : sl) {
					System.out.println("    info: " + info2);
					Line line = AudioSystem.getLine(info2);
					if (line instanceof SourceDataLine) {
						SourceDataLine source = (SourceDataLine) line;

						DataLine.Info i = (DataLine.Info) source.getLineInfo();
						for (AudioFormat format : i.getFormats()) {
							System.out.println("    format: " + format);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public void saveAudio(String file, InputStream audio) {
		try {
			File targetFile = new File(file);
			java.nio.file.Files.copy(audio, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			if (audio.markSupported()) {
				audio.reset();
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	public AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return format;
	}

	public class AudioRecorder implements Runnable {
		// record microphone && generate stream/byte array
		private AudioInputStream audioInputStream;
		private AudioFormat format;
		public TargetDataLine line;
		public Thread thread;
		private double duration;
		private boolean done;

		public AudioRecorder(AudioFormat format) {
			super();
			this.format = format;
		}

		public AudioRecorder(Mixer mixer, AudioFormat format) {
			super();
			this.format = format;
			this.line = getTargetDataLineForRecord(mixer);
		}

		public void start() {
			thread = new Thread(this);
			thread.setName("Capture");
			thread.start();
			done = false;
		}

		public void stop() {
			thread = null;
		}

		public void run() {
			duration = 0;
			if (line == null) {
				line = getTargetDataLineForRecord();
			}
			//System.out.println("Using line for recording: " + line.getLineInfo());
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			final int frameSizeInBytes = format.getFrameSize();
			final int bufferLengthInFrames = line.getBufferSize() / 8;
			final int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
			final byte[] data = new byte[bufferLengthInBytes];
			int numBytesRead;
			line.start();
			while (thread != null) {
				if ((numBytesRead = line.read(data, 0, bufferLengthInBytes)) == -1) {
					break;
				}
				out.write(data, 0, numBytesRead);
			}
			// we reached the end of the stream. stop and close the line.
			line.stop();
			line.close();
			line = null;
			// stop and close the output stream
			try {
				out.flush();
				out.close();
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
			// load bytes into the audio input stream for playback
			final byte audioBytes[] = out.toByteArray();
			final ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
			audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);
			final long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format.getFrameRate());
			duration = milliseconds / 1000.0;
			try {
				audioInputStream.reset();
			} catch (final Exception ex) {
				ex.printStackTrace();
				return;
			}
			done = true;
		}

		private TargetDataLine getTargetDataLineForRecord() {
			TargetDataLine line;
			final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			if (!AudioSystem.isLineSupported(info)) {
				return null;
			}
			// get and open the target data line for capture.
			try {
				line = (TargetDataLine) AudioSystem.getLine(info);
				line.open(format, line.getBufferSize());
			} catch (final Exception ex) {
				return null;
			}
			//Line.Info[] infos = AudioSystem.getTargetLineInfo(info);
			//for (Line.Info i : infos){
			//	System.out.println("USING LINE: " + i.toString());
			//}
			return line;
		}

		private TargetDataLine getTargetDataLineForRecord(Mixer mixer) {
			TargetDataLine line;
			final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
			if (!AudioSystem.isLineSupported(info)) {
				return null;
			}
			// get and open the target data line for capture.
			try {
				line = (TargetDataLine) mixer.getLine(info);
				line.open(format, line.getBufferSize());
			} catch (final Exception ex) {
				return null;
			}
			//Line.Info[] infos = AudioSystem.getTargetLineInfo(info);
			//for (Line.Info i : infos){
			//	System.out.println("USING LINE: " + i.toString());
			//}
			return line;
		}

		public AudioInputStream getAudioInputStream() {
			return audioInputStream;
		}

		public boolean isDone() {
			return done;
		}

		public AudioFormat getFormat() {
			return format;
		}

		public void setFormat(AudioFormat format) {
			this.format = format;
		}

		public Thread getThread() {
			return thread;
		}

		public double getDuration() {
			return duration;
		}

	}

}
