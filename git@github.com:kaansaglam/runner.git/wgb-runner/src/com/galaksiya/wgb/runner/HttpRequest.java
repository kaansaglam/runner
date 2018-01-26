package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.jena.atlas.json.io.parser.JSONParser;
import org.jboss.netty.util.internal.SystemPropertyUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class HttpRequest {

	private final String USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

	public static void main(String[] args) throws Exception {
		String uri = "https://www.farfetch.com/shopping/women/items.aspx";

		String uriArrayStr = "[\"https://www.zara.com/tr/en/embroidered-t-shirt-with-tulle-bow-p07134320.html?v1=5488913&v2=361513\"]";
		JsonArray uriArray = new JsonParser().parse(uriArrayStr).getAsJsonArray();

		HttpRequest http = new HttpRequest();
		// String page = http.sendPost(uri);
		for (JsonElement mangoUri : uriArray) {
			try {
				String sendGet = http.sendGet(mangoUri.getAsString());

			} catch (Exception e) {
				System.err.println("HATA OLUSTU" + e);
			}
		}

	}

	int a = 0;

	// HTTP GET request
	private String sendGet(String url) throws Exception {
		a++;
		if (a % 1000 == 1) {
			System.out.println("xxxx  : " + a);
		}
		// if (!url.contains("https")) {
		// url = url.replace("http", "https");
		// }
		url = url.trim();
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.getFollowRedirects();
		// optional default is GET
		con.setRequestMethod("GET");
		// add request header
		// con.setRequestProperty("User-Agent", USER_AGENT);

		// System.out.println("\nSending 'GET' request to URL : " + url);
		// System.out.println("waiting to page...");
		Thread.sleep(500);
		int responseCode = con.getResponseCode();
		// yönlendirme varsa gittigi url
		URL redirectURL = con.getURL();

		Map<String, List<String>> responseMessage = con.getHeaderFields();
		System.out.println(responseCode);
		// System.out.println(responseMessage);

		// if (!url.equals(redirectURL.toString())) {
		// counter++;
		// System.out.println("Count : " + counter + " Response Code : " +
		// responseCode + "\n" + url);
		// System.out.println(redirectURL + "\n");
		// }
		// if (responseCode != 200) {
		// counter++;
		// System.err.println("Count : " + counter + " Response Code : " +
		// responseCode + " uri " + url);
		// } else {
		// System.out.println(("Response Code : " + responseCode + " uri " +
		// url));
		// }
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		Document doc = Jsoup.parse(response.toString());
		doc.outputSettings().escapeMode(EscapeMode.xhtml);

		Elements select = doc.select("li[class*=_category-link-wrapper");
		for (Element element : select) {
			if (element.attr("data-categoryid").equals("853133") || element.attr("data-categoryid").equals("436363")) {
				Elements select2 = element.select("ul[class*=subcategories").select("li[class*=_category-link-wrapper")
						.select("ul[class*=subcategories").select("li[class*=_category-link-wrapper");
				System.out.println(select2.size());
				for (Element element2 : select2) {
					String string = element2.select("a").attr("href");
					String attr = element2.select("a").attr("data-extraquery");
					System.out.println("\"" + string + "?" + attr + "\",");
				}

			}
		}

		// in.close();
		// fileWriter.close();

		return null;

	}

	int counter = 0;

	private String sendPost(String url) throws Exception {

		URL obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String urlParameters = "'{\"settings\":{\"analysis\":{\"filter\":{\"custom_english_stemmer\":{\"type\":\"stemmer\",\"name\":\"english\"}},\"analyzer\":{\"custom_lowercase_stemmed\":{\"tokenizer\":\"standard\",\"filter\":[\"lowercase\",\"custom_english_stemmer\"]}}}},\"mappings\":{\"product\":{\"properties\":{\"identifier\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\"secondaryId\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\"allDocument\":{\"properties\":{\"description\":{\"type\":\"string\",\"analyzer\":\"custom_lowercase_stemmed\"},\"title\":{\"type\":\"string\",\"analyzer\":\"custom_lowercase_stemmed\"},\"brand\":{\"type\":\"string\",\"analyzer\":\"custom_lowercase_stemmed\"},\"productCategory\":{\"type\":\"string\",\"analyzer\":\"custom_lowercase_stemmed\"},\"color\":{\"type\":\"string\",\"analyzer\":\"custom_lowercase_stemmed\"},\"offers\":{\"properties\":{\"validFrom\":{\"type\":\"date\",\"format\":\"dd.MM.yyyy HH:mm\"}}}}},\"datePublished\":{\"type\":\"date\",\"format\":\"dd.MM.yyyy HH:mm\"},\"lastSeen\":{\"type\":\"date\",\"format\":\"dd.MM.yyyy HH:mm\"},\"online\":{\"type\":\"boolean\"},\"onSale\":{\"type\":\"boolean\"},\"inStock\":{\"type\":\"boolean\"},\"categories\":{\"properties\":{\"identifier\":{\"type\":\"string\",\"index\":\"not_analyzed\"}}},\"basedOn\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\"brand\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\"estags\":{\"properties\":{\"identifier\":{\"type\":\"string\",\"index\":\"not_analyzed\"}}}}}}}'";

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		return response.toString();

	}

}
