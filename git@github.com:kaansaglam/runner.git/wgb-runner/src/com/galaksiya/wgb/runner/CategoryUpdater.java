package com.galaksiya.wgb.runner;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import wgb.io.Tag;

public class CategoryUpdater {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(CategoryUpdater.class);

	public static void main(String args[]) {
		new CategoryUpdater().execute();
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

		try (BufferedReader br = new BufferedReader(new FileReader("/home/galaksiya/hm-12-ekim.json"))) {
			for (String line; (line = br.readLine()) != null;) {
				if (!line.isEmpty()) {

					JsonObject jsonObj = new JsonParser().parse(line).getAsJsonObject();
					JsonObject allDoc = jsonObj.get("_source").getAsJsonObject().get("allDocument").getAsJsonObject();
					String id = allDoc.get("id").getAsString();
					if (allDoc.has("title")&& allDoc.has("brand")) {
						String title = allDoc.get("title").getAsString();
						String brand = allDoc.get("brand").getAsString();
						
						if (brand.contains(title)) {
							counter++;

							System.out.println(id);
						prepareLoadBulkObj(id);
						}
					}
					


					if (counter % BULK_SIZE == 1 && counter > 1) {
						System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
						bulk.get();
						bulk = getESTransportClient().prepareBulk();
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			bulk.get();
			bulk = getESTransportClient().prepareBulk();
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

	private void prepareLoadBulkObj(String prodId) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject()
					.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);

			content.field(Tag.BRAND.text(), "H&M");

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

}
