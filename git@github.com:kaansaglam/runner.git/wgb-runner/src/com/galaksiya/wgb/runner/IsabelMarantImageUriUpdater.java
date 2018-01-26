package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.JsonFields;

public class IsabelMarantImageUriUpdater {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(IsabelMarantImageUriUpdater.class);

	public static void main(String args[]) {
		new IsabelMarantImageUriUpdater().execute();
	}

	private static TransportClient esTransportClient;

	private static TransportClient getESTransportClient() {
		if (esTransportClient == null) {
			esTransportClient = TransportClient.builder().build();
			ElasticSearchConfig config = new ElasticSearchConfig();
			int port = config.getPort();
			String[] elasticSearchHosts = config.getHosts().split(",");
			for (String host : elasticSearchHosts) {
				try {
					esTransportClient
							.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		return esTransportClient;
	}

	private BulkRequestBuilder bulk = getESTransportClient().prepareBulk();

	private int counter = 0;

	private static final String IMAGE_URI_REGEX = ".*jpg";

	private void execute() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/isabel.json"))) {
			stream.forEach(line -> {
				try {
					JsonObject allDocument = extractAllDocumentObj(line);
					String prodId = extractProdId(allDocument);
					JsonArray images = extractImages(allDocument);
					JsonArray newImages = correctImages(images);
					prepareLoadBulkObj(prodId, newImages);
					if (counter % BULK_SIZE == 1 && counter > 1) {
//						System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
						// bulk.get();
						// bulk = getESTransportClient().prepareBulk();
						Thread.sleep(1000);
					}
				} catch (Exception e) {
					logger.error("could not load", e);
				}
			});
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			// bulk.get();
			// bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private JsonArray correctImages(JsonArray images) {
		if (images != null && !images.isJsonNull() && images.size() > 0) {
			for (JsonElement imageEl : images) {
				JsonObject imageObj = imageEl.getAsJsonObject();
				JsonElement uriEl = imageObj.get("uri");
				String name = imageObj.get("name").getAsString();
				System.out.println(name);
				if (uriEl != null && !uriEl.isJsonNull()) {
					String uri = uriEl.getAsString();
					String newUri = findWithPattern(uri, IMAGE_URI_REGEX, 0);
					imageObj.addProperty("uri", newUri);
				}
			}
		}
		return images;
	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

	private JsonArray extractImages(JsonObject allDocumentObj) {
		JsonArray images = new JsonArray();
		if (allDocumentObj != null && !allDocumentObj.isJsonNull()) {
			JsonElement imagesElement = allDocumentObj.get("images");
			if (imagesElement != null && !imagesElement.isJsonNull()) {
				images = imagesElement.getAsJsonArray();
			}
		}
		return images;
	}

	private String extractProdId(JsonObject allDocument) {
		return allDocument.get("id").getAsString();
	}

	private JsonObject extractAllDocumentObj(String line) {
		JsonObject allDocumentObj = new JsonObject();
		if (line != null && !line.isEmpty()) {
			JsonObject productObj = new JsonParser().parse(line).getAsJsonObject();
			if (productObj != null && !productObj.isJsonNull()) {
				JsonElement sourceEl = productObj.get("_source");
				JsonObject sourceObj = sourceEl.getAsJsonObject();
				allDocumentObj = sourceObj.get("allDocument").getAsJsonObject();
			}
		}
		return allDocumentObj;
	}

	private void prepareLoadBulkObj(String prodId, JsonArray newImagesArray) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT)
					.field(ElasticSearchClient.PROPERTY_BROKEN_IMAGE, true);
			content.startArray("images");
			for (JsonElement imageUriEl : newImagesArray) {
				String imageUri = imageUriEl.getAsJsonObject().get(JsonFields.uri).getAsString();
				String imageName = imageUriEl.getAsJsonObject().get(JsonFields.name).getAsString();
				JsonObject sizeObject = imageUriEl.getAsJsonObject().get("sizeObject").getAsJsonObject();
				content.startObject().field("name", imageName).field("uri", imageUri).startObject("sizeObject")
						.field("width", "2000").field("height", "2500").endObject().endObject();
			}
			content.endArray();
			content.endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
//			System.out.println(content.string());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}