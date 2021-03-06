package org.danysoft.ev3rpi;

import java.io.InputStream;

import com.amazonaws.services.lexruntime.AmazonLexRuntime;
import com.amazonaws.services.lexruntime.model.PostContentRequest;
import com.amazonaws.services.lexruntime.model.PostContentResult;
import com.amazonaws.services.lexruntime.model.PostTextRequest;
import com.amazonaws.services.lexruntime.model.PostTextResult;

public class LexWrapper {

	private AmazonLexRuntime lex;
	private String botName;
	private String botAlias;
	private String user;

	public LexWrapper(AmazonLexRuntime lex, String botName, String botAlias, String user) {
		this.lex = lex;
		this.botName = botName;
		this.botAlias = botAlias;
		this.user = user;
	}

	public String sendAudio(InputStream audio) {
		PostContentRequest req = new PostContentRequest();
		req.setBotName(botName);
		req.setBotAlias(botAlias);
		req.setContentType("audio/l16; rate=16000; channels=1");
		req.setInputStream(audio);
		req.setUserId(user);
		req.setAccept("text/plain; charset=utf-8");
		PostContentResult res = lex.postContent(req);
		System.out.println("Lex transcriptedText: " + res.getInputTranscript());
		return res.getMessage();
	}

	public String sendText(String text) {
		PostTextRequest req = new PostTextRequest();
		req.setBotName(botName);
		req.setBotAlias(botAlias);
		req.setInputText(text);
		req.setUserId(user);
		PostTextResult res = lex.postText(req);
		//System.out.println("Lex response: " + res.getMessage());
		return res.getMessage();
	}

	public String getTranscriptedText(InputStream audio) {
		PostContentRequest req = new PostContentRequest();
		req.setBotName(botName);
		req.setBotAlias(botAlias);
		req.setContentType("audio/l16; rate=16000; channels=1");
		req.setInputStream(audio);
		req.setUserId(user);
		req.setAccept("text/plain; charset=utf-8");
		PostContentResult res = lex.postContent(req);
		return res.getInputTranscript();
	}

}
