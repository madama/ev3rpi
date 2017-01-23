package org.danysoft.ev3rpi;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.amazonaws.services.rekognition.model.DeleteCollectionRequest;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.SearchFacesByImageRequest;
import com.amazonaws.services.rekognition.model.SearchFacesByImageResult;

public class RekognitionWrapper {

	private AmazonRekognition rekognition;

	public RekognitionWrapper(AmazonRekognition rekognition) {
		this.rekognition = rekognition;
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

	public String createCollection(String collection) {
		CreateCollectionRequest req = new CreateCollectionRequest().withCollectionId(collection);
		CreateCollectionResult res = rekognition.createCollection(req);
		return res.getCollectionArn();
	}

	public void deleteCollection(String collection) {
		DeleteCollectionRequest req = new DeleteCollectionRequest().withCollectionId(collection);
		rekognition.deleteCollection(req);
	}

	public List<FaceRecord> indexFaces(String collection, Image image) {
		IndexFacesRequest req = new IndexFacesRequest();
		req.withImage(image);
		req.withCollectionId(collection);
		req.withDetectionAttributes("ALL");
		IndexFacesResult res = rekognition.indexFaces(req);
		return res.getFaceRecords();
	}

	public List<FaceMatch> searchFacesByImage(String collection, Image image) {
		SearchFacesByImageRequest req = new SearchFacesByImageRequest();
		req.withImage(image);
		req.withCollectionId(collection);
		SearchFacesByImageResult res = rekognition.searchFacesByImage(req);
		return res.getFaceMatches();
	}

}
