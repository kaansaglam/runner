package com.galaksiya.wgb.runner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import org.omg.IOP.TAG_ALTERNATE_IIOP_ADDRESS;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;

import wgb.io.Tag;

public class LastSeenAdder {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(LastSeenAdder.class);

	public static void main(String args[]) {
		new LastSeenAdder().execute();
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
		String path = "/home/galaksiya/topman-8-mayıs.csv";
		try (Stream<String> stream = Files.lines(Paths.get(path))) {
			logger.info("... STARTED ...");
			stream.forEach(line -> {
				try {
					counter++;
					String[] fieldList = line.split("~");
					String id = fieldList[0];
					String date = fieldList[1];

					prepareLoadBulkObj(id, date);

					if (counter % BULK_SIZE == 1 && counter > 1) {
						System.out.println("SENDED BULK:" + bulk.numberOfActions() + "  " + counter);
						bulk.get();
						bulk = getESTransportClient().prepareBulk();
						Thread.sleep(2000);
					}
				} catch (Exception e) {
					logger.error("could not load", e);
				}
			});
			// writing to file for analyze operation

			System.out.println("SENDED BULK:" + bulk.numberOfActions());
			bulk.get();
			bulk = getESTransportClient().prepareBulk();
			System.out.println("total product size" + counter);
		} catch (IOException e) {
			logger.error("error while reading file", e);
		}
		logger.info("DONE!");
		System.exit(0);
	}

	private void prepareLoadBulkObj(String prodId, String date) {
		try {
			XContentBuilder content = XContentFactory.jsonBuilder().startObject();
			content.field(Tag.LASTSEEN.text(), date);
			content.field(Tag.ONLINE.text(), true);
			content.startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
			content.field(Tag.LASTSEEN.text(), date);
			content.field(Tag.ONLINE.text(), true);
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
