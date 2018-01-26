package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.JsonFields;
import wgb.io.Tag;

public class ImageUpdater extends RunnerUtil {
	private static final Logger logger = LogManager.getLogger(ImageUpdater.class);

	public static void main(String args[]) {
		new ImageUpdater().execute();
	}

	private BulkRequestBuilder bulk = getESTransportClient().prepareBulk();

	private int counter = 0;
	private String path = "/home/galaksiya/mango-22-ara.json";

	private void execute() {
		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			logger.info("... STARTED ...");
			stream.forEach(line -> {
				try {
					JsonObject asJsonObject = new JsonParser().parse(line).getAsJsonObject().get("_source")
							.getAsJsonObject().get("allDocument").getAsJsonObject();

					String id = asJsonObject.get("id").getAsString();
					JsonArray newImagesArray = asJsonObject.get("images").getAsJsonArray();
					String asString = newImagesArray.get(0).getAsJsonObject().get(JsonFields.uri).getAsString();
					String secondary = asJsonObject.get(JsonFields.secondaryId).getAsString();
					imageProdIdComperator(secondary, asJsonObject.get(Tag.AREA_SERVED.text()).getAsJsonArray(),
							newImagesArray);

					if (counter % BULK_SIZE == 1 && counter > 1) {
						// sendBulk();
					}
				} catch (Exception e) {
					logger.error("could not load", e);
				}
			});

			// sendBulk();
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		System.out.println(counterUrl);
		logger.info("DONE!");
		System.exit(0);
	}

	int counterERROR = 0;
	int counterUrl = 0;

	private void imageProdIdComperator(String secondary, JsonArray areaServed, JsonArray newImagesArray) {

		for (JsonElement areaServedEl : areaServed) {
			counterUrl++;
			String id = areaServedEl.getAsJsonObject().get(Tag.URL.text()).getAsString();
			String prodId = null;
			// extract prodId from url;
			prodId = mangoProdIdExtractor(id);
			if (!secondary.toString().contains(prodId)) {
				System.err.println(counterERROR++ + "id images farklı : " + id + "   " + secondary);
				break;
			}
		}
	}

	private String mangoProdIdExtractor(String id) {
		String prodId;
		if (id.contains("html")) {
			prodId = id.substring(id.lastIndexOf("_") + 1, id.indexOf(".html"));
		} else {
			prodId = id.substring(id.lastIndexOf("id=") + 3);
			if (prodId.contains("?")) {
				prodId = prodId.substring(0, prodId.indexOf("?"));
			}
		}
		return prodId;
	}

}
