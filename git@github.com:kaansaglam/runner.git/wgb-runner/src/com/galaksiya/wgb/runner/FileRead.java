package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.galaksiya.util.FashionRegex;

public class FileRead {
	public static void main(String[] args) {
		new FileRead().read();
	}

	int counter = 0;

	public void read() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/zarauri.txt"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					// line = line.substring(line.indexOf("http"));
					isMultiProduct(line);
				}
			}
			System.exit(0);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	protected boolean isMultiProduct(String uri) {
		Boolean isMultiProduct = false;
		if (uri != null && !uri.isEmpty()) {
			String id = findWithPattern(uri, FashionRegex.ZARA_DESIRED_URI_END_REGEX, 0);

			System.out.println(counter++ + " " + id);
		}
		return isMultiProduct;
	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};
}
