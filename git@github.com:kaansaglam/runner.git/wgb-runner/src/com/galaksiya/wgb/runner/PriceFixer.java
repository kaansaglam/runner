package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.select.Elements;

import com.galaksiya.agent.Agent;
import com.galaksiya.extractor.fashion.AbstractFashionExtractor;
import com.galaksiya.extractor.fashion.HMExtractor;
import com.galaksiya.extractor.fashion.ImagelessProductException;
import com.galaksiya.extractor.fashion.MangoExtractor;
import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class PriceFixer extends RunnerUtil {

	public static void main(String args[]) {
		new PriceFixer().execute();
	}

	private int counter = 0;
	private int counter1 = 0;

	private void execute() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/zara-18-oc.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				try {

					// try {
					if (!line.isEmpty()) {

						JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
						JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument")
								.getAsJsonObject();
						String id = allDoc.get("id").getAsString();
						if (allDoc.has(Tag.AREA_SERVED.text())) {
							JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();

							prepareLoadBulkObj(id, areaServedArr);
							counter++;
						}

						if (counter % BULK_SIZE == 1 && counter > 1) {
							// sendBulk();
						}
					}
				} catch (Exception e) {
					System.out.println(e + " " + line);
				}
			}

			// sendBulk();
			System.out.println("DONE");
			System.exit(0);
		} catch (

		Exception e1) {
			e1.printStackTrace();
		}
	}

	private void prepareLoadBulkObj(String prodId, JsonArray areaServedArr) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).startArray(Tag.AREA_SERVED.text());

			for (JsonElement areaServedEl : areaServedArr) {

				JsonObject areaServed = areaServedEl.getAsJsonObject();

				content.startObject();

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
				String url = null;
				if (areaServed != null && areaServed.has(Tag.URL.text())) {
					JsonElement value = areaServed.get(Tag.URL.text());
					if (value != null && !value.isJsonNull()) {
						url = value.getAsString();
						content.field(Tag.URL.text(), url);
					}
				}
				content.startArray(Tag.OFFERS.text());
				JsonArray offers = new JsonArray();
				if (areaServed.has(Tag.OFFERS.text())) {
					offers = areaServed.get(Tag.OFFERS.text()).getAsJsonArray();
					if (offers.size() > 5) {
						System.out.println(counter1++ + prodId);
					}
					// burada saçma fiyatlar temizleniyor.
					if (offers.size() > 1) {
						// offers = fixPricesMango(offers, url);
						// offers = fixPricesHm(offers, url);
						// zara, mango, brs, pull ,str, decafto , lcw , hm
						// offers = fixPricesOffline(offers, url);
					}

					// burada hm.com ru ve ro fiyatlarını temizledik.
					// if (address.equals("RU") || address.equals("RO") || address.equals("TR")) {
					// System.out.println(url);
					// offers = cleanRoRuOffers(offers);
					// }

					// burada yanlış currency temizleniyor
					// if (address.equals("RO") || address.equals("RU")) {
					// offers = cleanCurrency(offers, address, url);
					// }

					for (JsonElement offerEl : offers) {
						content.startObject();
						if (offerEl.getAsJsonObject().has(Tag.PRICE.text())) {

							String currency = offerEl.getAsJsonObject().get(Tag.PRICE_CURRENCY.text()).getAsString();
							Double price = offerEl.getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();

							content.field(Tag.PRICE.text(), price).field(Tag.PRICE_CURRENCY.text(), currency)
									.field(Tag.TYPE.text(), "Offer");
							if (offerEl.getAsJsonObject().has(Tag.VALIDFROM.text())) {
								String date = offerEl.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
								DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
								// Date formatteddate = format.parse(date);
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
		} catch (

		Exception e) {
			e.printStackTrace();
		}

	}

	// mangonun bir fiyatını elle gömüyoruz..
	private JsonArray cleanMangoOfferByHand(JsonArray offers) {

		JsonArray newOffers = new JsonArray();
		System.out.println(offers);

		for (JsonElement offerEl : offers) {
			JsonObject offerObj = offerEl.getAsJsonObject();
			double price = Double.parseDouble("239.99");
			offerObj.addProperty(Tag.PRICE.text(), price);
			newOffers.add(offerObj);
			break;
		}
		System.out.println(newOffers + "\n\n");
		return newOffers;

	}

	private JsonArray cleanCurrency(JsonArray offers, String address, String url) {
		String desiredCurreny = getCurrencyMAp().get(address);
		JsonArray newOfferArr = new JsonArray();
		Double tempOffer = 0.00;
		if (url.contains("mango.com")) {

			if (desiredCurreny != null) {
				for (JsonElement offerEl : offers) {
					JsonObject offer = offerEl.getAsJsonObject();
					String curreny = offer.get(Tag.PRICE_CURRENCY.text()).getAsString();
					if (desiredCurreny.equals(curreny)
							&& !tempOffer.equals(offer.get(Tag.PRICE.text()).getAsDouble())) {
						newOfferArr.add(offer);
						tempOffer = offer.get(Tag.PRICE.text()).getAsDouble();

					}
				}
			} else {
				return offers;
			}
		} else if (url.contains("hm.com")) {
			if (desiredCurreny != null) {
				for (JsonElement offerEl : offers) {
					JsonObject offer = offerEl.getAsJsonObject();
					String curreny = offer.get(Tag.PRICE_CURRENCY.text()).getAsString();
					if (!tempOffer.equals(offer.get(Tag.PRICE.text()).getAsDouble())) {
						offer.addProperty(Tag.PRICE_CURRENCY.text(), desiredCurreny);
						newOfferArr.add(offer);
						tempOffer = offer.get(Tag.PRICE.text()).getAsDouble();

					}
				}
			} else {
				return offers;
			}
		} else if (url.contains("reserved")) {
			if (desiredCurreny != null) {
				for (JsonElement offerEl : offers) {
					JsonObject offer = offerEl.getAsJsonObject();
					String curreny = offer.get(Tag.PRICE_CURRENCY.text()).getAsString();
					if (!tempOffer.equals(offer.get(Tag.PRICE.text()).getAsDouble())) {
						offer.addProperty(Tag.PRICE_CURRENCY.text(), desiredCurreny);
						newOfferArr.add(offer);
						tempOffer = offer.get(Tag.PRICE.text()).getAsDouble();

					}
				}
			} else {
				return offers;
			}
		}
		if (!offers.equals(newOfferArr)) {
			System.out.println(url);
			System.out.println(offers);
			System.out.println(newOfferArr + "\n\n");
		}
		return newOfferArr;
	}

	private JsonArray cleanRoRuOffers(JsonArray offers) throws ParseException {
		JsonArray newOffers = new JsonArray();
		Double tempOffer = null;
		System.out.println(offers);

		for (JsonElement offerEl : offers) {
			JsonObject offerObj = offerEl.getAsJsonObject();
			Double price = offerObj.get(Tag.PRICE.text()).getAsDouble();
			if (findWithPattern(price.toString(), "\\.[0-9]{3}", 0) != null) {
				price = price * 1000;
				offerObj.addProperty(Tag.PRICE.text(), price);
			}
			// if (price > 2900) {
			// price = price / 100;
			// offerObj.addProperty(Tag.PRICE.text(), price);
			//
			// }
			// ilk fiyatı koşulsuz ekle
			if (newOffers.size() == 0) {
				tempOffer = price;
				newOffers.add(offerObj);
			} else {
				// bi öncekine eşit değilse ekle.
				if (!tempOffer.equals(offerObj.get(Tag.PRICE.text()).getAsDouble())) {
					tempOffer = price;
					newOffers.add(offerObj);
				}
			}
		}
		System.out.println(newOffers + "\n\n");
		return newOffers;
	}

	private Date deployTime() throws ParseException {
		Date deployTime = format.parse("20.12.2017 00:00");
		return deployTime;
	}

	DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
	DateFormat format2 = new SimpleDateFormat("dd.MM.yyyy");

	private JsonArray fixPricesMango(JsonArray offers, String url) throws Exception {
		JsonArray cleanOffeArray = new JsonArray();
		JsonObject cleanObj = new JsonObject();

		List<String> priceList = new ArrayList<>();
		String currency = null;
		String tempDateStr = null;
		Double tempPrice = null;

		double ilkFiyat = offers.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
		double sonFiyat = offers.get(offers.size() - 1).getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();

		// son > ilk
		if (sonFiyat / ilkFiyat - 1 > 0.5) {
			String currentPrice = findCurrentPrice(url);
			if (currentPrice != null) {
				String[] split = currentPrice.split(" ");
				String firstPrice = split[0];
				String salePrice = split[1];
				if (!firstPrice.equals(salePrice)) {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
					// indirimli fiyatı ekle
					JsonObject secondOfferObj = offers.get(1).getAsJsonObject();
					secondOfferObj.addProperty(Tag.PRICE.text(), salePrice);
					cleanOffeArray.add(secondOfferObj);

				} else {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
				}
			} else {
				JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
				firstOfferObj.addProperty(Tag.PRICE.text(), sonFiyat);
				cleanOffeArray.add(firstOfferObj);
			}

		}
		// ilk > son
		else if (sonFiyat / ilkFiyat - 1 < -0.5) {
			String currentPrice = findCurrentPrice(url);
			if (currentPrice != null) {
				String[] split = currentPrice.split(" ");
				String firstPrice = split[0];
				String salePrice = split[1];
				if (!firstPrice.equals(salePrice)) {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
					// indirimli fiyatı ekle
					JsonObject secondOfferObj = offers.get(1).getAsJsonObject();
					secondOfferObj.addProperty(Tag.PRICE.text(), salePrice);
					cleanOffeArray.add(secondOfferObj);
				} else {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
				}

			}
		} else if (offers.size() > 4) {
			// offer size 4 ten büyükse
			for (JsonElement offer : offers) {
				JsonObject offerObj = offer.getAsJsonObject();
				currency = offerObj.get(Tag.PRICE_CURRENCY.text()).getAsString();
				String priceStr = offerObj.get(Tag.PRICE.text()).getAsString();
				Double price = Double.parseDouble(priceStr);

				// price from areaServed
				String date = offer.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
				// date from areaServed
				Date currentdate = format.parse(date);
				Date oneDayBefore = new Date();
				Calendar c = Calendar.getInstance();
				c.setTime(currentdate);
				c.add(Calendar.DAY_OF_MONTH, -1);
				oneDayBefore = c.getTime();

				String oneDadyBeforeStr = format2.format(oneDayBefore);

				// date from areaServed
				String currentdateStr = format2.format(currentdate);
				if (priceList.size() > 2) {
					priceList = priceList.subList(priceList.size() - 2, priceList.size());
				}
				if (tempDateStr == null) {
					cleanOffeArray.add(offerObj);
					priceList.add(priceStr);
					tempPrice = Double.parseDouble(priceStr);

				} else if (!tempDateStr.equals(currentdateStr) && !tempDateStr.equals(oneDadyBeforeStr)
						&& !priceList.contains(priceStr) && tempPrice / 4 < price && tempPrice * 4 > price
						&& !cleanOffeArray.toString().contains(currentdateStr)) {
					cleanOffeArray.add(offerObj);
					priceList.add(priceStr);
					tempPrice = Double.parseDouble(priceStr);
				}
				tempDateStr = currentdateStr;
			}
		}
		if (cleanOffeArray != null && cleanOffeArray.size() > 0) {
			// fileWriter.write(offers.toString());
			// fileWriter.write("\n");
			// fileWriter.write(cleanOffeArray.toString());
			// fileWriter.write("\n");
			// fileWriter.write(counter1++ + " " + url);
			// fileWriter.write("\n");
			// fileWriter.write("\n");
			// fileWriter.flush();
			System.out.println(offers);
			System.out.println(cleanOffeArray);
			System.out.println(counter1++ + " " + url + "\n");
			return cleanOffeArray;
		}
		return offers;
	}

	private JsonArray fixPricesOffline(JsonArray offers, String url) throws Exception {

		List<String> priceList = new ArrayList<>();
		JsonArray cleanOffeArray = new JsonArray();
		String tempDateStr = null;
		Double tempPrice = null;
		Date tempDate = new Date();
		JsonArray oldTampArray = new JsonParser().parse(offers.toString()).getAsJsonArray();

		for (JsonElement offer : offers) {
			JsonObject offerObj = offer.getAsJsonObject();
			String priceStr = offerObj.get(Tag.PRICE.text()).getAsString();

			Double price = Double.parseDouble(priceStr);
			// 1.79 vs 1799 ikisi de varsa ilkini de 1799 yapıyor. hm için test edildi..
			if (price < 4 && offers.toString().contains("price\":" + price.toString().replace(".", ""))) {
				String findWithPattern = findWithPattern(offers.toString(),
						price.toString().replace(".", "") + "[0-9]*", 0);
				offerObj.addProperty(Tag.PRICE.text(), findWithPattern);

			}
			// 1.297 aslında 1297'dir. Bunu düzeltiyor. hm için test edildi.
			if (findWithPattern(priceStr, "\\.[0-9]{3}", 0) != null) {
				price = price * 1000;
				offerObj.addProperty(Tag.PRICE.text(), price);
			}

			priceStr = offerObj.get(Tag.PRICE.text()).getAsString();
			price = Double.parseDouble(priceStr);
			// price from areaServed
			String date = offer.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
			// date from areaServed
			Date currentdate = format.parse(date);
			Date sevenDayAfter = new Date();
			Calendar c = Calendar.getInstance();
			c.setTime(tempDate);
			c.add(Calendar.DAY_OF_MONTH, +0);
			sevenDayAfter = c.getTime();

			// date from areaServed
			String currentdateStr = format2.format(currentdate);

			if (priceList.size() > 1) {
				priceList = priceList.subList(priceList.size() - 1, priceList.size());
			}
			if (tempDateStr == null) {
				cleanOffeArray.add(offerObj);
				priceList.add(price.toString());
				tempPrice = price;

				tempDateStr = currentdateStr;
				tempDate = currentdate;

			}
			// else if (!tempDateStr.equals(currentdateStr) &&
			// currentdate.after(sevenDayAfter)
			// && !priceList.contains(price.toString()) &&
			// !cleanOffeArray.toString().contains(currentdateStr)
			// && tempPrice / 6 < price && tempPrice * 6 > price) {
			// cleanOffeArray.add(offerObj);
			// priceList.add(price.toString());
			// tempPrice = price;
			//
			// tempDateStr = currentdateStr;
			// tempDate = currentdate;
			// }
			else if (!priceList.contains(price.toString()) && tempPrice / 10 < price && tempPrice * 10 > price) {
				cleanOffeArray.add(offerObj);
				priceList.add(price.toString());
				tempPrice = price;

				tempDateStr = currentdateStr;
				tempDate = currentdate;
			}
		}
		if (cleanOffeArray != null && !cleanOffeArray.equals(oldTampArray)) {
			// fileWrite
			System.out.println(oldTampArray);
			System.out.println(cleanOffeArray);
			System.out.println(counter1++ + " " + url + "\n");
			return cleanOffeArray;
		}
		return offers;
	}

	private JsonArray fixPricesHm(JsonArray offers, String url) throws Exception {
		JsonArray cleanOffeArray = new JsonArray();

		List<String> priceList = new ArrayList<>();
		String tempDateStr = null;
		Double tempPrice = null;

		double ilkFiyat = offers.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();
		double sonFiyat = offers.get(offers.size() - 1).getAsJsonObject().get(Tag.PRICE.text()).getAsDouble();

		// son > ilk
		if (sonFiyat / ilkFiyat - 1 > 0.5) {

			String currentPrice = findCurrentPrice(url);
			if (currentPrice != null) {
				String[] split = currentPrice.split(" ");
				String firstPrice = split[0];
				String salePrice = split[1];
				if (!firstPrice.equals(salePrice)) {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
					// indirimli fiyatı ekle
					JsonObject secondOfferObj = offers.get(1).getAsJsonObject();
					secondOfferObj.addProperty(Tag.PRICE.text(), salePrice);
					cleanOffeArray.add(secondOfferObj);

				} else {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
				}
			} else {
				JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
				firstOfferObj.addProperty(Tag.PRICE.text(), sonFiyat);
				cleanOffeArray.add(firstOfferObj);
			}

		}
		// ilk > son
		else if (sonFiyat / ilkFiyat - 1 < -0.5) {
			String currentPrice = findCurrentPrice(url);
			if (currentPrice != null) {
				String[] split = currentPrice.split(" ");
				String firstPrice = split[0];
				String salePrice = split[1];
				if (!firstPrice.equals(salePrice)) {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
					// indirimli fiyatı ekle
					JsonObject secondOfferObj = offers.get(1).getAsJsonObject();
					secondOfferObj.addProperty(Tag.PRICE.text(), salePrice);
					cleanOffeArray.add(secondOfferObj);
				} else {
					// ilk fiyatı ekle
					JsonObject firstOfferObj = offers.get(0).getAsJsonObject();
					firstOfferObj.addProperty(Tag.PRICE.text(), firstPrice);
					cleanOffeArray.add(firstOfferObj);
				}

			}
		} else if (offers.size() > 4) {
			// offer size 4 ten büyükse
			for (JsonElement offer : offers) {
				JsonObject offerObj = offer.getAsJsonObject();
				String priceStr = offerObj.get(Tag.PRICE.text()).getAsString();
				Double price = Double.parseDouble(priceStr);

				// price from areaServed
				String date = offer.getAsJsonObject().get(Tag.VALIDFROM.text()).getAsString();
				// date from areaServed
				Date currentdate = format.parse(date);
				Date oneDayBefore = new Date();
				Calendar c = Calendar.getInstance();
				c.setTime(currentdate);
				c.add(Calendar.DAY_OF_MONTH, -1);
				oneDayBefore = c.getTime();

				String oneDadyBeforeStr = format2.format(oneDayBefore);

				// date from areaServed
				String currentdateStr = format2.format(currentdate);
				if (priceList.size() > 2) {
					priceList = priceList.subList(priceList.size() - 2, priceList.size());
				}
				if (tempDateStr == null) {
					cleanOffeArray.add(offerObj);
					priceList.add(priceStr);
					tempPrice = Double.parseDouble(priceStr);

				} else if (!tempDateStr.equals(currentdateStr) && !tempDateStr.equals(oneDadyBeforeStr)
						&& !priceList.contains(priceStr) && tempPrice / 4 < price && tempPrice * 4 > price
						&& !cleanOffeArray.toString().contains(currentdateStr)) {
					cleanOffeArray.add(offerObj);
					priceList.add(priceStr);
					tempPrice = Double.parseDouble(priceStr);
				}
				tempDateStr = currentdateStr;
			}
		}
		if (cleanOffeArray != null && cleanOffeArray.size() > 0) {
			System.out.println(offers);
			System.out.println(cleanOffeArray);
			System.out.println(counter1++ + " " + url + "\n");
			return cleanOffeArray;
		}
		return offers;
	}

	private String name;
	private Agent agent;
	MangoExtractor mangoExtractor = new MangoExtractor(name, agent);
	HMExtractor hmExtractor = new HMExtractor(name, agent);
	AbstractFashionExtractor afe = new AbstractFashionExtractor(name, agent) {

		@Override
		protected String extract(String pageContent, String trackedUri, Boolean isNewUri)
				throws ImagelessProductException {
			// TODO Auto-generated method stub
			return null;
		}
	};

	public String findCurrentPrice(String url) throws Exception {
		String content = null;
		try {
			if (url.contains("mango") && !url.contains("https")) {
				url = url.replace("http", "https");
			}
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");
			if (url.contains("mango")) {
				Thread.sleep(2000);
			} else {
				Thread.sleep(500);
			}

			// int responseCode = con.getResponseCode();
			// // yönlendirme varsa gittigi url
			// URL redirectURL = con.getURL();

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			content = response.toString();
		} catch (Exception e) {
			System.out.println(e);
		}
		String price = null;
		if (url.contains("hm.com") && !content.toString().contains("Ürün bulunamadı")
				&& !content.toString().contains("No product found") && !content.toString().contains("No items found")) {

			Document doc = Jsoup.parse(content);
			doc.outputSettings().escapeMode(EscapeMode.xhtml);

			// fiyatı cıkarıyor salePrice
			// String firstPrice = hmExtractor.extractPrice(doc, "old");
			// if (firstPrice != null) {
			// JsonArray currentOffeArr = new
			// JsonParser().parse(firstPrice).getAsJsonArray();
			// firstPrice =
			// currentOffeArr.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsString();
			// }
			// String salePrice = hmExtractor.extractPrice(doc, "actual-price new");
			// if (salePrice != null) {
			// JsonArray currentOffeArr = new
			// JsonParser().parse(salePrice).getAsJsonArray();
			// salePrice =
			// currentOffeArr.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsString();
			// }
			// price = firstPrice + " " + salePrice;
		}
		if (content != null && url.contains("mango")) {

			Document doc = Jsoup.parse(content);
			doc.outputSettings().escapeMode(EscapeMode.xhtml);

			Elements pageTypeElement = doc.select("meta[property=og:type]");
			if (pageTypeElement != null && !pageTypeElement.isEmpty()) {
				String pageType = pageTypeElement.attr("content");
				if (pageType.equals("article")) {
					// fiyatı cıkarıyor salePrice
					// String firstPrice =
					// mangoExtractor.extractPrice(mangoExtractor.extractScriptJon(doc),
					// "originalPrice");
					// if (firstPrice != null) {
					// JsonArray currentOffeArr = new
					// JsonParser().parse(firstPrice).getAsJsonArray();
					// firstPrice =
					// currentOffeArr.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsString();
					// }
					// String salePrice =
					// mangoExtractor.extractPrice(mangoExtractor.extractScriptJon(doc),
					// "salePrice");
					// if (salePrice != null) {
					// JsonArray currentOffeArr = new
					// JsonParser().parse(salePrice).getAsJsonArray();
					// salePrice =
					// currentOffeArr.get(0).getAsJsonObject().get(Tag.PRICE.text()).getAsString();
					// }
					// price = firstPrice + " " + salePrice;
				}
			}
		}

		return price;
	}
}