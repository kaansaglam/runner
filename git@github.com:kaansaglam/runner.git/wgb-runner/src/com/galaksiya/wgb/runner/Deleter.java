package com.galaksiya.wgb.runner;

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
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Deleter {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(Deleter.class);

	public static void main(String args[]) {
		new Deleter().execute();
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
		String path = "/home/galaksiya/zara-22-kasım.json";
		try (Stream<String> stream = Files.lines(Paths.get(path), Charset.forName("ISO_8859_1"))) {
			logger.info("... STARTED ...");
			try {
				stream.forEach(line -> {
					JsonObject asJsonObject = new JsonParser().parse(line).getAsJsonObject().get("_source")
							.getAsJsonObject().get("allDocument").getAsJsonObject();

					String id = asJsonObject.get("id").getAsString();
					String secondary = null;
					if (asJsonObject.has("secondaryId")) {
						secondary = asJsonObject.get("secondaryId").getAsString();
					}

					if (id.length() > 200) {
						System.out.println(id);
						System.out.println(secondary);
						 prepateDeleteBulkObj(id);
						counter++;
					}

					if (counter % BULK_SIZE == 1 && counter > 1) {
						System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
//						bulk.get();
//						bulk = getESTransportClient().prepareBulk();
						counter++;
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});
			} catch (Exception e) {
				logger.error("could not load", e);
			}
			System.out.println("SENDED BULK:" + bulk.numberOfActions());
//			bulk.get();
//			bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
		} catch (

		IOException e) {
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private void prepateDeleteBulkObj(String id) {
		DeleteRequestBuilder deleteReq = getESTransportClient()
				.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id).setId(id);
		bulk.add(deleteReq);
	}
}
