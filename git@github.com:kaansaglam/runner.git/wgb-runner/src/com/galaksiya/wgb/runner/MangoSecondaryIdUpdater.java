package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.JsonFields;

public class MangoSecondaryIdUpdater extends RunnerUtil {
	private static final Logger logger = LogManager.getLogger(MangoSecondaryIdUpdater.class);

	public static void main(String args[]) {
		new MangoSecondaryIdUpdater().execute();
	}

	private int counter = 0;
	int counterId = 0;
	int counter1 = 0;
	private static List<String> deleteList = new ArrayList<>();

	private void execute() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/mango-28-ara.json"))) {

			// create a map to store id and new secondary..
			read();

			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					JsonObject allDoc = new JsonParser().parse(line).getAsJsonObject().get("_source").getAsJsonObject()
							.get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					if (allDoc.has(JsonFields.secondaryId)) {

						String secondaryId = allDoc.get(JsonFields.secondaryId).getAsString();
						String colorFromSecodnary = findWithPattern(secondaryId, "\\/[0-9]+_([0-9]+)", 1);
						String colorFromId = findWithPattern(id, "\\?c=([0-9]+)", 1);
						String newSecondary = idSecondaryMap.get(id);
						String colorFromNewSecodnary = null;
						if (newSecondary != null && !newSecondary.isEmpty()) {
							colorFromNewSecodnary = findWithPattern(newSecondary, "\\/[0-9]+_([0-9]+)", 1);
						}

						if (colorFromSecodnary != null && colorFromNewSecodnary != null
								&& !colorFromNewSecodnary.equals(colorFromSecodnary)) {
							// System.out.println("\n\nnew sec before modify : " + newSecondary);
							newSecondary = newSecondary.replace("_" + colorFromNewSecodnary, "_" + colorFromSecodnary);
						}
						// System.out.println(id + " " + newSecondary);
						if (newSecondary != null && !newSecondary.isEmpty() && id != null && !id.isEmpty()) {
							newSecondary = newSecondary.substring(0, newSecondary.indexOf( "?ts="));
							System.out.println("mapte var : " + id + "    			  " + newSecondary);

							counter++;
							if (deleteList.contains(newSecondary)) {
								 System.out.println(counter1++ + " DELETED : "
								 + id + " " + newSecondary);
								 prepateDeleteBulkObj(id);
							} else {
								secondaryBulkCreator(id, newSecondary);
							}
							deleteList.add(newSecondary);
						} else {
							System.out.println("mapte yok : " +id);
						}
						if (counter % BULK_SIZE == 1 && counter > 1) {
							 sendBulk();
						}
					}
				}
			}
			 sendBulk();
		} catch (IOException e) {
			System.out.println(e);
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private String extractIdFromUrl(String trackedUri) {
		String prodId;
		if (trackedUri.contains("html")) {
			prodId = trackedUri.substring(trackedUri.lastIndexOf("_") + 1, trackedUri.indexOf(".html"));
		} else {
			prodId = trackedUri.substring(trackedUri.lastIndexOf("id=") + 3);
			if (prodId.contains("?")) {
				prodId = prodId.substring(0, prodId.indexOf("?"));
			}
		}
		return prodId;
	}

	HashMap<String, String> idSecondaryMap = new HashMap<>();

	public void read() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/newSecondary28.txt"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					String jsonStr = line.substring(line.indexOf("{\"uri"), line.length());
					JsonObject json = new JsonParser().parse(jsonStr).getAsJsonObject();
					String id = json.get("uri").getAsString();
					String secondaryId = json.get("secondary").getAsString();
					if (idSecondaryMap.get(id) == null) {
						idSecondaryMap.put(id, secondaryId);
					}
				}
			}
		} catch (Exception e1) {
			System.out.println(e1);
			e1.printStackTrace();
		}
	}
}