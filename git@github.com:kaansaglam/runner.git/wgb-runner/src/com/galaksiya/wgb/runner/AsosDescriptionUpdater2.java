package com.galaksiya.wgb.runner;
 
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
 
import wgb.io.Tag;
 
public class AsosDescriptionUpdater2 {
    private static final String CHECK = "check";
    private static final int BULK_SIZE = 1000;
    private static final Logger logger = LogManager.getLogger(AsosDescriptionUpdater2.class);
 
    public static void main(String args[]) {
        new AsosDescriptionUpdater2().execute();
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
    List<String> secondaryIdList = new ArrayList<>();
 
    private void execute() {
        try (Stream<String> stream = Files.lines(Paths.get("/home/galaksiya/asos-10-ara.json"),
                Charset.forName("UTF-8"))) {
            try {
                stream.forEach(line -> {
                    JsonObject allDocumentObj = extractAllDocumentObj(line);
                    String id = extractProdId(allDocumentObj);
                    String oldDescription = extractOldDescription(allDocumentObj);
                    Boolean expected = controlDescription(oldDescription);
                    if (expected) {
                        // get new description info
                        String newDescription = correctDescriptionInfo(oldDescription);
                        if (newDescription != null && !newDescription.isEmpty()) {
                            prepareLoadBulkObj(id, newDescription);
                            System.err.println(counter++ + " " + newDescription + "\n" + oldDescription);
                        }
                        if (counter % BULK_SIZE == 1 && counter > 1) {
                            System.out.println("SENDED BULK:" + bulk.numberOfActions() + " total product: " + counter);
                            bulk.get();
                            bulk = getESTransportClient().prepareBulk();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                logger.error("could not load", e);
            }
            System.out.println("SENDED BULK:" + bulk.numberOfActions());
            bulk.get();
            bulk = getESTransportClient().prepareBulk();
            System.out.println("total product size" + counter);
        } catch (IOException e) {
            logger.error("error while reading file", e);
        }
        System.exit(0);
    }
 
    private JsonObject extractAllDocumentObj(String line) {
        JsonObject allDocumentObj = new JsonObject();
        if (line != null && !line.isEmpty()) {
            JsonObject productObj = new JsonParser().parse(line).getAsJsonObject();
            if (productObj != null && !productObj.isJsonNull()) {
                JsonElement sourceEl = productObj.get("_source");
                JsonObject sourceObj = sourceEl.getAsJsonObject();
                allDocumentObj = sourceObj.get(ElasticSearchClient.PROPERTY_REAL_DOCUMENT).getAsJsonObject();
            }
        }
        return allDocumentObj;
    }
 
    private String extractProdId(JsonObject allDocument) {
        return allDocument.get("id").getAsString();
    }
 
    private static final String FIRST_DESCRIPTION_REGEX = "product details(.*)product code";
    private static final String SECOND_DESCRIPTION_REGEX = "product code(.*)";
 
    private Boolean controlDescription(String description) {
        if (description != null && !description.isEmpty()) {
            String firstRegexStr = findWithPattern(description, FIRST_DESCRIPTION_REGEX, 1);
            String secondRegexStr = findWithPattern(description, SECOND_DESCRIPTION_REGEX, 1);
            if (firstRegexStr != null && !firstRegexStr.contains(CHECK) && secondRegexStr != null
                    && secondRegexStr.contains(CHECK)) {
                return true;
            }
        }
        return false;
    }
 
    private String extractOldDescription(JsonObject allDocumentObj) {
        String desription = null;
        if (allDocumentObj != null && !allDocumentObj.isJsonNull()) {
            JsonElement descriptionEl = allDocumentObj.get(Tag.DESCRIPTION.text());
            if (descriptionEl != null && !descriptionEl.isJsonNull()) {
                desription = descriptionEl.getAsString().toLowerCase();
            }
        }
        return desription;
    }
 
    private String correctDescriptionInfo(String description) {
        if (description != null && !description.isEmpty()) {
            description = description.replace(CHECK, "");
        }
        return description;
    }
 
    protected String findWithPattern(String uri, String pattern, Integer groupNo) {
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(uri);
        if (m.find()) {
            return m.group(groupNo);
        }
        return null;
    };
 
    private void prepareLoadBulkObj(String prodId, String newDescription) {
        try {
            XContentBuilder content = XContentFactory.jsonBuilder().startObject()
                    .startObject(ElasticSearchClient.PROPERTY_REAL_DOCUMENT);
            content.field(Tag.DESCRIPTION.text(), newDescription).endObject().endObject();
            UpdateRequestBuilder updateReq = getESTransportClient()
                    .prepareUpdate(ElasticSearchClient.INDEX_GARMENT, ElasticSearchClient.TYPE_PRODUCT, prodId)
                    .setDoc(content);
            bulk.add(updateReq);
            counter++;
//            System.out.println(content.string());
        } catch (IOException e) {
            e.printStackTrace();
        }
 
    }
 
}