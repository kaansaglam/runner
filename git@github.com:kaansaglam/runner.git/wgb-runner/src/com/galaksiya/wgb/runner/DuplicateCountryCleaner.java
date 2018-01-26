package com.galaksiya.wgb.runner;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.galaksiya.util.elasticsearch.parameter.DateParameter;
import com.galaksiya.util.elasticsearch.parameter.TimeRange;
import com.galaksiya.util.elasticsearch.parameter.ValuesParameter;
import com.galaksiya.util.elasticsearch.result.ElasticSearchResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.BasedOn;
import wgb.io.JsonFields;
import wgb.io.Tag;

public class DuplicateCountryCleaner {
	private static final String UNDESIRED_URI_PART = "?$S$&wid=";
	private static final Logger logger = Logger.getLogger(DuplicateCountryCleaner.class);
	public static FileWriter fileWriter;
	public static int a = 0;

	public static void main(String[] args) throws UnknownHostException {
		TransportClient client = TransportClient.builder().build();
		ElasticSearchConfig config = new ElasticSearchConfig();
		int port = config.getPort();
		String[] elasticSearchHosts = config.getHosts().split(",");
		for (String host : elasticSearchHosts) {
			client = client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));
		}

		String dayBackString = args[0]; // from 2 day before
		String daysString = args[1]; // 10 days

		int back = Integer.parseInt(dayBackString);
		int days = Integer.parseInt(daysString) + back;
		int dayBack = 0 + back;
		try {
			fileWriter = new FileWriter("/home/galaksiya/areaServed1.2.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ElasticSearchResult firstProducts = findProducts(dayBack);
		while (firstProducts.getSearchResults().isEmpty()) {
			dayBack++;
			firstProducts = findProducts(dayBack);
			if (dayBack > days) {
				break;
			}
		}
		update(client, firstProducts, dayBack, days);
		System.out.println("DONE!");
	}

	private static void update(TransportClient client, ElasticSearchResult res, int dayBack, int days) {
		List<JsonObject> searchResults = res.getSearchResults();
		for (JsonElement product : searchResults) {
			JsonObject productObject = product.getAsJsonObject();
			if (productObject != null) {
				try {
					// try {
					String prodId = productObject.get(Tag.ID.text()).getAsString();
					JsonElement areaServed = productObject.get(Tag.AREA_SERVED.text());
					if (areaServed != null && areaServed.isJsonArray()) {
						JsonArray newAreaServedArray = createNewAreaServedArray(areaServed.toString());

						if (newAreaServedArray != null && newAreaServedArray.size() > 0
								&& !newAreaServedArray.equals(areaServed)) {

							JsonObject updateObj = new JsonObject();
							updateObj.addProperty(Tag.ID.text(), prodId);
							updateObj.add(Tag.AREA_SERVED.text(), newAreaServedArray);
							updateObj.add("OLDAREASERVED", areaServed);
							// System.out.println(updateObj);
							a = a + 1;
							System.out.println(a);
							fileWriter.write(updateObj.toString());
							fileWriter.write("\n");

							// XContentBuilder content =
							// XContentFactory.jsonBuilder().startObject()
							// .startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT)
							// .startArray(Tag.AREA_SERVED.text());
							//
							// for (JsonElement jsonElement :
							// newAreaServedArray) {
							// content.field(jsonElement.toString());
							// }
							// content.endArray();
							// content.endObject();
							//
							// client.prepareUpdate(ElasticSearchClient.INDEX_FASHION,
							// ElasticSearchClient.TYPE_PRODUCT,
							// prodId).setDoc(content).get();
							//
							// System.err.println("updated: " + prodId);
							// } else {
							// System.err.println("already correct: " + prodId);
							// }

						}
						// }
						// catch (IOException e) {
						// e.printStackTrace();
					}

				} catch (Exception e) {
					logger.error("could not update: " + productObject, e);
				}
			}
		}

		// continue
		ElasticSearchResult result = ElasticSearchClient.getInstance().search(res.getSearchId(), null, null, null,
				null);
		if (result.getSearchResults().size() != 0) {
			update(client, result, dayBack, days);
		} else {
			int newDayBack = dayBack + 1;
			if (newDayBack < days) {
				ElasticSearchResult products = findProducts(newDayBack);
				while (products.getSearchResults().isEmpty() && newDayBack < days) {
					newDayBack++;
					products = findProducts(newDayBack);
				}
				update(client, products, newDayBack, days);
			}
		}
	}

	private static JsonArray createNewAreaServedArray(String areaServed) {
		HashMap<String, JsonObject> countryMap = new HashMap<>();
		JsonArray tempArr = new JsonParser().parse(areaServed).getAsJsonArray();
		JsonArray areaServedArr = new JsonParser().parse(areaServed).getAsJsonArray();
		for (JsonElement areaServedEl : areaServedArr) {
			JsonObject countryJsonObject = areaServedEl.getAsJsonObject();
			String countryCode = countryJsonObject.get(Tag.ADDRESS_COUNTRY.text()).getAsString();
			if (countryMap.get(countryCode) == null) {
				countryMap.put(countryCode, countryJsonObject);
			} else {
				JsonObject duplicated = new JsonObject();
				duplicated = countryMap.get(countryCode);

				String duplicatedDate = duplicated.get(Tag.LASTSEEN.text()).getAsString();
				String date = countryJsonObject.get(Tag.LASTSEEN.text()).getAsString();
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyy hh:mm");
				try {
					if (sdf.parse(duplicatedDate).before(sdf.parse(date))) {
						tempArr.remove(duplicated);
						tempArr.remove(countryJsonObject);
						JsonArray newOffersArr = countryJsonObject.get(Tag.OFFERS.text()).getAsJsonArray();
						JsonArray oldOfferArr = duplicated.get(Tag.OFFERS.text()).getAsJsonArray();
						JsonArray tempOffersArr = new JsonParser().parse(oldOfferArr.toString()).getAsJsonArray();
						for (JsonElement newObj : newOffersArr) {
							Boolean addPrice = true;
							double newPrice = newObj.getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
							for (JsonElement oldObj : oldOfferArr) {
								double oldPrice = oldObj.getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
								if (newPrice == oldPrice) {
									addPrice = false;
								}
							}
							if (addPrice) {
								tempOffersArr.add(newObj);
							}
						}
						countryJsonObject.add(Tag.OFFERS.text(), tempOffersArr);
						tempArr.add(countryJsonObject);
					} else {

					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
				countryMap.put(countryCode, countryJsonObject);
			}

		}
		return tempArr;
	}

	/**
	 * Finds products for given day before.
	 * 
	 * @param day
	 *            day before.
	 * @return elastic search result.
	 */
	public static ElasticSearchResult findProducts(int day) {
		ElasticSearchResult result = null;
		DateParameter dateParameter = getDateParameter(day);
		result = ElasticSearchClient.getInstance().search(null, null, ElasticSearchClient.INDEX_GARMENT,
				ElasticSearchClient.TYPE_PRODUCT, null, dateParameter,
				new ValuesParameter(ElasticSearchClient.PROPERTY_BASED_ON, BasedOn.MANGO.toString()));

		return result;
	}

	private static DateParameter getDateParameter(int day) {
		Date startDate = new Date(TimeRange.getStartOfTheDay(dayBefore(day)));
		Date endDate = new Date(TimeRange.getEndOfTheDay(dayBefore(day)));
		return new DateParameter(startDate.getTime(), endDate.getTime());
	}

	private static Calendar dayBefore(int dayBack) {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DAY_OF_MONTH, -dayBack);
		return calendar;
	}

}
