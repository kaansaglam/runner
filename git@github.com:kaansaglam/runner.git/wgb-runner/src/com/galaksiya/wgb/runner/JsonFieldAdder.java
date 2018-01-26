package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class JsonFieldAdder extends RunnerUtil {

	public static void main(String args[]) {
		new JsonFieldAdder().execute();
	}

	private BulkRequestBuilder bulk = getESTransportClient().prepareBulk();

	private int counter = 0;
	private int prodIdNo = 10191;

	private void execute() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/zara-05-kasım1.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();

					// 23.05.2017 19:40
					JsonObject _source = jsonObj.get("_source").getAsJsonObject();
					String dateStr = _source.get("datePublished").getAsString();
					DateFormat format = new SimpleDateFormat("dd.MM.yyyy hh:mm");
					Date date = format.parse(dateStr);
					Date today = format.parse("30.10.2017 00:00");
					JsonObject allDoc = _source.get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();

					JsonArray areaServedArr = allDoc.get(Tag.AREA_SERVED.text()).getAsJsonArray();
					List<String> countryList = new ArrayList<>();
					// for (JsonElement areaSrv : areaServedArr) {
					String country = areaServedArr.getAsJsonObject().get(Tag.URL.text()).getAsString();
					countryList.add(country);
					//
					// }
					if (id.contains("title") || areaServedArr.toString().contains("title")) {
						System.out.println(_source);
					}
					//
					// if (date.after(today) && countryList.contains("TR")) {
					// counter++;
					// prodIdNo++;
					// System.out.println(prodIdNo + " " + id + " " + date);
					// prepareLoadBulkObjFromExcel(id, prodIdNo);
					// }
					//
					// // prepareLoadBulkObj(id);
					// if (counter % BULK_SIZE == 1 && counter > 1) {
					// System.out.println("SENDED BULK:" +
					// bulk.numberOfActions() + " " + counter);
					// bulk.get();
					// bulk = getESTransportClient().prepareBulk();
					// Thread.sleep(10000);
					// break;
					// }

				}
			}

			addFiledFromExcel();

			// System.out.println("SENDED BULK:" + bulk.numberOfActions());
			// bulk.get();
			// bulk = getESTransportClient().prepareBulk();
			// System.out.println("total product size" + counter);
		} catch (

		Exception e) {
			e.printStackTrace();
		}
	}

	int prodId = 1;

	private void prepareLoadBulkObj(String id) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			content.field("prodId", prodIdNo);
			content.endObject().endObject();

			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private HashMap<String, String> addFiledFromExcel()
			throws FileNotFoundException, IOException, InterruptedException {
		BufferedReader br = null;
		String sCurrentLine;
		HashMap<String, String> prodIdMap = new HashMap<>();
		br = new BufferedReader(new FileReader("/home/galaksiya/zara-prodId.txt"));
		while ((sCurrentLine = br.readLine()) != null) {
			String[] split = sCurrentLine.split("	");
			String duplicate = prodIdMap.get(split[1]);
			if (duplicate == null) {
				// prepareLoadBulkObjFromExcel(split[1], split[0]);
				counter++;
				if (counter % BULK_SIZE == 1 && counter > 1) {
					// System.out.println("SENDED BULK:" +
					// bulk.numberOfActions() + " " + counter);
					// bulk.get();
					// bulk = getESTransportClient().prepareBulk();
					// Thread.sleep(2000);
				}
				prodIdMap.put(split[1], split[0]);

			} else {
				System.out.println(counter + "duplicate");
			}
		}
		br.close();
		return prodIdMap;
	}

	private void prepareLoadBulkObjFromExcel(String id, int prodId) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			// int parseInt = Integer.parseInt(prodId);
			content.field("prodId", prodId);
			// System.out.println(prodId + " " + id);
			content.endObject().endObject();
			// id = id.replace("/tr/", "/es/");
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id)
					.setDoc(content);
			bulk.add(updateReq);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}