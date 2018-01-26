package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
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

public class Forever21SecondaryIdAdder {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(Forever21SecondaryIdAdder.class);

	public static void main(String args[]) {
		new Forever21SecondaryIdAdder().execute();
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
	int counter2 = 0;
	private int counterDeneme = 0;
	List<String> secondaryIdList = new ArrayList<>();

	private void execute() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/zara-28-ara.json"),
				Charset.forName("UTF-8"))) {
			logger.info("--- STARTED ---");
			try {
				stream.forEach(line -> {
					JsonObject productJsonObject = new JsonParser().parse(line).getAsJsonObject();
					JsonObject allDocumentObj = productJsonObject.get("_source").getAsJsonObject().get("allDocument")
							.getAsJsonObject();
					// get id
					String prodId = allDocumentObj.get("id").getAsString();
					// get secondaryId
					String secondaryId = extractFirstImageUri(allDocumentObj);
					String newSecondaryId = createNewSecondaryId(secondaryId);
					if (newSecondaryId != null && !newSecondaryId.isEmpty()) {
						if (!secondaryIdList.contains(newSecondaryId)) {
							secondaryIdList.add(newSecondaryId);
							counter++;
							prepareLoadBulkObj(prodId, newSecondaryId);
							// logger.error("secondaryId eklendi >> " + id);
						} else {
							prepateDeleteBulkObj(prodId);
							// logger.error("Uri silindi >> " + id);
							counter2++;
							System.err.println(counter2 + " Uri silindi >> " + prodId);
						}
					}
					if (counter % BULK_SIZE == 1 && counter > 1) {
						System.out.println("SENDED BULK:" + bulk.numberOfActions() + " total product : " + counter);
						bulk.get();
						bulk = getESTransportClient().prepareBulk();
						counter++;
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				logger.error("could not load", e);
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			bulk.get();
			bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private String createNewSecondaryId(String secondaryId) {
		if (secondaryId != null) {

			// http://static.zara.net/photos///2018/V/1/1/p/2214/301/077/7/w/560/2214301077_2_2_1.jpg
			String expected = findWithPattern(secondaryId, "static.zara.net\\/photos.*\\/.*(\\/[0-9]+\\/)[a-z]", 1);
			if (expected != null) {
				secondaryId = secondaryId.replace(expected, "/");
				// http://static.zara.net/photos///2018/V/1/1/p/2214/301/077/w/560/2214301077_2_2_1.jpg
				String prefixExpected = findWithPattern(secondaryId, ".*[0-9]+\\/[A-Z]+", 0);
				if (prefixExpected != null) {
					secondaryId = secondaryId.replace(prefixExpected, "");
				}
			}
		}
		return secondaryId;
	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

	private String extractFirstImageUri(JsonObject productJsonObject) {
		String firstImageUri = null;
		System.err.println(counterDeneme++);
		if (productJsonObject != null && !productJsonObject.isJsonNull()) {
			JsonElement imagesEl = productJsonObject.get("images");
			if (imagesEl != null && !imagesEl.isJsonNull()) {
				JsonArray imagesArray = imagesEl.getAsJsonArray();
				if (imagesArray.size() > 0) {

					JsonElement firstEl = imagesArray.get(0);
					if (firstEl != null && !firstEl.isJsonNull()) {
						JsonObject imagesObj = firstEl.getAsJsonObject();
						JsonElement uriEl = imagesObj.get("uri");
						if (uriEl != null && !uriEl.isJsonNull()) {
							firstImageUri = uriEl.getAsString();
						}
					}
				}
			}
		}
		return firstImageUri;
	}

	private void prepareLoadBulkObj(String prodId, String secondary) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.field(ElasticSearchClient.PROPERTY_SECONDARY, secondary);

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			content.field(ElasticSearchClient.PROPERTY_SECONDARY, secondary);

			content.endObject().endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
			System.out.println(prodId + " >> " + content.string());
		} catch (IOException e) {
			logger.error(e);
		}
	}

	private void prepateDeleteBulkObj(String id) {
		DeleteRequestBuilder deleteReq = getESTransportClient()
				.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id).setId(id);
		bulk.add(deleteReq);
	}

}
