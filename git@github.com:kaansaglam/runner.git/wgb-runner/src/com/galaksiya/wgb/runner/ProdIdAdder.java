package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.agent.Agent;
import com.galaksiya.extractor.fashion.AbstractFashionExtractor;
import com.galaksiya.extractor.fashion.HMExtractor;
import com.galaksiya.extractor.fashion.ImagelessProductException;
import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class ProdIdAdder extends RunnerUtil {

	public static void main(String args[]) throws FileNotFoundException, IOException, InterruptedException {

		new ProdIdAdder().execute();
		// new ProdIdAdder().addFiledFromExcel();
	}

	private int counter = 0;
	private int prodIdNo = 181;

	private void execute() {
		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/hm-21-ara-prodId.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				// try {
				if (!line.isEmpty()) {
					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();

					String id = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject().get("id")
							.getAsString();

					JsonElement jsonElement = jsonObj.get("_source").getAsJsonObject().get("allDocument")
							.getAsJsonObject().get("prodId");
					String prodId = null;
					if (jsonElement != null) {
						prodId = jsonElement.getAsString();
					}
					//
					if (prodId == null || prodId.isEmpty()) {
						counter++;
						prodIdNo++;
						prepareLoadBulkObj(id);
					} else {
						System.out.println("zate prodId var" + id);
					}
					//
					// prepareLoadBulkObj(id);
					if (counter % BULK_SIZE == 1 && counter > 1) {
						sendBulk();
					}

				}
			}

			// addFiledFromExcel();

			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			sendBulk();
			System.out.println("total product size" + counter);
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
			System.out.println(content.string() + "  " + id);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private HashMap<String, String> addFiledFromExcel()
			throws FileNotFoundException, IOException, InterruptedException {
		BufferedReader br = null;
		String sCurrentLine;
		int a = 11603;
		HashMap<String, String> prodIdMap = new HashMap<>();
		br = new BufferedReader(new FileReader("/home/galaksiya/hmpProdId.txt"));
		while ((sCurrentLine = br.readLine()) != null) {
			// prepareLoadBulkObjElle(sCurrentLine, a++);

			String[] split = sCurrentLine.split("	");
			String duplicate = prodIdMap.get(split[1]);
			if (duplicate == null) {
				prodIdMap.put(split[1], split[0]);
				int parseInt = Integer.parseInt(split[0]);

				prepareLoadBulkObjFromExcel(split[1], parseInt);
				counter++;
				System.out.println(counter + "" + split[1] + " " + split[0]);

			} else {
				// System.out.println(counter + " " + split[1]);
			}
		}
		br.close();

		System.out.println("SENDED BULK:" + bulk.numberOfActions());
		sendBulk();
		System.out.println("total product size" + counter);
		System.err.println(error);
		return null;
	}

	private String name;
	private Agent agent;
	AbstractFashionExtractor afe = new AbstractFashionExtractor(name, agent) {

		@Override
		protected String extract(String pageContent, String trackedUri, Boolean isNewUri)
				throws ImagelessProductException {
			// TODO Auto-generated method stub
			return null;
		}
	};
	List<String> error = new ArrayList<>();

	private void prepareLoadBulkObjFromExcel(String id, int prodId) {
		try {

			JsonObject fromElasticSearch = new JsonObject();
//			fromElasticSearch =afe.getAreaServed(id, null);
			if (fromElasticSearch != null && !fromElasticSearch.isJsonNull()
					&& fromElasticSearch.toString().length() > 5) {
				id = fromElasticSearch.get(Tag.ID.text()).getAsString();
				XContentBuilder content = XContentFactory.jsonBuilder().startObject();

				content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
				// int parseInt = Integer.parseInt(prodId);
				content.field("prodId", prodId);
				// System.out.println(prodId + " " + id);
				content.endObject().endObject();
				UpdateRequestBuilder updateReq = getESTransportClient()
						.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id)
						.setDoc(content);
				bulk.add(updateReq);
				System.out.println(content.string() + " " + id);
			} else {
				error.add(id);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void prepareLoadBulkObjElle(String id, int prodId) {
		try {

			XContentBuilder content = XContentFactory.jsonBuilder().startObject();

			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			// int parseInt = Integer.parseInt(prodId);
			content.field("prodId", prodId);
			// System.out.println(prodId + " " + id);
			content.endObject().endObject();
			UpdateRequestBuilder updateReq = getESTransportClient()
					.prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id)
					.setDoc(content);
			bulk.add(updateReq);
			System.out.println(content.string() + "  " + id);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}