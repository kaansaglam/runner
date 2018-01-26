package com.galaksiya.wgb.runner;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.galaksiya.util.elasticsearch.ElasticSearchUtil;
import com.galaksiya.util.elasticsearch.parameter.DateParameter;
import com.galaksiya.util.elasticsearch.parameter.TimeRange;
import com.galaksiya.util.elasticsearch.parameter.ValuesParameter;
import com.galaksiya.util.elasticsearch.result.ElasticSearchResult;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wgb.io.BasedOn;
import wgb.io.Tag;

public class ZaraIdUpdater {

	private static final Logger logger = Logger.getLogger(ZaraIdUpdater.class);
	private static int counterNotUpdate = 0;
	private static int counterUpdate = 0;

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
		ElasticSearchResult firstProducts = findProducts(dayBack);
		while (firstProducts.getSearchResults().isEmpty()) {
			dayBack++;
			firstProducts = findProducts(dayBack);
			if (dayBack > days) {
				break;
			}
		}
		update(client, firstProducts, dayBack, days);
		System.out.println("---DONE---");
		client.admin().indices().refresh(new RefreshRequest().indices(ElasticSearchClient.INDEX_GARMENT)).actionGet();
	}

	private static void update(TransportClient client, ElasticSearchResult res, int dayBack, int days) {
		List<JsonObject> searchResults = res.getSearchResults();
		for (JsonElement product : searchResults) {
			JsonObject productObject = product.getAsJsonObject();
			if (productObject != null) {
				try {
					String prodId = productObject.get(Tag.ID.text()).getAsString();
					String newProdId = createCanonicalUri(prodId);
					JsonArray areaServedArray = productObject.get("areaServed").getAsJsonArray();
					String isOnly_p_Code = findWithPattern(newProdId, "\\/en\\/-p.*.html$", 0);
					if (newProdId != null && !prodId.isEmpty() && isOnly_p_Code != null) {
						try {
							String desiredStr = isDesired(prodId, areaServedArray);
							if (desiredStr != null) {
								String nonDesired = findWithPattern(newProdId, "\\/en\\/-(p.*.html)$", 1);
								if (nonDesired != null) {
									if (desiredStr.contains(".html")) {
										newProdId = newProdId.replaceAll(nonDesired, desiredStr);
									} else {
										newProdId = newProdId + desiredStr;
									}
								}
							}
						} catch (Exception e) {
							System.err.println("failed to change prodId: " + prodId);
						}
						if (!newProdId.equals(prodId)) {
							productObject.addProperty(Tag.ID.text(), newProdId);
							client.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT,
									prodId).get();
							ElasticSearchClient cli = ElasticSearchClient.getInstance();
							cli.addProduct(ElasticSearchUtil.convertElasticSearchFormat(productObject.toString()));
							System.err.println(counterUpdate++ + "- updated: " + newProdId);
						}
					} else {
						System.err.println(counterNotUpdate++ + "- COULD NOT FOUND PRODUCT ID:" + prodId);
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

	private static String isDesired(String prodId, JsonArray areaServed) {
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

	private final static String PRODUCT_URI_REGEX = "\\/en\\/(.*)-";

	private static String createCanonicalUri(String trackedUri) {
		String canonicalUri = null;
		String expectedStr = findWithPattern(trackedUri, PRODUCT_URI_REGEX, 1);
		if (expectedStr != null) {
			canonicalUri = trackedUri.replace(expectedStr, "");
		}
		return canonicalUri;
	}

	protected static String findWithPattern(String uri, String pattern, Integer groupNo) {
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(uri);
		if (m.find()) {
			return m.group(groupNo);
		}
		return null;
	};

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
				new ValuesParameter(ElasticSearchClient.PROPERTY_BASED_ON, BasedOn.ZARA.toString()));

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
