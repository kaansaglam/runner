package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CityTracker {

	public static void main(String[] args) throws Exception {
		FileWriter fileWriter = new FileWriter("/home/galaksiya/yeni_address/city-country.txt");
		FileWriter fileWriter1 = new FileWriter("/home/galaksiya/yeni_address/country-district.txt");
		FileWriter fileWriter2 = new FileWriter("/home/galaksiya/yeni_address/district-apartment.txt");
		FileWriter fileWriter3 = new FileWriter("/home/galaksiya/yeni_address/addressCsv.txt");

		String uri = "http://www.superonline.net/location/city.json";
		String countryUri = "http://www.superonline.net/location/county.json?cityId=";
		String districtUri = "http://www.superonline.net/location/district.json?countyId=";
		String apartmentUri = "http://www.superonline.net/location/apartment.json";
		CityTracker http = new CityTracker();

		// get city list
		String page = http.sendGet(uri);
		if (isSuccess(page)) {
			JsonArray cityArr = new JsonParser().parse(page).getAsJsonArray();
			for (JsonElement city : cityArr) {
				JsonObject cityObj = city.getAsJsonObject();
				int cityId = cityObj.get("ID").getAsInt();

				String cityName = cityObj.get("ADI").getAsString();
				// get country list
				String country = http.sendGet(countryUri + cityId);
				if (isSuccess(country)) {
					JsonArray countryArr = new JsonParser().parse(country).getAsJsonArray();
					for (JsonElement countryElement : countryArr) {
						JsonObject countryObj = countryElement.getAsJsonObject();
						int countryId = countryObj.get("ID").getAsInt();
						String countryName = countryObj.get("ADI").getAsString();
						// write..
						write(fileWriter, cityName, countryName);
						// get district list
						String district = http.sendGet(districtUri + countryId);
						if (isSuccess(district)) {
							JsonArray districtArr = new JsonParser().parse(district).getAsJsonArray();
							for (JsonElement districtElement : districtArr) {
								JsonObject districtObj = districtElement.getAsJsonObject();
								String districtName = districtObj.get("ADI").getAsString();
								// write file..
								write(fileWriter1, countryName, districtName);
								String apartment = http.sendGet(apartmentUri + "?cityName=" + cityName + "&countyName="
										+ countryName + "&districtName=" + districtName);
								if (isSuccess(apartment)) {
									JsonArray aparmentArr = new JsonParser().parse(apartment).getAsJsonArray();
									if (isSuccess(aparmentArr.toString())) {
										for (JsonElement apartmentElement : aparmentArr) {
											JsonObject apartmentObj = apartmentElement.getAsJsonObject();
											String apartmentName = apartmentObj.get("ADI").getAsString();
											write(fileWriter2, districtName, apartmentName);
											write(fileWriter3, cityName,
													countryName + "," + districtName + "," + apartmentName);
										}
									} else {
										write(fileWriter3, cityName, countryName + "," + districtName);
									}
								}
							}
						}

					}
				}

			}
		}
		fileWriter.close();
		fileWriter1.close();
		fileWriter2.close();
		fileWriter3.close();

	}

	private static void write(FileWriter fileWriter, String str1, String str2) throws IOException {
		fileWriter.write(str1 + "," + str2);
		fileWriter.write("\n");
		fileWriter.flush();
	}

	private static boolean isSuccess(String country) {
		return country != null && country.length() > 10;
	}

	// HTTP GE request
	private String sendGet(String url) throws Exception {

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");
		Thread.sleep(100);
		int responseCode = con.getResponseCode();
		System.out.println("Response Code : " + responseCode + " Uri : " + url);
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		FileWriter fileWriter = new FileWriter("/home/galaksiya/hm-size.txt");
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
			fileWriter.write(inputLine);
			fileWriter.write("\n");
		}
		in.close();
		fileWriter.close();

		return response.toString();

	}
}
