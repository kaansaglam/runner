package com.galaksiya.wgb.runner;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

public class UrlEncoder {
	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		
		
		CSVReader csvReader = new CSVReader(new FileReader("/home/galaksiya/database.csv"), '~');
		FileWriter fileWriter = new FileWriter("/home/galaksiya/links.txt");
		List<String[]> readAll = csvReader.readAll();
		System.err.println("Size: " + readAll.size());
		for (String[] link : readAll) {
			if (link[0].startsWith("http")) {
				String newLink = URLEncoder.encode(link[0], "UTF-8");
				System.err.println(newLink);
				fileWriter.write(newLink);
				fileWriter.write("\n");
				fileWriter.flush();
			}
		}
		csvReader.close();
		fileWriter.close();
	}

}
