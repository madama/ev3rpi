package org.danysoft.ev3rpi;

import java.io.InputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;

public class S3Wrapper {

	private AmazonS3 s3;

	public S3Wrapper(AmazonS3 s3) {
		this.s3 = s3;
	}

	public boolean exist(String bucket, String key) {
		return s3.doesObjectExist(bucket, key);
	}

	public void put(InputStream is, String bucket, String key) {
		s3.putObject(bucket, key, is, null);
	}

	public S3Object get(String bucket, String key) {
		return s3.getObject(bucket, key);
	}

	public void delete(String bucket, String key) {
		s3.deleteObject(bucket, key);
	}

}
