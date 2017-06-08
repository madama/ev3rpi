package org.danysoft.ev3rpi;

import java.util.List;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class SQSWrapper {

	private AmazonSQS sqs;
	private String queueUrl;

	public SQSWrapper(AmazonSQS sqs, String queue) {
		this.sqs = sqs;
		this.queueUrl = getQueueUrl(queue);
	}

	public String getQueueUrl(String queueName) {
		return sqs.getQueueUrl(new GetQueueUrlRequest(queueName)).getQueueUrl();
	}

	public List<Message> readMessages() {
		ReceiveMessageRequest req = new ReceiveMessageRequest(this.queueUrl);
		req.setWaitTimeSeconds(20);
		return sqs.receiveMessage(req).getMessages();
	}

	public void deleteMessage(String receipt) {
		DeleteMessageRequest req = new DeleteMessageRequest().withQueueUrl(this.queueUrl).withReceiptHandle(receipt);
		sqs.deleteMessage(req);
	}

}
