package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
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

public class AreaServedHM {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(AreaServedHM.class);

	public static void main(String args[]) {
		new AreaServedHM().execute();
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
	int a = 0;

	private void execute() {

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/mango-22-kasım.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				try {
					if (!line.isEmpty()) {

						JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
						JsonObject _source = jsonObj.get("_source").getAsJsonObject();
						String dateStr = _source.get("datePublished").getAsString();
						// // 23.05.2017 19:40
						DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
						DateFormat format2 = new SimpleDateFormat("dd.MM.yyyy");

						// Date date = format.parse(dateStr);
						// Date today = format.parse("18.10.2017 00:00");
						JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument")
								.getAsJsonObject();
						String id = allDoc.get("id").getAsString();
						JsonArray images = allDoc.get(Tag.IMAGES.text()).getAsJsonArray();
						String secondary = null;
						if (allDoc.has(Tag.AREA_SERVED.text())) {
							JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();
							for (JsonElement jsonElement : areaServedArr) {
								JsonArray offersArr = jsonElement.getAsJsonObject().get(Tag.OFFERS.text())
										.getAsJsonArray();
								String url = jsonElement.getAsJsonObject().get(Tag.URL.text()).getAsString();

								String yesterday = null;
								for (JsonElement offers : offersArr) {
									String date = offers.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
									Date currentdate = format.parse(date);
									String currentdateStr = format2.format(currentdate);
									if (yesterday == null) {
										yesterday = currentdateStr;
									} else {

										if (yesterday.equals(currentdateStr)) {
											counter++;
											System.out.println(counter + " " + url + " " + offersArr);
											yesterday = currentdateStr;
											break;

										}
									}
								}

								// if (offersArr.size() > 5) {
								// counter++;
								// System.out.println(" "+dateStr +"
								// "+offersArr+ " " + url);
								// System.out.println(counter);
								// }
							}
							// if
							// (id.equals("http://www.hm.com/ph/product/77672?article=77672-A"))
							// {
							// System.out.println(id);
							// }
							// secondary = findIdFromImages(images, id);
							// if (secondary == null) {
							// secondary = findIdentifier(id);
							// // secondary = findeSecondary(id, areaServedArr);
							// }
							// if (secondary != null && !secondary.isEmpty()) {
							// prepareLoadBulkObj(id, secondary);
							// // System.out.println(secondary);
							// } else {
							// System.out.println(counter1++ + " secondary
							// bulunamadı " + id);
							// }
							// System.err.println(secondary + " " + id);
						}

						if (counter % BULK_SIZE == 1 && counter > 1) {
							// System.out.println("SENDED BULK:" +
							// bulk.numberOfActions() + " " + counter);
							// sendBulk();
						}
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			// sendBulk();
			System.out.println(" tüm ülkelerin sayısı: " + sayi2);
			//
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

	private void prepateDeleteBulkObj(String id) {
		DeleteRequestBuilder deleteReq = getESTransportClient()
				.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id).setId(id);
		bulk.add(deleteReq);
	}

	private String findIdFromImages(JsonArray images, String id) {
		String secondary = null;
		for (JsonElement imagesElement : images) {
			String imageUrl = imagesElement.getAsJsonObject().get("uri").getAsString();
			String[] split = imageUrl.split(" ");
			if (split.length > 2) {
				// System.out.println(split[1] + " " + id + " " + counter1++);
				secondary = split[1] + split[2];
				if (secondary != null) {
					break;
				}
			}
		}
		return secondary;
	}

	private void sendBulk() {
		bulk.get();
		bulk = getESTransportClient().prepareBulk();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	List<String> prodNo = new ArrayList<>();
	int counter1 = 0;
	int sayi = 0;
	int sayi2 = 0;
	String uriArrStr = "";
	// JsonArray uriArr = new JsonParser().parse(uriArrStr).getAsJsonArray();
	int bok = 0;

	private String findeSecondary(String prodId, JsonArray areaServedArr) {
		String secondary = null;
		for (JsonElement areaServedEl : areaServedArr) {
			JsonObject areaServed = areaServedEl.getAsJsonObject();
			if (areaServed != null && areaServed.has(Tag.URL.text())) {
				JsonElement value = areaServed.get(Tag.URL.text());
				// add2list(prodId, imagesArray, value.getAsString(),
				// list);
				String countryUrl = value.getAsString();
				if (countryUrl.contains("www2")) {
					String areaServedProdNo = findIdentifier(countryUrl);
					secondary = areaServedProdNo.substring(0, areaServedProdNo.length() - 3);
					break;
				}
			}
		}
		return secondary;
	}

	private void prepareLoadBulkObj(String prodId, String secondary) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.field(ElasticSearchClient.PROPERTY_SECONDARY, secondary);

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			content.field(ElasticSearchClient.PROPERTY_SECONDARY, secondary);
			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

	private String findIdentifier(String prodId) {
		String prodNo = null;
		if (prodId != null) {
			prodNo = findWithPattern(prodId, "productpage.([0-9]+)", 1);
		}
		return prodNo;
	}

}