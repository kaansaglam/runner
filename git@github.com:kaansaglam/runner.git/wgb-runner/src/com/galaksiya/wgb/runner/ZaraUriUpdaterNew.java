package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.JsonFields;
import wgb.io.Tag;

public class ZaraUriUpdaterNew {
	// identifier and areaServed.url with only p info
	// içinde sadece p mi var onu kontrol ediyor.
	private static final String COLOR_ID_REGEX = "static.zara.net\\/photos.*\\/(.*)\\/[0-9]\\/[a-z]";
	private static final String URL = Tag.URL.text();
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(ZaraUriUpdaterNew.class);

	public static void main(String args[]) {
		new ZaraUriUpdaterNew().execute();
	}

	private static TransportClient esTransportClient;

	private static TransportClient getESTransportClient() {
		if (esTransportClient == null) {
			esTransportClient = TransportClient.builder().build();
			ElasticSearchConfig config = new ElasticSearchConfig();
			int port = config.getPort();
			String[] elasticSearchHosts = config.getHosts().split(",");
			for (String host : elasticSearchHosts) {
				try {
					esTransportClient
							.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		return esTransportClient;
	}

	private BulkRequestBuilder bulk = getESTransportClient().prepareBulk();

	private static int counter = 0;
	private static int counterdeneme = 0;
	private int counterSendGet = 0;

	private void execute() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/zara-28-ara.json"),
				Charset.forName("UTF-8"))) {
			try {
				stream.forEach(line -> {
					JsonObject allDocumentObj = extractAllDocumentObj(line);
					String prodId = createCanonicalUri(allDocumentObj.get("id").getAsString());
					JsonArray areaServedArray = allDocumentObj.get("areaServed").getAsJsonArray();
					String secondaryId = null;
					if (allDocumentObj.has(JsonFields.secondaryId)) {
						secondaryId = allDocumentObj.get("secondaryId").getAsString();
					}
					String desiredStr = isDesired(prodId, areaServedArray);
					if (desiredStr != null) {
//						sendBulk(prodId, areaServedArray, desiredStr);
					} else {
						counterSendGet++;
						String v1 = extract_v1_value(prodId, secondaryId);
						if (v1 != null) {
							v1 = "?v1=" + v1;
							sendBulk(prodId, areaServedArray, v1);
						}
					}
				});
			} catch (Exception e) {
				System.err.println(e);

				logger.error("could not load", e);
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			// bulk.get();
			// bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
			System.out.println("Kac send get yapıldı: " + counterSendGet);
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		System.exit(0);
	}

	private final String PRODUCT_URI_REGEX = "\\/en\\/(.*)-";
	private int counterColor;

	private String createCanonicalUri(String trackedUri) {
		String canonicalUri = null;
		// https://www.zara.com/tr/en/contrasting-piqu%C3%A9-polo-shirt-p08373401.html?v1=5361028&v2=493002
		// https://www.zara.com/tr/en/contrasting-piqu%C3%A9-polo-shirt-c12534785p08373401.html
		String expectedStr = findWithPattern(trackedUri, PRODUCT_URI_REGEX, 1);
		if (expectedStr != null) {
			// https://www.zara.com/tr/en/-p08373401.html?v1=5361028&v2=493002
			canonicalUri = trackedUri.replace(expectedStr, "");
		}
		return canonicalUri;
	}

	private String isDesired(String prodId, JsonArray areaServed) {
		String desired = null;
		try {
			desired = findWithPattern(prodId, "\\/en\\/-((c[0-9]+p[0-9]+.html)|(p[0-9]+.html\\?v1=[0-9]+))", 1);
			if (desired == null) {
				for (JsonElement areaServedEl : areaServed) {
					JsonObject areaServedObj = areaServedEl.getAsJsonObject();
					JsonElement urlEl = areaServedObj.get("url");
					if (urlEl != null && !urlEl.isJsonNull()) {
						String url = createCanonicalUri(urlEl.getAsString());
						desired = findWithPattern(url, "\\/en\\/-((c[0-9]+p[0-9]+.html)|(p[0-9]+.html\\?v1=[0-9]+))",
								1);
						break;
					}
				}
			}
		} catch (Exception e) {
			System.err.println(e + "  " + prodId);
		}
		return desired;
	}

	private void sendBulk(String prodId, JsonArray areaServed, String v1) {
		prepareLoadBulkObj(prodId, areaServed, v1);
		// if (counter % BULK_SIZE == 1 && counter > 1) {
		//// System.out.println("SENDED BULK:" + bulk.numberOfActions() + " total
		// product: " + counter);
		//// bulk.get();
		// bulk = getESTransportClient().prepareBulk();
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
	}

	private String extract_v1_value(String prodId, String secondaryId) {
		String colorId = findWithPattern(secondaryId, COLOR_ID_REGEX, 1);
		String v1_value = null;
		Document doc;
		try {
			doc = sendGet(prodId);
			if (doc != null) {
				String jsonObjStr = findWithPattern(doc.toString(),
						"(;window.zara.dataLayer = )(.*)(;window.zara.viewPayload = window.zara.dataLayer;)", 2);
				if (jsonObjStr != null) {
					JsonObject productJsonObj = new JsonParser().parse(jsonObjStr).getAsJsonObject();
					String color_id_from_doc = extractColorId(productJsonObj);
					if (color_id_from_doc != null && colorId != null
							&& (color_id_from_doc.contains(colorId) || colorId.contains(color_id_from_doc))) {
//						System.err.println(counterColor++ + "- coloId eşleşen uriler: " + prodId);
						JsonElement parentIdEl = productJsonObj.get("parentId");
						if (parentIdEl != null && !parentIdEl.isJsonNull()) {
							v1_value = parentIdEl.getAsString();
						}
					} else {
						System.err.println(counterdeneme++ + "- colorId alamadıgımız uriler: " + prodId);
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}
		return v1_value;
	}

	private String extractColorId(JsonObject productJsonObj) {
		String colorId = null;
		JsonElement productElement = productJsonObj.get("product");
		if (productElement != null && !productElement.isJsonNull()) {
			JsonObject productObj = productElement.getAsJsonObject();
			JsonElement detailElement = productObj.get("detail");
			if (detailElement != null && !detailElement.isJsonNull()) {
				JsonObject detailObj = detailElement.getAsJsonObject();
				JsonElement colorElement = detailObj.get("colors");
				if (colorElement != null && !colorElement.isJsonNull()) {
					JsonArray colorsArray = colorElement.getAsJsonArray();
					if (colorsArray != null && !colorsArray.isJsonNull() && colorsArray.size() > 0) {
						JsonObject firstColorObj = colorsArray.get(0).getAsJsonObject();
						JsonElement idElement = firstColorObj.get("id");
						if (idElement != null && !idElement.isJsonNull()) {
							colorId = idElement.getAsString();
						}
					}
				}
			}
		}
		return colorId;
	}

	private JsonObject extractAllDocumentObj(String line) {
		JsonObject allDocumentObj = new JsonObject();
		if (line != null && !line.isEmpty()) {
			JsonObject productObj = new JsonParser().parse(line).getAsJsonObject();
			if (productObj != null && !productObj.isJsonNull()) {
				JsonElement sourceEl = productObj.get("_source");
				JsonObject sourceObj = sourceEl.getAsJsonObject();
				allDocumentObj = sourceObj.get(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).getAsJsonObject();
			}
		}
		return allDocumentObj;
	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

	private void prepareLoadBulkObj(String prodId, JsonArray areaServedArr, String desiredStr) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).startArray(Tag.AREA_SERVED.text());
			for (JsonElement areaServedEl : areaServedArr) {
				content.startObject();
				JsonObject areaServed = areaServedEl.getAsJsonObject();
				// adress country
				String addressCountry = Tag.ADDRESS_COUNTRY.text();
				if (areaServed != null && areaServed.has(addressCountry)) {
					JsonElement value = areaServed.get(addressCountry);
					if (value != null && !value.isJsonNull()) {
						content.field(addressCountry, value.getAsString());
					}
				}
				// lastSeen
				String lastSeen = Tag.LASTSEEN.text();
				if (areaServed != null && areaServed.has(lastSeen)) {
					JsonElement value = areaServed.get(lastSeen);
					if (value != null && !value.equals("null") && !value.isJsonNull()) {
						content.field(lastSeen, value.getAsString());
					}
				}
				// onSale
				String onSale = Tag.ONSALE.text();
				if (areaServed != null && areaServed.has(onSale)) {
					JsonElement value = areaServed.get(onSale);
					if (value != null && !value.isJsonNull()) {
						content.field(onSale, Boolean.valueOf(value.getAsString()));
					}
				}
				// online
				String online = Tag.ONLINE.text();
				if (areaServed != null && areaServed.has(online)) {
					JsonElement value = areaServed.get(online);
					if (value != null && !value.isJsonNull()) {
						content.field(online, Boolean.valueOf(value.getAsString()));
					}
				} else {
					content.field(online, Boolean.valueOf(true));
				}
				// inStock
				String inStock = Tag.INSTOCK.text();
				if (areaServed != null && areaServed.has(inStock)) {
					JsonElement value = areaServed.get(inStock);
					if (value != null && !value.isJsonNull()) {
						content.field(inStock, Boolean.valueOf(value.getAsString()));
					}
				}
				// url
				if (areaServed != null && areaServed.has(URL)) {
					JsonElement value = areaServed.get(URL);
					if (value != null && !value.isJsonNull()) {
						String newUrl = createCanonicalUri(value.getAsString());
						String nonDesired = findWithPattern(newUrl, "\\/en\\/-(p.*.html)$", 1);
						if (nonDesired != null) {
							if (desiredStr.contains(".html")) {
								newUrl = newUrl.replaceAll(nonDesired, desiredStr);
							} else {
								newUrl = newUrl + desiredStr;
							}
						}
						content.field(URL, newUrl);
					}
				}
				// offers
				String offers = Tag.OFFERS.text();
				String price = Tag.PRICE.text();
				String priceCurrency = Tag.PRICE_CURRENCY.text();
				String validFrom = Tag.VALIDFROM.text();
				content.startArray(offers);
				JsonArray offersArray = new JsonArray();
				if (areaServed.has(offers)) {
					offersArray = areaServed.get(offers).getAsJsonArray();
					for (JsonElement offersEl : offersArray) {
						content.startObject();
						if (offersEl.getAsJsonObject().has(price)) {
							content.field(price, offersEl.getAsJsonObject().get(price).getAsDouble())
									.field(priceCurrency, offersEl.getAsJsonObject().get(priceCurrency).getAsString())
									.field(Tag.TYPE.text(), "Offer");
							if (offersEl.getAsJsonObject().has(validFrom)) {
								content.field(validFrom, offersEl.getAsJsonObject().get(validFrom).getAsString());
							} else {
								if (areaServed.has(lastSeen)) {
									content.field(validFrom, areaServed.get(lastSeen).getAsString());
								}
							}
						}
						content.endObject();
					}
					content.endArray().endObject();
				}
			}
			content.endArray().endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			// System.out.println(counter++ + "- " + content.string());
			bulk.add(updateReq);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Document sendGet(String oldProductUrl) throws IOException {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Document doc = null;
		URL obj = new URL(oldProductUrl.replace("http://", "https://"));
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent",
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.108 Safari/537.36");
		int responseCode = con.getResponseCode();
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			doc = Jsoup.parse(response.toString());
			doc.outputSettings().escapeMode(EscapeMode.xhtml);
		} else {
			System.err.println("GET request not worked");
		}
		return doc;
	}

}
