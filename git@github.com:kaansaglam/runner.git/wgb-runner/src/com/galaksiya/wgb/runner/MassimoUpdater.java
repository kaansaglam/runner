package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
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
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.board.JsonProperties;
import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.JsonFields;
import wgb.io.Tag;

public class MassimoUpdater {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(MassimoUpdater.class);

	public static void main(String args[]) {
		new MassimoUpdater().execute();
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

	private void execute() {

		String path = "/home/galaksiya/massimodutti-prod.csv";

		try (Stream<String> stream = Files.lines(Paths.get(path), Charset.forName("ISO_8859_1"))) {
			logger.info("... STARTED ...");
			try {
				stream.forEach(line -> {

					counter++;
					String[] fieldList = line.split("~");
					String id = fieldList[0];
					String oldCategory = fieldList[1];
					int nameSizeInt = 10;
					JsonArray newImagesArray = new JsonArray();

					for (int i = 2; i < fieldList.length - nameSizeInt; i++) {
						JsonObject newImageUriObject = new JsonObject();
						String imageUri = fieldList[i + nameSizeInt];
						if (imageUri != null && !imageUri.isEmpty()) {
							if (imageUri != null && imageUri.contains("2.jpg")) {
								imageUri = imageUri.replace("2.jpg", "14.jpg");
								if (imageUri.contains("?timesstamp")) {
									imageUri = imageUri.substring(0, imageUri.indexOf("?timesstamp"));
								}
							}
							newImageUriObject.addProperty(JsonProperties.URI, imageUri);

							newImageUriObject.addProperty(JsonProperties.NAME, fieldList[i]);
							newImagesArray.add(newImageUriObject);
						}
					}

					String updatedCategory = createNewCategory(id, oldCategory);

					prepareLoadBulkObj(id, updatedCategory, newImagesArray);
					System.out.println(updatedCategory);
					System.out.println(id);
					System.out.println(newImagesArray);

					if (counter % BULK_SIZE == 1 && counter > 1) {
						System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
						bulk.get();
						bulk = getESTransportClient().prepareBulk();
						try {
							Thread.sleep(10000);
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

	private void prepareLoadBulkObjImg(String prodId, JsonArray newImagesArray) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			content.startArray("images");
			for (JsonElement jsonElement : newImagesArray) {
				content.startObject().field("name", jsonElement.getAsJsonObject().get(JsonFields.name).getAsString())
						.field("uri", jsonElement.getAsJsonObject().get(JsonFields.uri).getAsString())
						.startObject("sizeObject").field("width", 722).field("height", 963).endObject().endObject();
			}
			content.endArray();
			content.endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void prepareLoadBulkObj(String prodId, String categoryStr, JsonArray imageArray) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			if (categoryStr != null && !categoryStr.isEmpty()) {
				content.field(Tag.PRODUCT_CATEGORY.text(), categoryStr);
			}

			content.startArray("images");
			for (JsonElement jsonElement : imageArray) {
				content.startObject().field("name", jsonElement.getAsJsonObject().get(JsonFields.name).getAsString())
						.field("uri", jsonElement.getAsJsonObject().get(JsonFields.uri).getAsString())
						.startObject("sizeObject").field("width", 722).field("height", 963).endObject().endObject();
			}
			content.endArray();
			content.field(ElasticSearchClient.PROPERTY_BROKEN_IMAGE, true);
			String imageUri = imageArray.get(0).getAsJsonObject().get("uri").getAsString();
			content.field(ElasticSearchClient.PROPERTY_SECONDARY, imageUri).endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);

			XContentBuilder content4 = XContentFactory.jsonBuilder().startObject()
					.field(ElasticSearchClient.PROPERTY_SECONDARY, imageUri).endObject();

			UpdateRequestBuilder updateReq4 = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content4);

			bulk.add(updateReq4);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected String findCategoryId(String trackedUri, String pattern) {
		String id = null;
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(trackedUri);
		if (m.find()) {
			id = m.group(0);
			id = id.substring(1);
			return id;
		}
		return null;
	}

	private String createNewCategory(String id, String oldCategory) {
		id = findCategoryId(id, "p[0-9]+(?=$|.html)");
		String newUri = "https://www.massimodutti.com/itxrest/2/catalog/store/34009450/30359464/category/0/product/"
				+ id + "/detail?languageId=-1&appId=1";

		String page;
		String newCategroy = "";
		try {

			page = sendGet(newUri);
			if (page != null && !page.isEmpty()) {
				JsonObject pageObj = new JsonParser().parse(page).getAsJsonObject();
				if (pageObj.get("isBuyable").getAsBoolean()) {

					JsonArray categoryArray = pageObj.get("relatedCategories").getAsJsonArray();
					for (JsonElement jsonElement : categoryArray) {
						String genderStr = jsonElement.getAsJsonObject().get("identifier").getAsString();
						if (genderStr.contains("_MEN_")) {
							newCategroy = "men ";
						} else if (genderStr.contains("_WOMEN_")) {
							newCategroy = "women ";
						} else if (genderStr.contains("_GIRLS_")) {
							newCategroy = "girl kids ";
						} else if (genderStr.contains("_BOYS_")) {
							newCategroy = "boy kids ";
						}
					}
					if (oldCategory.contains("/")) {
						String[] split = oldCategory.split("/");
						oldCategory = oldCategory.replace(split[0], "");
						oldCategory = oldCategory.replace(split[1], "");
						newCategroy = newCategroy + " " + oldCategory;
					}
					newCategroy = newCategroy + pageObj.get("productType").getAsString();
				} else {
					return null;
				}
			}

		} catch (

		Exception e) {
			System.out.println(e);
		}
		return newCategroy;
	}

	private final String USER_AGENT = "Mozilla/5.0";

	private String sendGet(String url) throws Exception {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return response.toString();

	}

}