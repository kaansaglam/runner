package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hp.hpl.jena.sparql.pfunction.library.listIndex;

import io.wgb.organization.common.Constants;
import wgb.io.Tag;

public class AreaServedUpdater extends RunnerUtil {
	private static final Logger logger = LogManager.getLogger(AreaServedUpdater.class);
	private int counter = 0;

	public static void main(String args[]) {
		new AreaServedUpdater().execute();
	}

	private void execute() {

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/zara-26-oc.json"))) {

			createCleanMapFromFile();

			for (String line; (line = br.readLine()) != null;) {
				if (!line.isEmpty()) {

					JsonObject allDoc = new JsonParser().parse(line).getAsJsonObject().get("_source").getAsJsonObject()
							.get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					List<String> problematicCountry = prodIdCountry.get(id);
					if (problematicCountry != null ) {
						String dateStr = allDoc.getAsJsonObject().get("datePublishedObject").getAsJsonObject()
								.get("value").getAsString();
						Date date = newExtractedDateFormatWithTimeZone().parse(dateStr);
						List<String> deleteAllCountry = new ArrayList<>();
						List<String> deleteOffers = new ArrayList<>();
						JsonArray areaServed = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();
						List<String> unDesiredUrlPartList = null;
						unDesiredUrlPartList = undesiredUrlPart(areaServed, problematicCountry);
						for (JsonElement areaServedEl : areaServed) {
							JsonObject areaServedObjet = areaServedEl.getAsJsonObject();
							String addressCounry = areaServedObjet.get(Tag.ADDRESS_COUNTRY.text()).getAsString();
							String url = areaServedObjet.get(Tag.URL.text()).getAsString();
							String urlDesiredPart = findWithPattern(url, "\\/-p[0-9]+.html\\?v1=[0-9]+", 0);
							// dosyadaki ülke ise ya da o ülkenin uri'si ile aynı uri ise bu ülkeyi de
							// incele..
							if (problematicCountry.contains(addressCounry.toUpperCase()) || (urlDesiredPart != null
									&& unDesiredUrlPartList != null && unDesiredUrlPartList.contains(urlDesiredPart))) {
								JsonArray offerArray = areaServedObjet.get(Tag.OFFERS.text()).getAsJsonArray();
								if (!offerArray.isJsonNull() && offerArray.size() > 0) {
									String dateStrOffer = offerArray.get(0).getAsJsonObject().get(Tag.VALIDFROM.text())
											.getAsString();
									Date dateOffer = newExtractedDateFormat().parse(dateStrOffer);
									int compareTo = date.compareTo(dateOffer);
									// bakılan ülkenin ilk fiyat tarihi ürünle aynı ise sadece ilk fiyatı kalsın.
									if (compareTo == 1) {
										// burada sorunlu ülkenin ilk offer elemanını bırak gerisini sil..
										if (!deleteOffers.contains(addressCounry)) {
											deleteOffers.add(addressCounry);
										}
										// bakılan ülkenin tarihi yeni ise
									} else {
										// burada ülkeyi komple sil..
										if (!deleteOffers.contains(addressCounry)) {
											deleteAllCountry.add(addressCounry);
										}
									}
								}
							}
						}
						System.out.println(problematicCountry + "  " + dateStr + " " + id);
						System.out.println(deleteOffers);
						System.out.println(deleteAllCountry);
						areaServedBulkCreator1(id, areaServed, deleteOffers, deleteAllCountry);
					}
				}
				if (counter % BULK_SIZE == 1 && counter > 1) {
					// System.out.println("SENDED BULK:" +
					// bulk.numberOfActions() + " " + counter);
					// sendBulk();
				}
			}
			// sendBulk();
			logger.info("DONE!");
			System.exit(0);
		} catch (Exception e1) {
			e1.printStackTrace();
			System.out.println(e1);
		}
	}

	private List<String> undesiredUrlPart(JsonArray areaServed, List<String> problematicCountry) {
		List<String> unDesiredUrlPart = new ArrayList<>();
		for (JsonElement areaServedElTemp : areaServed) {
			JsonObject areaServedObj = areaServedElTemp.getAsJsonObject();
			String url = areaServedObj.get(Tag.URL.text()).getAsString();
			String countryAreaServed = areaServedObj.get(Tag.ADDRESS_COUNTRY.text()).getAsString();
			if (problematicCountry.contains(countryAreaServed.toUpperCase())) {
				unDesiredUrlPart.add(findWithPattern(url, "\\/-p[0-9]+.html\\?v1=[0-9]+", 0));
			}
		}
		return unDesiredUrlPart;
	}

	private void areaServedBulkCreator1(String prodId, JsonArray areaServedArr, List<String> deleteOffers,
			List<String> deleteAllCountry) throws Exception {
		List<String> duplicateCountry = new ArrayList<>();
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).startArray(Tag.AREA_SERVED.text());

			for (JsonElement areaServedEl : areaServedArr) {
				JsonObject areaServed = areaServedEl.getAsJsonObject();
				String address1 = null;
				if (areaServed != null && areaServed.has(Tag.ADDRESS_COUNTRY.text())) {
					JsonElement value = areaServed.get(Tag.ADDRESS_COUNTRY.text());
					if (value != null && !value.isJsonNull()) {
						address1 = value.getAsString();
					}
				}
				if (!deleteAllCountry.contains(address1.toUpperCase()) && !duplicateCountry.contains(address1)) {
					duplicateCountry.add(address1);
					content.startObject();
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
							if (deleteOffers.contains(address.toUpperCase())) {
								break;
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
			System.out.println(content.string());
		} catch (

		IOException e) {
			System.out.println(e);
			e.printStackTrace();
		}

	}

	public static final String EXTRACTED_DATE_FORMAT_WITH_TIMEZONE = "dd.MM.yyyy HH:mm Z";

	public static final String EXTRACTED_DATE_FORMAT = "dd.MM.yyyy";

	public static final String EXTRACTED_DATE_FORMAT_NO_TIME = "dd.MM.yyyy HH:mm";

	public static SimpleDateFormat newExtractedDateFormatNoTıme() {
		return new SimpleDateFormat(EXTRACTED_DATE_FORMAT_NO_TIME);
	}

	public static SimpleDateFormat newExtractedDateFormat() {
		return new SimpleDateFormat(EXTRACTED_DATE_FORMAT);
	}

	public static SimpleDateFormat newExtractedDateFormatWithTimeZone() {
		return new SimpleDateFormat(EXTRACTED_DATE_FORMAT_WITH_TIMEZONE);
	}

	HashMap<String, List<String>> prodIdCountry = null;

	private void createCleanMapFromFile() throws FileNotFoundException, IOException, InterruptedException {
		BufferedReader br = null;
		prodIdCountry = new HashMap<>();
		String line;
		int count = 0;
		br = new BufferedReader(new FileReader("/home/galaksiya/IncompatibleUrl.txt"));
		while ((line = br.readLine()) != null) {
			line = line.substring(line.indexOf("{\""));
			JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
			String identifier = jsonObject.get("identifier").getAsString();
			String country = jsonObject.get("countrty").getAsString();
			List<String> countryList = prodIdCountry.get(identifier);
			if (countryList != null && !countryList.contains(country.toUpperCase())) {
				countryList.add(country.toUpperCase());
			} else {
				countryList = new ArrayList<>();
				countryList.add(country.toUpperCase());
			}
			prodIdCountry.put(identifier, countryList);

			// System.out.println(jsonObject.get("secondaryWithYear").getAsString());
			// System.out.println(jsonObject.get("secondaryFromESWithYear").getAsString());
			// System.out.println(line);
			// System.out.println(count++);

		}
		br.close();
		System.out.println("clean map is ready..");
	}

	DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final String USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";

	public Document htttpReques(String url) throws Exception {
		if (!url.contains("https")) {
			url = url.replace("http", "https");
		}
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		// optional default is GET
		con.setRequestMethod("GET");
		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setConnectTimeout(2000);
		Thread.sleep(1000);

		int responseCode = con.getResponseCode();
		// yönlendirme varsa gittigi url
		URL redirectURL = con.getURL();
		if (url.contains("lcwaikiki") && responseCode == 404) {
			writeIntoConsole(url, responseCode, redirectURL);
		}

		if (url.contains("defacto") && !url.equals(redirectURL.toString())) {
			writeIntoConsole(url, responseCode, redirectURL);
		}
		if (url.contains("zara")
				&& (responseCode == 404 || findWithPattern(redirectURL.toString(), "p[0-9]\\d{3,}", 0) == null)) {
			writeIntoConsole(url, responseCode, redirectURL);
		}
		Document doc = null;
		if (url.contains("mango") && responseCode != 404) {

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			doc = Jsoup.parse(response.toString());
			doc.outputSettings().escapeMode(EscapeMode.xhtml);
			String metaİmage = null;
			if (doc != null) {
				metaİmage = doc.select("meta[property=og:image]").attr("content");
			}
			System.out.println(url + "  " + metaİmage);
			Date date = new Date();
			System.out.println(dateFormat.format(date));
		}
		if (url.contains("mango") && (responseCode == 404)) {
			writeIntoConsole(url, responseCode, redirectURL);

		}

		if (url.contains("hm.com")) {

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			if (response.toString().contains("Ürün bulunamadı")) {
				writeIntoConsole(url, responseCode, redirectURL);
			}

		}
		return doc;
	}

	// erişilemeyen uriyi consol'a yazdır
	private void writeIntoConsole(String url, int responseCode, URL redirectURL) {
		counter++;
		System.out.println("Count : " + counter + "  Response Code : " + responseCode + "\nrequested : " + url);
		System.out.println("redirect  : " + redirectURL + "\n");
	}

}