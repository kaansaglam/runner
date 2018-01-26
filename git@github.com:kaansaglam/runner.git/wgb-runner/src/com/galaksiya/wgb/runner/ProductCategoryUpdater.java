
package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.galaksiya.extractor.fashion.DefactoExtractor;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class ProductCategoryUpdater extends RunnerUtil {

	public static void main(String args[]) {
		new ProductCategoryUpdater().execute();
	}

	DefactoExtractor defacto = new DefactoExtractor(name, agent);

	private void execute() {
		List<String> categroyList = new ArrayList<>();
		categroyList.add("kız çocuk");
		categroyList.add("genç kız");
		categroyList.add("erkek çocuk");
		categroyList.add("genç erkek");
		categroyList.add("cocuk");
		categroyList.add("genç");
		categroyList.add("kız");
		categroyList.add("bebek");

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/defacto-24-kasım.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
				JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
				String id = allDoc.get("id").getAsString();
				String productCategory = allDoc.get(Tag.PRODUCT_CATEGORY.text()).getAsString();
				boolean update = false;
				for (String string : categroyList) {
					if (productCategory == null || productCategory.contains(string)) {
						update = true;
						break;
					}
				}
				if (update) {

					String tempid = id;
					if (!id.contains("https")) {
						id = id.replaceAll("http", "https");
					}
					// id="https://www.defacto.com.tr/en-us/barcelona-lisansli-genc-erkek-t-shirt-718683";

					String retrieveWithCurl = retrieveWithCurl(id);
					if (retrieveWithCurl != null && retrieveWithCurl.length() > 500) {
						Document doc = Jsoup.parse(retrieveWithCurl);
						String extractCategory = null;
//						extractCategory=defacto.extractCategory(doc);
						if (extractCategory != null) {
							descriptionBulkCreator(tempid, extractCategory);
							System.out.println(counter + " " + extractCategory + " " + id);
							counter++;
						}
					}
				if (counter % BULK_SIZE == 1 && counter > 1) {
					sendBulk();
				}
				}
			}
			sendBulk();
			System.out.println("total product size" + counter);
			logger.info("DONE!");
		} catch (

		Exception e1) {
			e1.printStackTrace();
		}
	}

	private String retrieveWithCurl(String uri) {
		String command = "curl " + uri;
		StringBuffer inputLine = new StringBuffer();
		Process curlProc;
		try {
			curlProc = Runtime.getRuntime().exec(command);
			try (BufferedReader input = new BufferedReader(new InputStreamReader(curlProc.getInputStream()))) {

				String tmp;
				while ((tmp = input.readLine()) != null) {
					inputLine.append(tmp);
				}
			} catch (Exception e) {
				String ptrn = "Failed to read file: {\"uri\": \"%s\"}";
				System.err.printf(String.format(ptrn, uri), e);
			}
		} catch (IOException e) {
			String ptrn = "Failed to handle unsuccessfull retrieve: {\"uri\": \"%s\"}";
			System.err.printf(String.format(ptrn, uri), e);
		}
		return inputLine.toString();

	}
}