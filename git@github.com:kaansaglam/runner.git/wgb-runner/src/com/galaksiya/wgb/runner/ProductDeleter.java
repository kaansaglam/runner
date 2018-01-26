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
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import com.galaksiya.util.elasticsearch.ElasticSearchClient;
import com.galaksiya.util.elasticsearch.ElasticSearchConfig;

import wgb.io.Tag;

public class ProductDeleter {
	private static final int BULK_SIZE = 1000;
	private static final Logger logger = LogManager.getLogger(ProductDeleter.class);

	public static void main(String args[]) {
		new ProductDeleter().execute();
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
		String path = "/home/galaksiya/zara.csv";
		try (Stream<String> stream = Files.lines(Paths.get(path), Charset.forName("ISO_8859_1"))) {
			logger.info("... STARTED ...");
			stream.forEach(line -> {

				String[] fieldList = line.split("~");
				String id = fieldList[0];
				System.out.println(counter);
				counter++;
				if (!id.contains("/es/en")) {
					System.out.println(id);
					prepateDeleteBulkObj(id);
				}
				if (counter % BULK_SIZE == 1 && counter > 1) {
					System.out.println("SENDED BULK:" + bulk.numberOfActions() + " " + counter);
					bulk.get();
					bulk = getESTransportClient().prepareBulk();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("SENDED BULK:" + bulk.numberOfActions());
		bulk.get();
		bulk = getESTransportClient().prepareBulk();
		System.out.println("total product size" + counter);
		logger.info("DONE!");
		System.exit(0);
	}

	private void prepateDeleteBulkObj(String id) {
		DeleteRequestBuilder deleteReq = getESTransportClient()
				.prepareDelete(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, id).setId(id);
		bulk.add(deleteReq);
	}
}
