package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

public class CurrencyCleaner {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(CurrencyCleaner.class);

	public static void main(String args[]) {
		new CurrencyCleaner().execute();
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

	private int counter = 0;

	private void execute() {
		currencyMap.put("TR", "TRY");
		currencyMap.put("US", "USD");
		currencyMap.put("UK", "GBP");
		currencyMap.put("CA", "CAD");

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/mango-17-ekim.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {

					counter++;
					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
					JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();

					if (allDoc.has(Tag.AREA_SERVED.text())) {
						JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();
						prepareLoadBulkObj(id, areaServedArr);
					}

					if (counter % BULK_SIZE == 1 && counter > 1) {
						sendBulk();
					}
				}
			}
			sendBulk();
			System.out.println(sayi2);

			System.out.println("total product size" + counter);

			logger.info("DONE!");
			System.exit(0);
		} catch (

		FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void sendBulk() {
		System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
		bulk.get();
		bulk = getESTransportClient().prepareBulk();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String findProdId(String id) {
		String a;
		if (id.contains("id=")) {
			a = id.substring(id.indexOf("id=") + 3);
		} else {
			a = id.substring(id.indexOf("_") + 1, id.indexOf(".html"));
		}
		return a;
	}

	int sayi = 0;
	int sayi2 = 0;

	private void prepareLoadBulkObj(String prodId, JsonArray areaServedArr) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).startArray(Tag.AREA_SERVED.text());

			for (JsonElement areaServedEl : areaServedArr) {
				content.startObject();
				JsonObject areaServed = areaServedEl.getAsJsonObject();

				if (areaServed != null && areaServed.has(Tag.URL.text())) {
					JsonElement value = areaServed.get(Tag.URL.text());
					if (value != null && !value.isJsonNull()) {
						content.field(Tag.URL.text(), value.getAsString());
					}
				}

				if (areaServed != null && areaServed.has(Tag.LASTSEEN.text())) {
					JsonElement value = areaServed.get(Tag.LASTSEEN.text());
					if (value != null && !value.equals("null") && !value.isJsonNull()) {
						content.field(Tag.LASTSEEN.text(), value.getAsString());
					}
				}
				if (areaServed != null && areaServed.has(Tag.ONSALE.text())) {
					JsonElement value = areaServed.get(Tag.ONSALE.text());
					if (value != null && !value.isJsonNull()) {
						content.field(Tag.ONSALE.text(), Boolean.valueOf(value.getAsString()));
					}
				}
				if (areaServed != null && areaServed.has(Tag.INSTOCK.text())) {
					JsonElement value = areaServed.get(Tag.INSTOCK.text());
					if (value != null && !value.isJsonNull()) {
						content.field(Tag.INSTOCK.text(), Boolean.valueOf(value.getAsString()));
					}
				}
				if (areaServed != null && areaServed.has(Tag.ONLINE.text())) {
					JsonElement value = areaServed.get(Tag.ONLINE.text());
					if (value != null && !value.isJsonNull()) {
						content.field(Tag.ONLINE.text(), Boolean.valueOf(value.getAsString()));
					}
				} else {
					content.field(Tag.ONLINE.text(), Boolean.valueOf(true));
				}
				String address = null;
				if (areaServed != null && areaServed.has(Tag.ADDRESS_COUNTRY.text())) {
					JsonElement value = areaServed.get(Tag.ADDRESS_COUNTRY.text());
					if (value != null && !value.isJsonNull()) {
						address = value.getAsString();
						content.field(Tag.ADDRESS_COUNTRY.text(), value.getAsString());
					}
				}

				content.startArray(Tag.OFFERS.text());
				JsonArray offers = new JsonArray();
				if (areaServed.has(Tag.OFFERS.text())) {

					offers = areaServed.get(Tag.OFFERS.text()).getAsJsonArray();
					List<String> priceList = new ArrayList<>();

					for (JsonElement jsonElemen : offers) {
						if (jsonElemen.getAsJsonObject().has(Tag.PRICE.text())) {
							String price = jsonElemen.getAsJsonObject().get(Tag.PRICE.text()).getAsString();
							priceList.add(price);
						}
					}
					if (priceList != null && !priceList.isEmpty()) {
						priceList = priceRepair(priceList, prodId);

					}

					for (int i = 0; i < priceList.size(); i++) {
						JsonElement jsonElement = offers.get(i);
						if (jsonElement.getAsJsonObject().has(Tag.PRICE.text())) {

							String currency = jsonElement.getAsJsonObject().get(Tag.PRICE_CURRENCY.text())
									.getAsString();

							if (currencyMap.get(address).equals(currency)) {
								content.startObject();

								Double price = null;
								try {
									price = Double.parseDouble(priceList.get(i));
								} catch (Exception e) {
									// TODO: handle exception
									price = jsonElement.getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
								}
								content.field(Tag.PRICE.text(), price).field(Tag.PRICE_CURRENCY.text(), currency)
										.field(Tag.TYPE.text(), "Offer");
								if (jsonElement.getAsJsonObject().has(Tag.VALIDFROM.text())) {
									content.field(Tag.VALIDFROM.text(),
											jsonElement.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString());
								} else {
									if (areaServed.has(Tag.LASTSEEN.text())) {
										content.field(Tag.VALIDFROM.text(),
												areaServed.get(Tag.LASTSEEN.text()).getAsString());
									}
								}
								content.endObject();
							} else {
								System.out.println(prodId);
							}
						}
					}
					content.endArray();
					content.endObject();
				}
			}

			content.endArray().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	HashMap<String, String> currencyMap = new HashMap<>();

	private List<String> priceRepair(List<String> priceList, String uri) {
		return priceList;

	}

}