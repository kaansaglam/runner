package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

import com.galaksiya.agent.Agent;
import com.galaksiya.extractor.fashion.AbstractFashionExtractor;
import com.galaksiya.extractor.fashion.HMExtractor;
import com.galaksiya.extractor.fashion.ImagelessProductException;
import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wgb.io.Tag;

public abstract class RunnerUtil {

	public String name;
	public Agent agent;
	public static final int BULK_SIZE = 1000;
	public static final Logger logger = LogManager.getLogger(RunnerUtil.class);
	public int counter = 0;
	private HashMap<String, String> currencyMAp = new HashMap<>();
	public static TransportClient esTransportClient;

	public static TransportClient getESTransportClient() {
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

	public BulkRequestBuilder bulk = getESTransportClient().prepareBulk();

	HMExtractor hmExtractor = new HMExtractor(name, agent);
	AbstractFashionExtractor abstractExtractor = new AbstractFashionExtractor(name, agent) {

		@Override
		protected String extract(String pageContent, String trackedUri, Boolean isNewUri)
				throws ImagelessProductException {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public void sendBulk() {
		if (bulk.numberOfActions() > 0) {
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			bulk.get();
			bulk = getESTransportClient().prepareBulk();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	}

	public void datePublishedBulkCreator(String prodId, String secondary) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.field(ElasticSearchClient.PROPERTY_DATE_PUBLISHED, secondary);

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			content.field(ElasticSearchClient.PROPERTY_DATEPUBLISHED, secondary);
			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void secondaryBulkCreator(String prodId, String secondary) {
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
			e.printStackTrace();
		}

	}

	public void productCategoryBulkCreator(String prodId, String productCategory) throws Exception {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			if (productCategory != null && !productCategory.isEmpty()) {
				content.field(Tag.PRODUCT_CATEGORY.text(), productCategory);
			}

			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void titleBulkCreator(String prodId, String productCategory) throws Exception {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			if (productCategory != null && !productCategory.isEmpty()) {
				content.field(Tag.TITLE.text(), productCategory);
			}

			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
			System.out.println(content.string());
		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void descriptionBulkCreator(String prodId, String description) throws Exception {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			if (description != null && !description.isEmpty()) {
				content.field(Tag.DESCRIPTION.text(), description);
			}

			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
		} catch (

		IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void prepateDeleteBulkObj(String id) {
		DeleteRequestBuilder deleteReq = getESTransportClient()
				.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id).setId(id);
		bulk.add(deleteReq);
	}

	protected void areaServedBulkCreator(String prodId, JsonArray areaServedArr) throws Exception {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).startArray(Tag.AREA_SERVED.text());

			for (JsonElement areaServedEl : areaServedArr) {
				content.startObject();
				JsonObject areaServed = areaServedEl.getAsJsonObject();
				if (areaServed != null && areaServed.has(Tag.URL.text())) {
					JsonElement value = areaServed.get(Tag.URL.text());
					if (value != null && !value.isJsonNull()) {
						String url = value.getAsString();
						content.field(Tag.URL.text(), url);
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
						content.field(Tag.ADDRESS_COUNTRY.text(), address);
					}
				}

				content.startArray(Tag.OFFERS.text());
				JsonArray offers = new JsonArray();
				if (areaServed.has(Tag.OFFERS.text())) {

					List<String> priceList = new ArrayList<>();
					offers = areaServed.get(Tag.OFFERS.text()).getAsJsonArray();

					// fiyatları düzenleyip döndür..
					for (JsonElement jsonElemen : offers) {
						if (jsonElemen.getAsJsonObject().has(Tag.PRICE.text())) {
							String price = jsonElemen.getAsJsonObject().get(Tag.PRICE.text()).getAsString();
							priceList.add(price);
						}
					}

					for (int i = 0; i < priceList.size(); i++) {
						JsonElement jsonElement = offers.get(i);
						content.startObject();
						if (jsonElement.getAsJsonObject().has(Tag.PRICE.text())) {

							String currency = jsonElement.getAsJsonObject().get(Tag.PRICE_CURRENCY.text())
									.getAsString();

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
								String date = jsonElement.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
								DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
								Date formatteddate = format.parse(date);
								if (date.contains("+")) {
									date = date.substring(0, date.indexOf("+")).trim();

								}
								content.field(Tag.VALIDFROM.text(), date);
							} else {
								if (areaServed.has(Tag.LASTSEEN.text())) {
									content.field(Tag.VALIDFROM.text(),
											areaServed.get(Tag.LASTSEEN.text()).getAsString());
								}
							}
						}
						content.endObject();
					}

				}
				content.endArray();
				content.endObject();
			}
			content.endArray().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
					.setDoc(content);
			bulk.add(updateReq);
			System.out.println(content.string());
		} catch (

		IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}

	}

	public HashMap<String, String> getCurrencyMAp() {
		if (currencyMAp.size() < 1) {
			currencyMAp.put("TR", "TRY");
			currencyMAp.put("US", "USD");
			currencyMAp.put("UK", "GBP");
			currencyMAp.put("RO", "RON");
			currencyMAp.put("RU", "RUB");
			currencyMAp.put("CA", "CAD");
		}
		return currencyMAp;
	}

}