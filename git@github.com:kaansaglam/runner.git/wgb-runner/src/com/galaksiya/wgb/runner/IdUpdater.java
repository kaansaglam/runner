package com.galaksiya.wgb.runner;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.galaksiya.util.FashionRegex;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class IdUpdater {
	public static void main(String args[]) {
		new IdUpdater().readFile();
	}

	private int counter;
	private int countryCount = 0;

	private void readFile() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/mango-24-kasım.json"))) {
			stream.forEach(line -> {
				try {
					JsonObject obj = new JsonParser().parse(line).getAsJsonObject();
					String id = obj.get("_id").getAsString();
					// if (id.length() > 200) {
					JsonArray asJsonArray = obj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject()
							.get(Tag.AREA_SERVED.text()).getAsJsonArray();
					for (JsonElement jsonElement : asJsonArray) {
						JsonArray asJsonObject = jsonElement.getAsJsonObject().get(Tag.OFFERS.text()).getAsJsonArray();
						if (asJsonObject.size() > 4) {
							countryCount++;
							System.out.println(countryCount + "  " + id + "  " + asJsonObject);
						}
					}
					// System.out.println(asJsonArray);
					// System.out.println(id.substring(0, id.indexOf("html")
					// + 4));
					// System.out.println("id " + id + "\n\n\n");

					// }

					// counter++;
					// updateId(line);
				} catch (Exception e) {
				}
			});
			// fileWriter("/home/galaksiya/updated-jack-jones.json");
		} catch (

		IOException e) {
		}
	}

	private JsonArray newProduct = new JsonArray();

	private void updateId(String line) {
		if (line != null) {
			JsonObject obj = new JsonParser().parse(line).getAsJsonObject();
			String id = obj.get("_id").getAsString();
			String newId = cleanId(id);
			obj.addProperty("_id", newId);
			obj.get("_source").getAsJsonObject().addProperty("identifier", newId);
			obj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject().addProperty("id", newId);
			newProduct.add(obj);
		}

	}

	private String cleanId(String id) {
		Pattern pattern = Pattern.compile(FashionRegex.JACK_AND_JONES_PRODUCT_NO_REGEX);
		Matcher matcher = pattern.matcher(id);
		String newUri = null;
		if (matcher.find()) {
			String productNumber = matcher.group(0);
			newUri = ("http://www.jackjones.com/gb/en/" + productNumber);
		}
		return newUri;
	}

	private void fileWriter(String fileName) {

		try (FileWriter writer = new FileWriter(fileName);) {
			if (newProduct != null) {
				for (JsonElement jsonElement : newProduct) {
					if (jsonElement != null && jsonElement.isJsonObject()) {
						writer.append(jsonElement.toString());
						writer.append("\n");
					}
				}
			}
		} catch (IOException e) {
		}

	}
}
