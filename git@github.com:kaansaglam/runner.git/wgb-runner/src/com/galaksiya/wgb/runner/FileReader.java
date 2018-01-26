package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class FileReader {

	public static void main(String args[]) {

		try {
			new FileReader().addFiledFromExcel();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void addFiledFromExcel() throws FileNotFoundException, IOException, InterruptedException {
		BufferedReader br = null;
		List<String> duplicate = new ArrayList<>();
		String line;
		int count = 0;
		int countTotal = 0;
		br = new BufferedReader(new java.io.FileReader("/home/galaksiya/IncompatibleUrl.txt"));
		while ((line = br.readLine()) != null) {
			countTotal++;
			line = line.substring(line.indexOf("{\""));
			JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
			String identifier = jsonObject.get("identifier").getAsString();
			if (!duplicate.contains(identifier)) {
				duplicate.add(identifier);
				System.out.println(count++);
			} else {
				System.out.println(identifier);
			}
		}
		System.out.println("total count : " + countTotal);
		br.close();
	}

}