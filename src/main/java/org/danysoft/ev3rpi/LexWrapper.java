package org.danysoft.ev3rpi;

import java.io.InputStream;

import com.amazonaws.services.lexrts.AmazonLexRuntimeClient;
import com.amazonaws.services.lexrts.model.PostContentRequest;
import com.amazonaws.services.lexrts.model.PostContentResult;

public class LexWrapper {

	private AmazonLexRuntimeClient lex;
	private String botName;
	private String botAlias;

	public LexWrapper(AmazonLexRuntimeClient lex, String botName, String botAlias) {
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
		System.out.println(res);
		return res.getMessage();
	}

}
