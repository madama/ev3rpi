package org.danysoft.ev3rpi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;


public class AudioUtils {

	public AudioInputStream recordAudio() {
		System.out.println("Start audio recording...");
		AudioRecorder rec = new AudioRecorder(getAudioFormat());
		rec.start();
		try {
			Thread.sleep(5000);
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
		System.out.println("Audio recorded!");
		return rec.getAudioInputStream();
	}

	public void playAudio(AudioInputStream audio) {
		try {
			System.out.println("Start audio playing...");
			Clip player = AudioSystem.getClip();
			player.open(audio);
			player.start();
			while (!player.isRunning()) {
				Thread.sleep(10);
			}
			player.close();
			System.out.println("Audio played!");
		} catch (Exception e) {
			System.err.println("Cannot play audio file!");
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
			audio.reset();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	private AudioFormat getAudioFormat() {
		float sampleRate = 16000;
		int sampleSizeInBits = 16;
		int channels = 1;
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
		return format;
	}

	private class AudioRecorder implements Runnable {
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
			line = getTargetDataLineForRecord();
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
			System.out.println("Audio time: " + duration);
			try {
				audioInputStream.reset();
				System.out.println("resetting...");
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
