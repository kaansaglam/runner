package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.lucene.queries.function.valuesource.SortedSetFieldSource;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.BasedOn;
import wgb.io.JsonFields;
import wgb.io.Tag;

public class secondaryIdAdder extends RunnerUtil {
	private static final Logger logger = LogManager.getLogger(secondaryIdAdder.class);

	public static void main(String args[]) {
		new secondaryIdAdder().execute();
	}

	private int counter = 0;
	int counter1 = 0;
	private static List<String> deleteList = new ArrayList<>();

	private void execute() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/zara-18-oc.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
					JsonObject _source = jsonObj.get("_source").getAsJsonObject();
					JsonObject allDoc = _source.get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();
					// String currentSecondary = allDoc.get("secondaryId").getAsString();
					String secondaryId = null;
					// secondaryId =
					// secondaryIdCreator(images.get(0).getAsJsonObject().get("uri").getAsString(),
					// id);

					// defacto
					// secondaryId = findWithPattern(id, "-([0-9]+$)", 1);
					// System.out.println(counter++ + " " + secondaryId + " " + id);
					// prepareLoadBulkObj(id, secondaryId);

					// zara
					if (images.size() > 0) {
						String imageUri = images.get(0).getAsJsonObject().get("uri").getAsString();
						String seconary = createSecondaryId(imageUri);
						if (seconary != null) {
							counter++;
							System.out.println(counter + " " + seconary);
							System.out.println(imageUri);
							System.out.println();
							prepareLoadBulkObj(id, seconary);
						}
					}
					// tommy
					// if (images.size() > 0) {
					// String imageUri = images.get(0).getAsJsonObject().get("uri").getAsString();
					// String seconary = createSecondaryIdTommy(imageUri);
					// counter++;
					// System.out.println(counter + " " + seconary);
					// if (seconary != null) {
					// prepareLoadBulkObj(id, seconary);
					// }
					// }

					// if (secondaryId != null && !secondaryId.isEmpty() && id != null &&
					// !id.isEmpty()) {
					// counter++;
					// if (deleteList.contains(secondaryId)) {
					// System.out.println(counter1++ + " DELETED : " + id + " " + currentSecondary);
					// prepateDeleteBulkObj(id);
					// } else {
					// prepareLoadBulkObj(id, secondaryId);
					// }
					// deleteList.add(secondaryId);
					// }
					if (counter % BULK_SIZE == 1 && counter > 1) {
						// sendBulk();
					}
				}
			}
			// sendBulk();
		} catch (IOException e) {
			System.out.println(e);
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private String createSecondaryIdTommy(String imageUri) {
		String secondaryId = null;
		secondaryId = findWithPattern(imageUri, "image\\/tommy\\/[0-9]+_(.*[0-9|A-Z]+)_[0-9|A-Z]+", 1);
		if (secondaryId == null) {

			secondaryId = findWithPattern(imageUri, "image\\/tommy\\/[zoom1_|0-9]+_(.*[0-9|A-Z]+)_[0-9|A-Z]+", 1);
		}
		return secondaryId;
	}

	private String secondaryIdCreator(String imageURIs, String trackedUri) {
		String secondaryId = null;
		secondaryId = findWithPattern(imageURIs, "-([0-9|A-Z|-]*)", 1);
		return secondaryId;
	}

	private String createSecondaryId(String secondaryId) {
		// http://static.zara.net/photos///2018/V/1/1/p/2214/301/077/7/w/560/2214301077_2_2_1.jpg
		String newSecondaryId = null;
		if (secondaryId != null) {
			// 2018/
			String yearStr = findWithPattern(secondaryId, "static\\.zara\\.net\\/photos\\/\\/(\\/[0-9]+)\\/", 1);
			// /7/w
			String expected = findWithPattern(secondaryId, "static.zara.net\\/photos.*\\/.*(\\/[0-9]+\\/[a-z])", 1);
			if (expected != null) {
				// http://static.zara.net/photos///2018/V/1/1/p/2214/301/077/w/560/2214301077_2_2_1.jpg
				newSecondaryId = secondaryId.replace(expected, "/w");
				// http://static.zara.net/photos///2018/V
				String prefixExpected = findWithPattern(secondaryId, ".*[0-9]+\\/[A-Z]+", 0);
				if (prefixExpected != null) {
					// /1/1/p/2214/301/077/w/560/2214301077_2_2_1.jpg
					newSecondaryId = newSecondaryId.replace(prefixExpected, "");
				}
				// _2_2_1.jpg
				String postfixExpected = findWithPattern(secondaryId, "_[0-9]*.*", 0);
				if (postfixExpected != null) {
					// /1/1/p/2214/301/077/w/560/2214301077
					newSecondaryId = newSecondaryId.replace(postfixExpected, "");
					newSecondaryId = yearStr + newSecondaryId;
				}
			}
		}
		return newSecondaryId;
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

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}