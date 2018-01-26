package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import com.galaksiya.extractor.fashion.AbstractFashionExtractor;
import com.galaksiya.extractor.fashion.ImagelessProductException;
import com.google.gson.JsonObject;

public class FindDiff {
	public FindDiff() {
		System.out.println("Test.Test()");
	}

	static AbstractFashionExtractor a = new AbstractFashionExtractor(null, null) {

		@Override
		protected String extract(String pageContent, String trackedUri, Boolean isNewUri)
				throws ImagelessProductException {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public static void main(String[] args) throws Exception {
		createMap();
	}

	private static HashMap<String, String> createMap() throws FileNotFoundException, IOException, InterruptedException {
		BufferedReader br = null;
		String sCurrentLine;
		HashMap<String, String> prodIdMap = new HashMap<>();
		br = new BufferedReader(new FileReader("/home/galaksiya/zara-prodId.txt"));
		int counter = 1;
		while ((sCurrentLine = br.readLine()) != null) {
			String[] split = sCurrentLine.split("	");
			String duplicate = prodIdMap.get(split[1]);
			if (duplicate == null) {
				prodIdMap.put(split[1], split[0]);
//				 isFromES(split);
			} else {
				System.out.println(counter++);

			}
		}
		br.close();
		System.out.println(prodIdMap);
		return prodIdMap;
	}

//	private static void isFromES(String[] split) {
//		String id = split[1];
//		 id = id.replaceAll("/tr/", "/es/");
//		 JsonObject fromElasticSearch = a.getAreaServed(id, null);
//		 if (fromElasticSearch.isJsonNull() || fromElasticSearch ==
//		 null
//		 || fromElasticSearch.toString().length() < 5) {
//		 System.out.println(split[1]);
//		 }
//	}
}