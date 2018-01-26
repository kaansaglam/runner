package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.riot.SysRIOT;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.galaksiya.agent.vocabulary.JsonProperties;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class HmProductUriComperator {
	private static final Logger logger = LogManager.getLogger(HmProductUriComperator.class);

	public static void main(String args[]) {
		new HmProductUriComperator().execute();
	}

	private int counter = 0;
	private int counter2 = 0;

	private void execute() {

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/hm-20-ekim.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {

					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();

					JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();

					JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();
					// if (date.before(today)) {
					counter++;
					if (allDoc.has(Tag.AREA_SERVED.text())) {
						JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();

						findeSecondary(id, areaServedArr, images);
					}
					// }
				}
			}
			//
			System.out.println("total product size" + counter);

			logger.info("DONE!");
			System.exit(0);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	HashMap<String, JsonArray> addressCountryWithSecondary = new HashMap<>();
	int counter1 = 0;
	// http://www.hm.com/cy/product/79963?article=79963-A

	private String findeSecondary(String prodId, JsonArray areaServedArr, JsonArray images) {
		List<String> prodNo = new ArrayList<>();
		HashMap<String, Integer> prodMap = new HashMap<>();
		String secondary = null;

		String idFormImages = findIdFromImages(images, prodId);
		String tempProdId = null;
		String prodIdNo = null;
		if (prodId.contains("www2")) {
			prodIdNo = findIdentifier(prodId);
			// prodIdNo = prodIdNo.substring(0, prodIdNo.length() - 3);
			tempProdId = prodIdNo;
			prodMap.put(prodIdNo, 1);
		} else {
			// prodIdNo = findIdentifier2(prodId);
			prodIdNo = idFormImages;
			prodMap.put(prodIdNo, 1);
		}
		prodNo.add(prodIdNo);
		JsonArray wrongAdressCountry = new JsonArray();
		for (JsonElement areaServedEl : areaServedArr) {
			JsonObject areaServed = areaServedEl.getAsJsonObject();
			if (areaServed != null && areaServed.has(Tag.URL.text())) {
				JsonElement value = areaServed.get(Tag.URL.text());
				String countryUrl = value.getAsString();
				String areaServedProdNo = null;
				if (countryUrl.contains("www2")) {
					if (idFormImages != null && !countryUrl.contains(idFormImages)) {
						if (addressCountryWithSecondary.get(idFormImages) != null) {
							addressCountryWithSecondary.get(idFormImages).getAsJsonArray().add(areaServedEl);
						} else {
							wrongAdressCountry.add(areaServedEl);
							addressCountryWithSecondary.put(idFormImages, wrongAdressCountry);
						}
						System.out.println(counter1++ + " " + prodId + "  " + idFormImages);
					}
					areaServedProdNo = findIdentifier(countryUrl);
					// areaServedProdNo = areaServedProdNo.substring(0,
					// areaServedProdNo.length() - 3);
					// if (tempProdId != null &&
					// !areaServedProdNo.equals(prodIdNo)) {
					// System.out.println(counter1++ + prodId);
					// System.err.println(tempProdId + " " + areaServedProdNo);
					// }
				} else {
					// areaServedProdNo = findIdentifier2(countryUrl);
					areaServedProdNo = idFormImages;

				}

				if (!prodNo.contains(areaServedProdNo)) {
					prodNo.add(areaServedProdNo);
				}
				if (prodMap.get(areaServedProdNo) != null) {
					Integer prodNoCount = prodMap.get(areaServedProdNo);
					prodMap.put(areaServedProdNo, prodNoCount + 1);
				} else {
					prodMap.put(areaServedProdNo, 1);
				}
			}
		}
		if (prodNo.size() > 2) {
			for (JsonElement areaServedEl : areaServedArr) {
				JsonObject areaServed = areaServedEl.getAsJsonObject();
				if (areaServed != null && areaServed.has(Tag.URL.text())) {
					JsonElement value = areaServed.get(Tag.URL.text());
					String countryUrl = value.getAsString();
					if (!countryUrl.contains("www2")) {
						// System.out.println("\"" + countryUrl + "\",");
					}
				}
			}

			// System.err.println(counter1++ + prodId + " " + idFormImages + " "
			// + prodNo);
		}
		return secondary;
	}

	private String findIdFromImages(JsonArray images, String id) {
		String secondary = null;
		for (JsonElement imagesElement : images) {
			String imageUrl = imagesElement.getAsJsonObject().get("uri").getAsString();
			String[] split = imageUrl.split(" ");
			if (split.length > 2) {
				secondary = split[1] + split[2];
				break;
			}
		}
		return secondary;
	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

	private String findIdentifier(String prodId) {
		String prodNo = null;
		if (prodId != null) {
			prodNo = findWithPattern(prodId, "productpage.([0-9]+)", 1);
		}
		return prodNo;
	}

	private String findIdentifier2(String prodId) {
		String prodNo = null;
		if (prodId != null) {
			prodNo = findWithPattern(prodId, "product\\/([0-9]+)\\?article", 1);
		}
		return prodNo;
	}

	private final String USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

	private String sendGet(String url) throws Exception {
		if (url.contains("zara") && !url.contains("https")) {
			url = url.replace("http", "https");
		}
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		System.out.println("çekiyorum " + url);
		// optional default is GET
		con.setRequestMethod("GET");
		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setConnectTimeout(1000);

		int responseCode = con.getResponseCode();
		// yönlendirme varsa gittigi url
		URL redirectURL = con.getURL();

		StringBuffer response = new StringBuffer();
		if (url.contains("hm.com")) {

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			if (response.toString().contains("Ürün bulunamadı")) {
				System.out.println(url + " " + responseCode + "  " + redirectURL);
			}

		}
		return response.toString();
	}

}