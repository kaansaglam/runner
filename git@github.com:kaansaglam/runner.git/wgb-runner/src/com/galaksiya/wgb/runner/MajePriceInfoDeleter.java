package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class MajePriceInfoDeleter {

	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(MajePriceInfoDeleter.class);

	public static void main(String args[]) {
		new MajePriceInfoDeleter().execute();
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

	private void execute() {
		try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/maje-13-oc.json"),
				Charset.forName("UTF-8"))) {
			try {
				stream.forEach(line -> {
					JsonObject allDocumentObj = extractAllDocumentObj(line);
					String prodId = allDocumentObj.get("id").getAsString();
					JsonArray areaServedArray = allDocumentObj.get("areaServed").getAsJsonArray();
					String price = extractPriceCurrency(areaServedArray);
					if (price.equals("0.01") || price.equals("0")) {
						prepareLoadBulkObjForCurrentPrice(prodId, allDocumentObj);
						prepareLoadBulkObjForAreaServed(prodId, areaServedArray);
						System.out.println();
						if (counter % BULK_SIZE == 1 && counter > 1) {
							System.out.println("SENDED BULK:" + bulk.numberOfActions() + " total product: " + counter);
							bulk.get();
							bulk = getESTransportClient().prepareBulk();
							try {
								Thread.sleep(2000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
			} catch (Exception e) {
				logger.error("could not load", e);
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			bulk.get();
			bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		System.exit(0);
	}

	private void prepareLoadBulkObjForCurrentPrice(String prodId, JsonObject allDocumentObj) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT)
					.field(ElasticSearchClient.PROPERTY_CURRENT_PRICE, "");
			content.endObject().endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
			System.out.println(content.string());
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	private String extractPriceCurrency(JsonArray areaServedArray) {
		String price = null;
		JsonObject areaServedObj = areaServedArray.get(0).getAsJsonObject();
		JsonElement offersEl = areaServedObj.get("offers");
		if (offersEl != null && !offersEl.isJsonNull()) {
			JsonArray offersArray = offersEl.getAsJsonArray();
			JsonElement priceCurrencyEl = offersArray.get(0).getAsJsonObject().get("price");
			if (priceCurrencyEl != null && !priceCurrencyEl.isJsonNull()) {
				price = priceCurrencyEl.getAsString();
			}
		}
		return price;
	}

	private void prepareLoadBulkObjForAreaServed(String prodId, JsonArray areaServedArr) {
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
				if (areaServed != null && areaServed.has(Tag.URL.text())) {
					JsonElement value = areaServed.get(Tag.URL.text());
					if (value != null && !value.isJsonNull()) {
						content.field(Tag.URL.text(), value.getAsString());
					}
				}
				// offers
				content.startArray(Tag.OFFERS.text());
			}
			content.endArray().endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			System.out.println(counter++ + "- " + content.string());
			bulk.add(updateReq);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}