package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
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

import riotcmd.trig;
import wgb.io.JsonFields;
import wgb.io.Tag;

public class AreaServedUnifier extends RunnerUtil {
	public static void main(String args[]) {
		new AreaServedUnifier().readFile();
	}

	private int counter1 = 0;
	List<String> resultList = new ArrayList<>();

	private void readFile() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/hm-24-kasım.json"))) {
			stream.forEach(line -> {
				JsonObject allDoc = new JsonParser().parse(line).getAsJsonObject().get("_source").getAsJsonObject()
						.get("allDocument").getAsJsonObject();
				JsonArray areaServed2 = allDoc.get("areaServed").getAsJsonArray();

				String duplicateId = compareId(line);
				if (duplicateId != null && !duplicateId.isEmpty()) {
					try { // ilk areaServed
						JsonObject countryObject = duplicateMap.get(duplicateId);
						String duplicateCounteryCode = countryObject.get(Tag.ADDRESS_COUNTRY.text()).getAsString();
						JsonArray areaServed1 = countryObject.get(Tag.AREA_SERVED.text()).getAsJsonArray();
						JsonObject firstAreaServed = findCountryObjWithCountry(duplicateCounteryCode, areaServed1);
						String firstProductId = countryObject.get(Tag.ID.text()).getAsString();
						String firstSecodnaryId = countryObject.get(JsonFields.secondaryId).getAsString();

						// ikinci areaServed
						JsonObject secondaAreaServed = findCountryObjWithCountry(duplicateCounteryCode, areaServed2);
						String secondaSecondaryId = allDoc.get(JsonFields.secondaryId).getAsString();
						String secodnaryProductId = allDoc.get(JsonFields.id).getAsString();
						JsonObject newCountryObj = createAreaServed(firstProductId, firstSecodnaryId, firstAreaServed,
								secodnaryProductId, secondaSecondaryId, secondaAreaServed);
						String id = null;
						// id belirleme..

						if (duplicateId.contains("html")) {
							if (duplicateId.contains(firstSecodnaryId)) {
								areaServed1.add(newCountryObj);
								id = firstProductId;
								areaServedBulkCreator(id, areaServed1);
								areaServedBulkCreator(secodnaryProductId, areaServed2);
								counter++;
								System.out.println(counter + "  " + duplicateId + "  \n\n");
							} else {
								areaServed2.add(newCountryObj);
								id = secodnaryProductId;
								areaServedBulkCreator(id, areaServed2);
								areaServedBulkCreator(firstProductId, areaServed1);
								counter++;
								System.out.println(counter + "  " + duplicateId + "  \n\n");
							}

							if (counter % BULK_SIZE == 1 && counter > 1) {
								sendBulk();
							}
						}

					} catch (Exception e) {
						System.out.println(e);
					}
				}
			});
			sendBulk();
			System.out.println("total duplicate : " + counter1);
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	private JsonObject createAreaServed(String firstProductId, String firstSecodnaryId, JsonObject firstAreaServed,
			String secodnaryProductId, String secondaSecondaryId, JsonObject secondaAreaServed) throws ParseException {
		JsonArray firstOffers = firstAreaServed.get(Tag.OFFERS.text()).getAsJsonArray();
		JsonArray secodOffers = secondaAreaServed.get(Tag.OFFERS.text()).getAsJsonArray();
		JsonArray currentOffers = new JsonArray();
		HashMap<Double, JsonObject> offerMap = new HashMap<>();
		offerUnifier(firstOffers, offerMap);
		offerUnifier(secodOffers, offerMap);

		Set<Double> keySet = offerMap.keySet();
		for (Double double1 : keySet) {
			JsonObject asJsonObject = offerMap.get(double1).getAsJsonObject();
			currentOffers.add(asJsonObject);
		}

		// offers eklendi
		firstAreaServed.add(Tag.OFFERS.text(), currentOffers);
		String secondLastSeen = secondaAreaServed.get(Tag.LASTSEEN.text()).getAsString();
		String firstLastSeen = firstAreaServed.get(Tag.LASTSEEN.text()).getAsString();
		if (dateComperator(firstLastSeen, secondLastSeen)) {
			firstAreaServed.addProperty(Tag.LASTSEEN.text(), secondLastSeen);
		} else {
			firstAreaServed.addProperty(Tag.LASTSEEN.text(), firstLastSeen);
		}
		return firstAreaServed;
	}

	private void offerUnifier(JsonArray firstOffers, HashMap<Double, JsonObject> offerMap) throws ParseException {
		for (JsonElement firstOffer : firstOffers) {
			Double firstValue = firstOffer.getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
			if (offerMap.get(firstValue) == null) {
				offerMap.put(firstValue, firstOffer.getAsJsonObject());
			} else {

				JsonObject mapJsonObj = offerMap.get(firstValue);
				if (!isOld(firstOffer.getAsJsonObject(), mapJsonObj.getAsJsonObject())) {
					offerMap.put(firstValue, firstOffer.getAsJsonObject());
				} else {
					offerMap.put(firstValue, mapJsonObj.getAsJsonObject());
				}
			}
		}
	}

	// is firs old..
	private Boolean isOld(JsonObject firstOffer, JsonObject secondOffer) throws ParseException {
		String firstDate = firstOffer.get(Tag.VALIDFROM.text()).getAsString();
		String secondDate = firstOffer.get(Tag.VALIDFROM.text()).getAsString();

		return dateComperator(firstDate, secondDate);

	}

	private boolean dateComperator(String firstDate, String secondDate) throws ParseException {
		DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
		Date fDate = format.parse(firstDate);
		Date sDate = format.parse(secondDate);
		if (fDate.before(sDate)) {
			return true;
		}
		return false;
	}

	private String findCountryCode(JsonObject countryObject) {
		String country = null;
		if (countryObject.has(Tag.ADDRESS_COUNTRY.text())) {
			country = countryObject.get(Tag.ADDRESS_COUNTRY.text()).getAsString();
		}
		return country;
	}

	protected JsonObject findCountryObjWithCountry(String countryAddress, JsonArray areaServedArr) {
		JsonObject desiredAreaServed = new JsonObject();
		for (JsonElement areaServedEl : areaServedArr) {
			JsonElement countryEl = areaServedEl.getAsJsonObject().get(Tag.ADDRESS_COUNTRY.text());
			if (countryEl != null && countryEl.getAsString().equals(countryAddress)) {
				desiredAreaServed = areaServedEl.getAsJsonObject();
				areaServedArr.remove(areaServedEl);
				break;
			}
		}
		return desiredAreaServed;
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
			String secondaryId = null ;
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
		try {

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
			// for id
			JsonElement idEl = allDocumentObj.get("id");
			if (idEl != null && !idEl.isJsonNull()) {
				id = idEl.getAsString();
				if (!id.contains(".html")) {
					tmpId = id.substring(id.indexOf("?article="));
				}
			}
			// for url
			JsonElement areaServedEl = allDocumentObj.get("areaServed");
			if (areaServedEl != null && !areaServedEl.isJsonNull()) {
				JsonElement urlEl = areaServedEl.getAsJsonArray().get(0).getAsJsonObject().get("url");
				if (urlEl != null && !urlEl.isJsonNull()) {
					urlFromAreaServed = urlEl.getAsString();
					if (urlFromAreaServed.contains("?article=")) {
						tmpUrl = urlFromAreaServed.substring(urlFromAreaServed.indexOf("?article="));
					} else {
						tmpUrl = urlFromAreaServed;
					}
				}
			}
			// for image uri
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
			for (JsonElement countryEl : areaServed) {
				String url = countryEl.getAsJsonObject().get(Tag.URL.text()).getAsString();
				if (!urlList.contains(url)) {
					urlList.add(url);
					JsonObject productInfo = new JsonObject();
					productInfo.addProperty(Tag.ID.text(), id);
					productInfo.addProperty(JsonFields.secondaryId, secondaryId);
					productInfo.add(Tag.AREA_SERVED.text(), areaServed);
					productInfo.addProperty(Tag.ADDRESS_COUNTRY.text(),
							countryEl.getAsJsonObject().get(Tag.ADDRESS_COUNTRY.text()).getAsString());
					duplicateMap.put(url, productInfo);

				} else {
					// if (url.contains("html")) {
					System.out.println(counter1++ + " " + url);
					return url;
					// }

					// if (url.contains("html") && !url.contains(secondaryId)) {
					// System.out.println(counter++ + " " + url);
					// }
					// String duplicateSecondary = duplicateMap.get(url);
					//
					// if (!url.contains(duplicateSecondary) &&
					// !url.contains(secondaryId)) {
					// System.out.println(counter++ + " " + url);
					// }
				}
			}

			// if (id.contains(".html") || urlFromAreaServed.contains(".html"))
			// {
			// if (urlFromAreaServed.contains(secondaryId) ||
			// id.contains(secondaryId)) {
			// expected = secondaryId + " | " + id + " | " + urlFromAreaServed;
			// }
			// } else {
			// if (!imageUri.contains(secondaryId) && !id.contains("html") &&
			// !imageUri.contains("hmgoepprod")) {
			// // if (!imageUri.contains(secondaryId)) {
			//
			// System.out.println(counter++ + " " + id);
			// // return id;
			// }
			// }
		} catch (Exception e) {
			System.out.println(e);
		}
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