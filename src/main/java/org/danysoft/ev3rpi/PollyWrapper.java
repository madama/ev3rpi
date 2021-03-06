package org.danysoft.ev3rpi;

import java.io.InputStream;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.amazonaws.services.polly.model.VoiceId;

public class PollyWrapper {

	private AmazonPolly polly;
	private VoiceId voice;

	public PollyWrapper(AmazonPolly polly, VoiceId voice) {
		this.polly = polly;
		this.voice = voice;
	}

	public InputStream tts(String text) {
		SynthesizeSpeechRequest req = new SynthesizeSpeechRequest().withVoiceId(voice);
		req.setText(text);
		req.setOutputFormat(OutputFormat.Pcm);
		SynthesizeSpeechResult res = polly.synthesizeSpeech(req);
		System.out.println("Polly response: " + res);
		return res.getAudioStream();
	}

}
