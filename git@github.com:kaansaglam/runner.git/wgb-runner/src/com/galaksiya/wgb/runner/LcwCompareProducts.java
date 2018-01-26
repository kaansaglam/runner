package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class LcwCompareProducts {
	private static final Logger logger = LogManager.getLogger(LcwCompareProducts.class);

	public static void main(String args[]) {
		new LcwCompareProducts().execute();
	}

	private int counter = 0;

	private void execute() {

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/lcw-20-ara.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {

					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
					JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();

					if (allDoc.has(Tag.AREA_SERVED.text())) {
						// counter++;
						JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();
						compareProductUri(id, images.toString(), areaServedArr);
					}
				}
			}
			System.out.println("total product size" + counter);
			logger.info("DONE!");
			System.exit(0);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	int counter1 = 0;
	List<String> uriList = new ArrayList<>();

	private void compareProductUri(String prodId, String imagesArray, JsonArray areaServedArr) {
		try {

			// String identifierProdNo = findIdentifier(prodId);
			// if (!imagesArray.contains(identifierProdNo)) {
			// System.out.println("\"" + prodId + "\",");
			// }

			for (JsonElement areaServedEl : areaServedArr) {
				JsonObject areaServed = areaServedEl.getAsJsonObject();

				JsonElement value = areaServed.get(Tag.URL.text());
				String url = value.getAsString();
				if (!uriList.contains(url)) {
					uriList.add(url);
				} else {
					System.out.println(counter++ + "DUPLİCATE : " + url);
				}
				// String areaServedProdNo = findIdentifier(url);
				// if (!prodId.contains(areaServedProdNo) &&
				// areaServedArr.size() > 1) {
				// System.out.println(counter1++ + prodId + " \"" + url +
				// "\",");
				// }
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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
			prodNo = findWithPattern(prodId, "[a-z|\\/|A-Z]+([0-9]+)", 1);
		}
		return prodNo;
	}
}