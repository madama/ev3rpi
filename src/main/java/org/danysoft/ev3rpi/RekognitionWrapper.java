package org.danysoft.ev3rpi;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;

public class RekognitionWrapper {

	private AmazonRekognition rekognition;

	public RekognitionWrapper(AmazonRekognition rekognition) {
		this.rekognition = rekognition;
		this.rekognition.setRegion(Region.getRegion(Regions.EU_WEST_1));
	}

	public List<Label> detectLabels(Image image) {
		DetectLabelsRequest req = new DetectLabelsRequest();
		req.setImage(image);
		DetectLabelsResult res = rekognition.detectLabels(req);
		return res.getLabels();
	}

	public List<FaceDetail> detectFaces(Image image) {
		DetectFacesRequest req = new DetectFacesRequest();
		req.setImage(image);
		List<String> attributes = new ArrayList<String>();
		attributes.add("ALL");
		req.setAttributes(attributes);
		DetectFacesResult res = rekognition.detectFaces(req);
		return res.getFaceDetails();
	}

}
