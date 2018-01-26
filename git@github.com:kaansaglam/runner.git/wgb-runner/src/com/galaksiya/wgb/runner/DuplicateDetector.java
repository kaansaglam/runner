package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import com.galaksiya.agent.Agent;
import com.galaksiya.extractor.fashion.HMExtractor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class DuplicateDetector extends RunnerUtil {
	public static void main(String args[]) {
		new DuplicateDetector().readFile();
	}

	private int counter1 = 0;
	List<String> resultList = new ArrayList<>();

	private void readFile() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/hm-24-kasım.json"))) {
			stream.forEach(line -> {
				JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
				JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
				JsonElement areaServedEl = allDoc.get("areaServed");

				JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();
				String id = compareId(line);
				if (id != null && !id.isEmpty()) {
					try {
						System.out.println(counter + " " + id);
						String secondaryFromImages = findIdFromImages(images, id);
						if (secondaryFromImages != null && !secondaryFromImages.isEmpty()) {
							System.out.println(secondaryFromImages + "  from images " + id);
							secondaryBulkCreator(id, secondaryFromImages);
							counter++;
						} else {
							String secondaryId = createSecondaryId(id);
							System.out.println(secondaryId + "  " + id);
							if (secondaryId != null && !secondaryId.isEmpty()) {
								secondaryBulkCreator(id, secondaryId);
								counter++;
							}
						}

					} catch (Exception e) {
						System.out.println(e);
					}
				}
				if (counter % BULK_SIZE == 1 && counter > 1 && bulk.numberOfActions() > 0) {
					System.out.println("SENDED BULK : " + counter);
					// sendBulk();
				}
			});
			// sendBulk();
			System.out.println("total duplicate : " + counter1);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private String findIdFromImages(JsonArray images, String id) {
		String secondary = null;
		for (JsonElement imagesElement : images) {
			String imageUrl = imagesElement.getAsJsonObject().get("uri").getAsString();
			String[] split = imageUrl.split(" ");
			if (split.length > 2) {
				// System.out.println(split[1] + " " + id + " " + counter1++);
				secondary = split[1] + split[2];
				if (secondary != null) {
					break;
				}
			}
		}
		return secondary;
	}

	private String name;
	private Agent agent;
	HMExtractor hmExtractor = new HMExtractor(name, agent);

	private String createSecondaryId(String id) throws IOException, InterruptedException {
		Document doc = sendGET(id);
		if (doc != null) {
			String secondaryId = null;
//			secondaryId = hmExtractor.secondaryIdCreator(id, doc);
			return secondaryId;
		} else {
			System.out.println("bos sayfa  : " + id);
		}
		return null;
	}

	private List<String> urlList = new ArrayList<String>();
	private HashMap<String, JsonObject> duplicateMap = new HashMap<>();

	private String compareId(String line) {
		String expected = null;
		String secondaryId = null;
		String id = null;
		String tmpId = null;
		String urlFromAreaServed = null;
		String tmpUrl = null;
		String imageUri = null;
		JsonObject productObj = new JsonParser().parse(line).getAsJsonObject();
		JsonElement sourceEl = productObj.get("_source");
		JsonObject sourceObj = sourceEl.getAsJsonObject();
		JsonObject allDocumentObj = sourceObj.get("allDocument").getAsJsonObject();
		// for secondaryId
		JsonElement secondaryIdEl = allDocumentObj.get("secondaryId");
		if (secondaryIdEl != null && !secondaryIdEl.isJsonNull()) {
			secondaryId = secondaryIdEl.getAsString();
		}
		// // for id
		JsonElement idEl = allDocumentObj.get("id");
		if (idEl != null && !idEl.isJsonNull()) {
			id = idEl.getAsString();
			if (!id.contains(".html")) {
				tmpId = id.substring(id.indexOf("?article="));
			}
		}
		// for url
		JsonElement areaServedEl = allDocumentObj.get("areaServed");
		// if (areaServedEl != null && !areaServedEl.isJsonNull()) {
		// JsonElement urlEl =
		// areaServedEl.getAsJsonArray().get(0).getAsJsonObject().get("url");
		// if (urlEl != null && !urlEl.isJsonNull()) {
		// urlFromAreaServed = urlEl.getAsString();
		// if (urlFromAreaServed.contains("?article=")) {
		// tmpUrl =
		// urlFromAreaServed.substring(urlFromAreaServed.indexOf("?article="));
		// } else {
		// tmpUrl = urlFromAreaServed;
		// }
		// }
		// }
		// // for image uri
		JsonElement imagesEl = allDocumentObj.get("images");
		if (imagesEl != null && !imagesEl.isJsonNull()) {
			JsonArray imagesArray = imagesEl.getAsJsonArray();
			JsonObject imageObj = new JsonObject();
			if (imagesArray.size() > 1) {
				imageObj = imagesArray.get(1).getAsJsonObject();
			} else {
				imageObj = imagesArray.get(0).getAsJsonObject();
			}
			if (imageObj != null && !imageObj.isJsonNull()) {
				JsonElement imageUriEl = imageObj.get("uri");
				if (imageUriEl != null && !imageUriEl.isJsonNull()) {
					imageUri = imageUriEl.getAsString();
					imageUri = imageUri.replace(" ", "");
				}
			}
			imageUri = imagesArray.toString().replaceAll(" ", "");
		}
		JsonArray areaServed = areaServedEl.getAsJsonArray();
		// for (JsonElement countryEl : areaServed) {
		// String url =
		// countryEl.getAsJsonObject().get(Tag.URL.text()).getAsString();
		// if (!urlList.contains(url)) {
		// urlList.add(url);
		//
		// } else {
		// System.out.println(counter++ + " " + url);
		//
		// // return url;
		//
		// // if (url.contains("html") && !url.contains(secondaryId)) {
		// // System.out.println(counter++ + " " + url);
		// // }
		// // String duplicateSecondary = duplicateMap.get(url);
		// //
		// // if (!url.contains(duplicateSecondary) &&
		// // !url.contains(secondaryId)) {
		// // System.out.println(counter++ + " " + url);
		// // }
		// }
		// }

		// if (id.contains(".html") || urlFromAreaServed.contains(".html")) {
		// if (urlFromAreaServed.contains(secondaryId) ||
		// id.contains(secondaryId)) {
		// expected = secondaryId + " | " + id + " | " + urlFromAreaServed;
		// }
		// } else {
		if (!imageUri.contains(secondaryId) && !imageUri.contains("hmgoepprod")) {
			// if (!imageUri.contains(secondaryId)) {

			System.out.println(counter++ + " " + id);
			// return id;
		}
		// }
		return null;
	}

	private static Document sendGET(String url) throws IOException, InterruptedException {
		url = url.trim();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// System.out.println("\nSending 'GET' request to URL : " + url);
		// System.out.println("waiting to page...");
		Thread.sleep(1000);
		int responseCode = con.getResponseCode();
		// yönlendirme varsa gittigi url
		URL redirectURL = con.getURL();

		StringBuffer response = new StringBuffer();
		if (url.contains("hm.com") && !redirectURL.toString().contains("index.html")) {

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			if (response.toString().contains("Ürün bulunamadı") || response.toString().contains("No product found")
					|| response.toString().contains("No items found")) {
				return null;
			}
			in.close();

		} else {
			return null;
		}

		Document doc = null;
		if (response != null) {
			doc = Jsoup.parse(response.toString());
			doc.outputSettings().escapeMode(EscapeMode.xhtml);
		}
		// in.close();
		// fileWriter.close();

		return doc;

	}
}