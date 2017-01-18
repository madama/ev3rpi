package org.danysoft.ev3rpi;

import java.io.InputStream;

import com.amazonaws.services.lexrts.AmazonLexRuntime;
import com.amazonaws.services.lexrts.model.PostContentRequest;
import com.amazonaws.services.lexrts.model.PostContentResult;

public class LexWrapper {

	private AmazonLexRuntime lex;
	private String botName;
	private String botAlias;

	public LexWrapper(AmazonLexRuntime lex, String botName, String botAlias) {
		this.lex = lex;
		this.botName = botName;
		this.botAlias = botAlias;
	}

	public String sendAudio(InputStream audio) {
		PostContentRequest req = new PostContentRequest();
		req.setBotName(botName);
		req.setBotAlias(botAlias);
		req.setContentType("audio/l16; rate=16000; channels=1");
		req.setInputStream(audio);
		req.setUserId("ev3rpi");
		req.setAccept("text/plain; charset=utf-8");
		PostContentResult res = lex.postContent(req);
		System.out.println("Lex response: " + res);
		return res.getMessage();
	}

}
