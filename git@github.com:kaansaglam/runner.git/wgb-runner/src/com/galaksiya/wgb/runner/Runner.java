package com.galaksiya.wgb.runner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;

import com.galaksiya.agent.Agent;
import com.galaksiya.agent.communication.Message;
import com.galaksiya.agent.communication.Performative;
import com.galaksiya.agent.communication.protocol.Protocol;
import com.galaksiya.agent.role.Role;
import com.galaksiya.agent.role.RoleKnowledge;
import com.galaksiya.agent.role.action.Action;
import com.galaksiya.extractor.ExtractorAgent;
import com.galaksiya.retriever.RetrieverAgent;
import com.galaksiya.sprinkler.SprinklerAgent;
import com.galaksiya.tagger.TaggerAgent;
import com.galaksiya.tracker.TrackerAgent;
import com.galaksiya.tracker.TrackerList;
import com.galaksiya.util.FashionRegex;
import com.galaksiya.util.TrackerInfo;
import com.galaksiya.util.client.WGBClient;
import com.galaksiya.wgb.reporter.ReporterAgent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import wgb.io.BasedOn;
import wgb.io.GraphConstants;
import wgb.io.JsonFields;
import wgb.io.Tag;

public class Runner {
	private static PipelineEndRole role = null;
	private static final String PIPELINE_END_ROLE = wgb.io.roles.tagger.Roles.SEMANTIC_TAGGER_URI;

	private static final String PIPELINE_END_ACTION = wgb.io.actions.tagger.Actions.TAG;

	private static final String TRACKER_AGENT_URI = "http://localhost:8081/";
	public final static String CUSTODIAN_AGENT_URI = "http://custodian.wgb.io:8085/";

	public static void main(String[] args) throws Exception {

		new Runner().runScenario();
	}

	public void runScenario() {
		try {
			// start pipeline end agent...
			// startPipelineEndAgent();
			HttpClient client = new HttpClient();
			client.start();
			String[] arguments = {};
			SprinklerAgent.main(arguments);
			Thread.sleep(1000);
			RetrieverAgent.main(new String[] { "queue" });
			Thread.sleep(1000);
			ExtractorAgent.main(arguments);
			Thread.sleep(1000);
			TrackerAgent.main(arguments);
			// CustodianAgent.main(arguments);
			Thread.sleep(1000);
			TaggerAgent.main(arguments);
			Thread.sleep(1000);

			registerFollowl(client);
			// registerCustodian(client);
			// CollectorAgent.main(arguments);
			// Thread.sleep(1000);
			ReporterAgent.main(arguments);
			Thread.sleep(1000);
			registerReporter(client);

			// String name = null;
			// Agent agent = null;
			//
			// ElasticEngine engine = new ElasticEngine(name, agent);
			// engine.handleProductsWithBrokenImages(ElasticSearchClient.getInstance().findProductsWithBrokenImages(4,
			// 0,
			// BasedOn.ISABEL_MARANT.toString()),
			// ElasticSearchUtil.getFileName(4, 0));

			fashionStartMethods(client);

			rssStartMethods(client);

			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void fashionStartMethods(HttpClient client)
			throws InterruptedException, TimeoutException, ExecutionException {

		// startSiteMapTracker(client, wgb.io.roles.tracker.Roles.BIKBOK_TRACKER_URI,
		// TrackerList.BIKBOK_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.BIKBOK.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.BIKBOK_EXTRACTOR_URI, null);

		// startSiteMapTracker(client, wgb.io.roles.tracker.Roles.NETWORK_TRACKER_URI,
		// TrackerList.NETWORK_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.NETWORK.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.NETWORK_EXTRACTOR_URI, null);

		// startSiteMapTracker(client, wgb.io.roles.tracker.Roles.RESERVED_TRACKER_URI,
		// TrackerList.RESERVED_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.RESERVED.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.RESERVED_EXTRACTOR_URI, null);

		// startTracker(client, wgb.io.roles.tracker.Roles.HM_JSON_TRACKER,
		// "[\"http://www2.hm.com/tr_tr/kadin/urune-gore-satin-al/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.HM.toString(),
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.HM_EXTRACTOR_URI);

		// startTracker(client, wgb.io.roles.tracker.Roles.HM_JSON_TRACKER,
		// "[\"http://www2.hm.com/ru_ru/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.HM.toString(),
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.HM_EXTRACTOR_URI);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.MANGO_CLOTHING_TRACKER,
		// TrackerList.MANGO_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.MANGO.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.MANGO_EXTRACTOR_URI, null);

		// startSiteMapTracker(client, wgb.io.roles.tracker.Roles.DEFACTO_TRACKER_URI,
		// TrackerList.DEFACTO_LIST.toString(),
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.DEFACTO.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.DEFACTO_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.LCW_TRACKER_URI,
		// "[\"https://www.lcw.com/en-UK/sitemap\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.LCW.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.LCW_EXTRACTOR_URI, null);

		// "http://www.lcwaikiki.com/tr-TR/TR"
		// "[\"https://www.lcw.com/en-UK/sitemap\"]"

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.ZALANDO_TRACKER_URI,
		// TrackerList.ZALANDO_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.ZALANDO.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.ZALANDO_EXTRACTOR_URI, null);
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.LUCY_TRACKER_URI,
		// TrackerList.LUCY_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.LUCY.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.LUCY_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.ALO_YOGA_TRACKER_URI,
		// TrackerList.ALO_YOGA_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.ALO_YOGA.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.ALO_YOGA_EXTRACTOR_URI,
		// FashionTrackerRegex.ALO_YOGA_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.AVOCADO_TRACKER_URI,
		// TrackerList.AVOCADO_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.AVOCADO.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.AVOCADO_EXTRACTOR_URI,
		// FashionTrackerRegex.AVOCADO_REGEX);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.LULULEMON_TRACKER_URI,
		// TrackerList.LULULEMON_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.LULULEMON.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.LULULEMON_EXTRACTOR_URI, null);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.NIKE_TRACKER_URI,
		// "[\"http://store.nike.com/gb/en_gb/\"]", TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.NIKE.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.NIKE_EXTRACTOR_URI, null);
		//
		// startSiteMapTracker(client,
		// // startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.YAYA_TRACKER_URI,
		// "[\"http://www.yaya.nl/shop/en/129-stripe-out\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.YAYA.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.YAYA_EXTRACTOR_URI, null);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.MAVI_TRACKER_URI,
		// "[\"https://www.mavi.com/kadin/c/1?pageSize=0\"]", TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.MAVI.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.MAVI_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.VEROMODA_TRACKER_URI,
		// "[\"https://www.veromoda.com/on/demandware.static/-/Library-Sites-bestseller-content-library/da_DK/dwf42e5f42/affiliatefeeds/product-sitemap/Sitemap_vm_0_BSE-DK.xml.gz\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.VEROMODA.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.VEROMODA_EXTRACTOR_URI,
		// FashionTrackerRegex.VERO_MODA_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.ONLY_TRACKER_URI,
		// "[\"https://www.only.com/on/demandware.static/-/Library-Sites-bestseller-content-library/da_DK/dw3c24baf7/affiliatefeeds/product-sitemap/Sitemap_on_6_BSE-DK.xml.gz\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.ONLY.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.ONLY_EXTRACTOR_URI,
		// FashionTrackerRegex.ONLY_REGEX);

		startSiteMapTracker(client, wgb.io.roles.tracker.Roles.KOTON_TRACKER_URI,
				TrackerList.KOTON_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
				new TrackerInfo(BasedOn.KOTON.toString(), GraphConstants.ENGLISH),
				wgb.io.roles.extractor.Roles.KOTON_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.PRIMARK_TRACKER_URI,
		// TrackerList.PRIMARK_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.PRIMARK.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.PRIMARK_EXTRACTOR_URI,
		// FashionTrackerRegex.PRIMARK_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.CASUAL_FASHION_TRACKER_URI,
		// "[\"https://www.casual-fashion.com/en_nl/someday\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.CASUAL_FASHION.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.CASUAL_FASHION_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.GERRY_WEBER_TRACKER_URI,
		// TrackerList.GERRY_WEBER_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.GERRY_WEBER.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.GERRY_WEBER_EXTRACTOR_URI, null);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.TAIFUN_TRACKER_URI,
		// TrackerList.TAIFUN_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.TAIFUN.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.TAIFUN_EXTRACTOR_URI, null);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.SAMOON_TRACKER_URI,
		// TrackerList.SAMOON_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.SAMOON.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.SAMOON_EXTRACTOR_URI, null);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.MARKAFONI_TRACKER_URI,
		// "[\"http://www.markafoni.com/\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.MARKAFONI.toString(),
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.MARKAFONI_EXTRACTOR_URI);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.NORDSTROM_TRACKER_URI,
		// "[\"http://shop.nordstrom.com/api/c/mens-whats-new\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.NORDSTORM.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.NORDSTROM_EXTRACTOR_URI);

		// startTracker(client, wgb.io.roles.tracker.Roles.MORHIPO_TRACKER,
		// "[\"https://www.morhipo.com/sitemap/product-sitemap3.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.MORHIPO.toString(), "breakingnews",
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.MORHIPO_EXTRACTOR_URI);

		// startTracker(client, wgb.io.roles.tracker.Roles.TRENDYOL_TRACKER_URI,
		// TrackerList.TRENDYOL_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.TRENDYOL.toString(), GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.TRENDYOL_EXTRACTOR_URI);

		//
		// startTracker(client, wgb.io.roles.tracker.Roles.LESARA_TRACKER_URI,
		// "[\"https://www.lesara.com/women?sort=new/\"]", TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.LESARA.toString(), "breakingnews",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.LESARA_EXTRACTOR_URI);
		//
		// startTracker(client, wgb.io.roles.tracker.Roles.LESARA_TRACKER_URI,
		// "[\"https://www.lesara.com/kids?sort=new/\"]", TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.LESARA.toString(), "breakingnews",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.LESARA_EXTRACTOR_URI);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.ASOS_TRACKER_URI,
		// "[\"http://www.asos.com/sitemap.ashx?InventoryGroupFilter=A\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.ASOS.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.ASOS_EXTRACTOR_URI, null);
		// // //
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.NORDSTROM_TRACKER_URI,
		// "[\"http://shop.nordstrom.com/sitemap_product_0.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://shop.nordstrom.com", "product",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.NORDSTROM_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.NET_A_PORTER_TRACKER_URI,
		// "[\"https://www.net-a-porter.com/us/en/d/Shop/Clothing/All\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.NET_A_PORTER.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.NET_A_PORTER_EXTRACTOR_URI, null);
		// String arr =
		// "[\"tr\",\n\"hr\",\n\"no\",\n\"us\",\n\"do\",\n\"pa\",\n\"qa\",\n\"al\",\n\"cz\",\n\"bg\",\n\"ro\",\n\"pl\",\n\"pt\",\n\"ae\",\n\"is\",\n\"fr\",\n\"id\",\n\"ni\",\n\"nl\",\n\"xe\",\n\"gt\",\n\"me\",\n\"mk\",\n\"cr\",\n\"fi\",\n\"ca\",\n\"jo\",\n\"ic\",\n\"kz\",\n\"lv\",\n\"kr\",\n\"at\",\n\"eg\",\n\"ch\",\n\"ec\",\n\"sv\",\n\"de\",\n\"om\",\n\"dk\",\n\"sa\",\n\"gr\",\n\"ee\",\n\"ru\",\n\"rs\",\n\"bh\",\n\"ad\",\n\"sk\",\n\"co\",\n\"lu\",\n\"lb\",\n\"it\",\n\"hu\",\n\"ve\",\n\"in\",\n\"dz\",\n\"lt\",\n\"am\",\n\"se\",\n\"jp\",\n\"ph\",\n\"ba\",\n\"tw\",\n\"az\",\n\"ma\",\n\"si\",\n\"mx\",\n\"ge\",\n\"aw\",\n\"hn\",\n\"il\",\n\"uk\",\n\"kw\",\n\"cy\",\n\"es\",\n\"mt\",\n\"tn\",\n\"be\",\n\"mc\",\n\"ua\",\n\"ie\"]";
		// JsonArray array = new JsonParser().parse(arr).getAsJsonArray();
		// for (JsonElement country : array) {

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.COMMA_TRACKER_URI,
		// "[\"http://www.comma-store.eu/sitemap_0.xml\"]", TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.COMMA.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.COMMA_EXTRACTOR_URI,
		// FashionTrackerRegex.COMMA_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.SITEMAP_TRACKER,
		// "[\"https://www.casual-fashion.com/xmlsitemap/download/sitemap-en_nl.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.CASUAL_FASHION.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.CASUAL_FASHION_EXTRACTOR_URI,
		// FashionTrackerRegex.CASUAL_FASHION_REGEX);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.JACKJONES_TRACKER_URI,
		// "[\"http://www.jackjones.com/on/demandware.static/-/Library-Sites-bestseller-content-library/da_DK/dwc0df13c3/affiliatefeeds/product-sitemap/Sitemap_jj_0_BSE-DK.xml.gz\"]",
		// TRACKER_AGENT_URI, new TrackerInfo(BasedOn.JACK_JONES.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.JACK_JONES_EXTRACTOR, null);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FREE_PEOPLE_HTML_TRACKER_URI,
		// TrackerList.FREE_PEOPLE_NEW_PRODUCTS.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.FREE_PEOPLE.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.FREE_PEOPLE_EXTRACTOR_URI);

		// startTracker(client, wgb.io.roles.tracker.Roles.SITEMAP_TRACKER,
		// TrackerList.FREE_PEOPLE_SITE_MAP.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.FREE_PEOPLE.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.FREE_PEOPLE_EXTRACTOR_URI);
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.STRADIVARIUS_TRACKER_URI,
		// TrackerList.STRADIVARIUS_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.STRADIVARIUS.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.STRADIVARIUS_EXTRACTOR_URI,
		// FashionRegex.STRADIVARIUS_REGEX);
		// //
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.BERSHKA_CLOTHING_TRACKER,
		// TrackerList.BERSHKA_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.BERSHKA.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.BERSHKA_EXTRACTOR, FashionRegex.BERSHKA_REGEX);
		// //
		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.PULLANDBEAR_TRACKER_URI,
		// TrackerList.PULLANDBEAR_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.PULLANDBEAR.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.PULLANDBEAR_EXTRACTOR_URI,
		// FashionRegex.PULL_AND_BEAR_REGEX);
		// // //
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.MASSIMIDUTTI_TRACKER_URI,
		// TrackerList.MASSIMODUTTI_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.MASSIMODUTTI.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.MASSIMIDUTTI_EXTRACTOR_URI,
		// FashionRegex.MASSIMODUTTI_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.RIVER_ISLAND_TRACKER_URI,
		// "[\"https://us.riverisland.com/sitemaps/sitemap-women_us.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.RIVER_ISLAND.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.RIVER_ISLAND_EXTRACTOR_URI,
		// FashionTrackerRegex.RIVERISLAND_REGEX);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FOREVER21_TRACKER_URI,
		// TrackerList.FOREVER21_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.FOREVER21.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.FOREVER21_EXTRACTOR_URI);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.OVERTHEOCEAN_TRACKER,
		// "[\"https://www.overtheocean.com/sitemap_products_1.xml?from=1724890561\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.gunes.com/", "product",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.OVER_THE_OCEAN_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.TOPSHOP_TRACKER_URI,
		// TrackerList.TOPSHOP_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.TOPSHOP.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.TOPSHOP_EXTRACTOR,
		// FashionRegex.TOPSHOP_REGEX);
		// // //
		// http:www.zara.com/sitemaps/sitemap-tr-en.xml.gz

//		startSiteMapTracker(client, wgb.io.roles.tracker.Roles.ZARA_CLOTHING_TRACKER,
//				TrackerList.ZARA_TRACKER_LIST.toString(), TRACKER_AGENT_URI,
//				new TrackerInfo(BasedOn.ZARA.toString(), GraphConstants.ENGLISH),
//				wgb.io.roles.extractor.Roles.ZARA_EXTRACTOR, FashionRegex.ZARA_REGEX_TO_GET_CATEGORY_PAGES);

		// wgb.io.roles.tracker.Roles.ZARA_CLOTHING_TRACKER,
		// "[\"http://www.zara.com/sitemaps/sitemap-es-en.xml.gz\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.ZARA.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.ZARA_EXTRACTOR,
		// FashionTrackerRegex.ZARA_REGEX);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.MELIJOE_TRACKER_URI,
		// "[\"https://www.melijoe.com/es/sitemap.products.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.MELIJOE.toString(), GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.MELIJOE_EXTRACTOR_URI, null);

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.SITEMAP_TRACKER,
		// "[\"http://www.promod.eu/sitemap.xml\"]", TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.promod.eu", "product",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.PROMOD_EXTRACTOR_URI, null);

		// "http://www2.hm.com/ro_ro/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ro_ro/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ro_ro/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ro_ro/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0",
		//
		// "http://www2.hm.com/ru_ru/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ru_ru/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ru_ru/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0",
		// "http://www2.hm.com/ru_ru/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0",

		// "http://www2.hm.com/tr_tr/kadin/urune-gore-satin-al/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/tr_tr/erkek/urune-gore-satin-al/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/tr_tr/cocuk/urune-gore-satin-al/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/tr_tr/home/odaya-gore-alisveris/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0"

		// "http://www2.hm.com/en_asia1/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&offset=30&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia1/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia1/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&offset=30&page-size=200&offset=0"

		// "http://www2.hm.com/en_cn/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_cn/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_cn/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_cn/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0"

		// "http://www2.hm.com/en_asia2/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia2/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia2/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&offset=30&page-size=200&offset=0"

		// "http://www2.hm.com/en_asia3/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia3/ladies/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_asia3/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&offset=30&page-size=200&offset=0"

		// "http://www2.hm.com/en_ca/women/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ca/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ca/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ca/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0"

		// "http://www2.hm.com/en_ie/women/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ie/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ie/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_ie/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0"

		// "http://www2.hm.com/en_gb/women/shop-by-product/view-all/_jcr_content/main/productlisting_30ab.display.html?product-type=ladies_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_gb/home/shop-by-product/view-all/_jcr_content/main/productlisting_c559.display.html?product-type=home_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_gb/men/shop-by-product/view-all/_jcr_content/main/productlisting_fa5b.display.html?product-type=men_all&sort=newProduct&page-size=200&offset=0"
		// "http://www2.hm.com/en_gb/kids/shop-by-product/view-all/_jcr_content/main/productlisting_acbd.display.html?product-type=kids_all&sort=newProduct&page-size=200&offset=0"

		// "********* *********** ****************"

		// "http://api.hm.com/v2/za/en/products/display?searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1",
		// "http://api.hm.com/v2/za/en/products/display?searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1",
		// "http://api.hm.com/v2/za/en/products/display?searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1",
		// "http://api.hm.com/v2/za/en/products/display?searchOrders=newfrom_desc&categories=home&concealCategories=true&pageSize=200&page=1",

		// "http://api.hm.com/v2/in/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/in/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/in/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"

		// "http://api.hm.com/v2/ph/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/ph/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/ph/en/products/display?page=2&searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/ph/en/products/display?searchOrders=newfrom_desc&categories=home&concealCategories=true&pageSize=200&page=1",

		// "http://api.hm.com/v2/us/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/us/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/us/en/products/display?page=2&searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/us/en/products/display?searchOrders=newfrom_desc&categories=home&concealCategories=true&pageSize=200&page=1",

		// "http://api.hm.com/v2/au/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/au/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/au/en/products/display?page=2&searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/au/en/products/display?searchOrders=newfrom_desc&categories=home&concealCategories=true&pageSize=200&page=1",

		// "http://api.hm.com/v2/nz/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/nz/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/nz/en/products/display?page=2&searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1"

		// "http://api.hm.com/v2/cy/en/products/display?page=2&searchOrders=newfrom_desc&categories=ladies&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/cy/en/products/display?page=2&searchOrders=newfrom_desc&categories=men&concealCategories=true&pageSize=200&page=1"
		// "http://api.hm.com/v2/cy/en/products/display?page=2&searchOrders=newfrom_desc&categories=kids&concealCategories=true&pageSize=200&page=1"
		// //

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.PAGINATED_HTML_TRACKER_BASE,
		// "[\"http://www.esprit.co.uk/men-new/new-today-in-the-last-few-days/brand-new\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.esprit.co.uk/", "breakingnews",
		// GraphConstants.ENGLISH),wgb.io.roles.extractor.Roles.ESPRIT_EXTRACTOR_URI);
		//
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.SCOTCHSODA_TRACKER_URI,
		// "[\"https://www.scotch-soda.com/us/en/women/all-clothing?sz=36\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("https://www.scotch-soda.com/", "breakingnews",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.SCOTCHSODA_EXTRACTOR_URI);
		// execute();
	}

	private JsonElement send2Extractor(String trackedUri, String extractorRoleUri, String pageContent, String date) {
		JsonElement response = null;
		JsonObject content = new JsonObject();
		content.addProperty(JsonFields.trackedUri, trackedUri);
		content.addProperty(JsonFields.pageContent, pageContent);
		content.addProperty(JsonFields.date, date);

		response = wgbClient.send(content.toString(), extractorRoleUri,
				wgb.io.actions.extractor.Actions.TRY_EXTRACTION);

		return response;
	}

	private WGBClient wgbClient = new WGBClient();

	private void rssStartMethods(HttpClient client) throws InterruptedException, TimeoutException, ExecutionException {
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.SITEMAP_TRACKER,
		// "[\"https://www.ihealthtube.com/sitemap.xml?page=1\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.IHEALTHTUBE.toString(), "product",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.IHEALTHTUBE,
		// "(www.ihealthtube.com\\/video)[A-Z a-z -\\/ 0-9]+");
		//

		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.SITEMAP_TRACKER,
		// "[\"http://www.healthandcarevideos.com/product-sitemap.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo(BasedOn.HEALTHANDCAREVIDEOS.toString(),
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.META_TAG_EXTRACTOR_URI, null);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.HURRIYET_COMMENT_TRACKER_URI,
		// "http://www.hurriyet.com.tr/kahraman-polis-fethi-sekin-sehit-oldu-alcaklarin-katliam-planini-onledi-40327836",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.hurriyet.com.tr/", "breakingnews",
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.HURRIYET_COMMENT_EXTRACTOR);
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.URI_PATTERN_BASED_TRACKER_BASE,
		// "http://abc.az/eng/news/",
		// TRACKER_AGENT_URI, new TrackerInfo("http://abc.az/eng/",
		// BasedOn.UKRAINIAN_JOURNAL.category().toString(),
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.CSS_QUERY_EXTRACTOR_URI);

		// startTracker(client, wgb.io.roles.tracker.Roles.RSS_TRACKER_BASE,
		// "https://www.bereketenerji.com.tr/feed/",
		// TRACKER_AGENT_URI, new
		// TrackerInfo("https://www.bereketenerji.com.tr/",
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.RSS_EXTRACTOR_URI);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.URI_PATTERN_BASED_TRACKER_BASE,
		// "http://www.supplychaindigital.com/", TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.supplychaindigital.com/",
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.META_TAG_EXTRACTOR_URI);

		//

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.URI_PATTERN_BASED_TRACKER_BASE,
		// "http://www.sonsozbasinin.com",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.sonsozbasinin.com",
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.SONSOZBASININ_EXTRACTOR_URI);

		// startTracker(client,
		// wgb.io.roles.tracker.Roles.HISSE_NET_FORUM_PIYASALAR_TRACKER_URI,
		// "http://www.hisse.net/forum/forumdisplay.php?f=1",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://hisse.net/forum/",
		// GraphConstants.BREAKING_NEWS_DOMAIN,
		// GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		//
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.HISSE_NET_FORUM_HISSELER_TRACKER_URI,
		// "http://www.hisse.net/forum/forumdisplay.php?f=27",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://hisse.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FORUM_BORSA_TRACKER_URI,
		// "http://www.forumborsa.net/forumdisplay.php?14-BORSA-MEKTEB%C4%B0",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.forumborsa.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		// //
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FORUM_BORSA_TRACKER_URI,
		// "http://www.forumborsa.net/forumdisplay.php?11-DOLAR-ALTIN-EMT%C4%B0A-ANAL%C4%B0Z-YORUM",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.forumborsa.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		// //
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FORUM_BORSA_TRACKER_URI,
		// "http://www.forumborsa.net/forumdisplay.php?7-V%C4%B0OB-VARANT-YORUM-ANAL%C4%B0Z",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.forumborsa.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		// //
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FORUM_BORSA_TRACKER_URI,
		// "http://www.forumborsa.net/forumdisplay.php?1-FORUM-BORSA-GENEL",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.forumborsa.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);
		// //
		// startTracker(client,
		// wgb.io.roles.tracker.Roles.FORUM_BORSA_TRACKER_URI,
		// "http://www.forumborsa.net/forumdisplay.php?19-Hisseler",
		// TRACKER_AGENT_URI, new TrackerInfo("http://www.forumborsa.net/",
		// GraphConstants.BREAKING_NEWS_DOMAIN, GraphConstants.TURKISH),
		// wgb.io.roles.extractor.Roles.VBULLETIN_FORUM_EXTRACTOR_URI);

		// startOtherTracker(client,
		// wgb.io.roles.tracker.Roles.EKSI_TRACKER_URI,
		// TrackerList.EKSI_SOZLUK,
		// "http://localhost:8081/eksi_tracker", null);

		// startSocialMediaTracker(client,
		// wgb.io.roles.tracker.Roles.FACEBOOK_TRACKER_BASE,
		// FacebookPages.TUKAS,
		// "http://localhost:8081/facebook/tukas", null,
		// wgb.io.roles.extractor.Roles.FACEBOOK_EXTRACTOR_URI);

		//
		// startSiteMapTracker(client,
		// wgb.io.roles.tracker.Roles.MIGROS_SANAL_MARKET_TRACKER_URI,
		// "[\"http://www.sanalmarket.com.tr/sitemap_1.xml\"]",
		// TRACKER_AGENT_URI,
		// new TrackerInfo("http://www.sanalmarket.com.tr", "product",
		// GraphConstants.ENGLISH),
		// wgb.io.roles.extractor.Roles.MIGROS_SANAL_MARKET_EXTRACTOR);
		// Thread.sleep(sleepTime);

		// startOtherTracker(client,
		// wgb.io.roles.tracker.Roles.KAMU_AYDINLATMA_PLATFORMU_TRACKER_URI,
		// TrackerList.KAMU_AYDINLATMA_PLATFORMU,
		// "http://localhost:8081/kamu_aydinlatma_platformu_api",
		// "www.kap.org.tr");
		//
		// try {
		// Agent agent = new Agent("tracker", new TrackerRoleKnowledge());
		// String action = wgb.io.actions.tracker.Actions.GET_TWITTER_TERMS;
		// agent.getRole(wgb.io.roles.tracker.Roles.TWITTER_STREAM_TRACKER).trigger().to(action).then();
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	private static void startSocialMediaTracker(HttpClient client, String roleName, String trackUri, String localeUri,
			String basedOnUri, String extractorUri) throws InterruptedException, TimeoutException, ExecutionException {
		JsonObject obj = new JsonObject();
		obj.addProperty(JsonFields.trackedUri, trackUri);
		if (basedOnUri != null) {
			obj.addProperty(JsonFields.basedOnUri, basedOnUri);
		}
		obj.addProperty(JsonFields.extractorUri, extractorUri);
		String json = new Gson().toJson(obj);
		Message message = new Message("http://localhost:8080/counter", roleName, Performative.REQUEST,
				wgb.io.actions.tracker.Actions.START, Protocol.RI, json);
		ContentResponse response = client.POST(localeUri).content(new StringContentProvider(message.serialize()))
				.send();
		System.out.println(response.getContentAsString());
		Thread.sleep(1000);
	}

	private static void startOtherTracker(HttpClient client, String roleName, String trackUri, String localeUri,
			String basedOnUri) throws InterruptedException, TimeoutException, ExecutionException {
		JsonObject obj = new JsonObject();
		obj.addProperty(JsonFields.trackedUri, trackUri);
		if (basedOnUri != null) {
			obj.addProperty(JsonFields.basedOnUri, basedOnUri);
		}
		String json = new Gson().toJson(obj);
		Message message = new Message("http://localhost:8080/counter", roleName, Performative.REQUEST,
				wgb.io.actions.tracker.Actions.START, Protocol.RI, json);
		ContentResponse response = client.POST(localeUri).content(new StringContentProvider(message.serialize()))
				.send();
		System.out.println(response.getContentAsString());
		Thread.sleep(1000);
	}

	// private static void registerCustodian(HttpClient client)
	// throws InterruptedException, TimeoutException, ExecutionException {
	// Message msg = new Message("http://localhost:8080/counter",
	// wgb.io.roles.custodian.Roles.CUSTODIAN_URI,
	// Performative.REQUEST, wgb.io.actions.custodian.Actions.FIRE, Protocol.RI,
	// null);
	// client.POST("http://localhost:8085/custodian").content(new
	// StringContentProvider(msg.serialize()))
	// .send(new BufferingResponseListener(8 * 1024 * 1024) {
	// @Override
	// public void onComplete(Result result) {
	// if (!result.isFailed()) {
	// byte[] responseContent = getContent();
	// System.out.println("RESPONSE: " + responseContent.toString());
	// }
	// }
	// });
	//
	// Thread.sleep(1000);
	// }

	private static void registerFollowl(HttpClient client)
			throws InterruptedException, TimeoutException, ExecutionException {
		Message msg = new Message(wgb.io.roles.reporter.Roles.TAG_FOLLOWER_URI, wgb.io.roles.followl.Roles.FOLLOWL_URI,
				Performative.REQUEST, wgb.io.actions.sprinkler.Actions.REGISTER, Protocol.RI,
				wgb.io.roles.reporter.Roles.TAG_FOLLOWER_URI);
		client.POST("http://localhost:8082/").content(new StringContentProvider(msg.serialize()))
				.send(new BufferingResponseListener(8 * 1024 * 1024) {
					@Override
					public void onComplete(Result result) {
						if (!result.isFailed()) {
							byte[] responseContent = getContent();
							System.out.println("RESPONSE: " + responseContent.toString());
						}
					}
				});

		Thread.sleep(1000);
	}

	private static void registerReporter(HttpClient client)
			throws InterruptedException, TimeoutException, ExecutionException {
		Message msg = new Message(wgb.io.roles.reporter.Roles.TAG_FOLLOWER_URI,
				wgb.io.roles.sprinkler.Roles.SPRINKLER_URI, Performative.REQUEST,
				wgb.io.actions.sprinkler.Actions.REGISTER, Protocol.RI, wgb.io.roles.reporter.Roles.TAG_FOLLOWER_URI);
		client.POST("http://localhost:8082/").content(new StringContentProvider(msg.serialize()))
				.send(new BufferingResponseListener(8 * 1024 * 1024) {
					@Override
					public void onComplete(Result result) {
						if (!result.isFailed()) {
							byte[] responseContent = getContent();
							System.out.println("RESPONSE: " + responseContent.toString());
						}
					}
				});

		Thread.sleep(1000);
	}

	private static void startSiteMapTracker(HttpClient client, String roleName, String trackUri, String localeUri,
			TrackerInfo trackerInfo, String extractorUri, String regexPattern)
			throws InterruptedException, TimeoutException, ExecutionException {
		JsonObject obj = new JsonObject();
		obj.addProperty(JsonFields.trackedUri, trackUri);
		JsonObject trackerInfoObj = new JsonObject();
		trackerInfoObj.addProperty(Tag.CATEGORY.text(), "clothing");
		trackerInfoObj.addProperty(Tag.IN_LANGUAGE.text(), trackerInfo.getLanguage());
		obj.addProperty(JsonFields.extractorUri, extractorUri);

		if (regexPattern != null && !regexPattern.isEmpty()) {
			trackerInfoObj.addProperty(JsonFields.patternInfo, regexPattern);
		}
		obj.add(JsonFields.trackerInfo, trackerInfoObj);
		obj.addProperty("messageSize", 3);
		// obj.addProperty(JsonFields.isCurl, true);
		// *************************
		// *************************

		obj.addProperty(JsonFields.checkExists, false);
		obj.addProperty(JsonFields.sendToExtractor, false);

		// *************************
		// *************************

		// JsonArray headerArray = new JsonArray();
		// headerArray.add(new JsonPrimitive(
		// "User-Agent: Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36
		// (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36"));
		// obj.add(JsonFields.headers, headerArray);

		obj.addProperty(JsonFields.extractorUri, extractorUri);
		if (trackerInfo.getBasedOnUri() != null) {
			obj.addProperty(JsonFields.basedOnUri, trackerInfo.getBasedOnUri());
		}
		String json = new Gson().toJson(obj);
		Message message = new Message("http://localhost:8080/counter", roleName, Performative.REQUEST,
				wgb.io.actions.tracker.Actions.START, Protocol.RI, json);
		client.POST(localeUri).content(new StringContentProvider(message.serialize()))
				.send(new BufferingResponseListener(8 * 1024 * 1024) {
					@Override
					public void onComplete(Result result) {
						if (!result.isFailed()) {
							byte[] responseContent = getContent();
							System.out.println("RESPONSE: " + responseContent.toString());
						}
					}
				});
		Thread.sleep(1000);
	}

	private static void startTracker(HttpClient client, String roleName, String trackUri, String localeUri,
			TrackerInfo trackerInfo, String extractorRole)
			throws InterruptedException, TimeoutException, ExecutionException {
		JsonObject obj = new JsonObject();
		obj.addProperty(JsonFields.trackedUri, trackUri);
		JsonObject trackerInfoObj = new JsonObject();
		trackerInfoObj.addProperty(Tag.IN_LANGUAGE.text(), trackerInfo.getLanguage());
		obj.addProperty(JsonFields.extractorUri, extractorRole);
		// trackerInfoObj.addProperty(JsonFields.resolve, false);
		JsonArray headerArray = new JsonArray();
		// headerArray.add(new JsonPrimitive(
		// "User-Agent: Mozilla/5.0 (X11; Linux x86_64)
		// AppleWebKit/537.36(KHTML, like Gecko) Chrome/53.0.2785.92
		// Safari/537.36"));
		// headerArray.add(
		// new JsonPrimitive("User-Agent:Mozilla/5.0
		// (compatible;Googlebot/2.1;http://www.google.com/bot.html)"));
		// obj.add(JsonFields.trackerHeaders, headerArray);
		// obj.addProperty(JsonFields.isCurl, true);

		// JsonObject patternInfo = new JsonObject();
		// patternInfo.addProperty("valuePattern",
		// "(?i)(duyuru-haberler-)([a-z]|[0-9]|-)*.html");
		// obj.add("patternInfo", patternInfo);

		obj.addProperty(JsonFields.sendToExtractor, false);
		obj.addProperty(JsonFields.checkExists, false);

		JsonObject patternInfo = new JsonObject();
		// patternInfo.addProperty("valuePattern",
		// "(denib-gundem-|aktuel-(duyurular|sirkuler)-)([a-z]|[0-9]|-)*.html");
		// patternInfo.addProperty("valuePattern", "(news_)[0-9a-z-_]+.html$");
		// patternInfo.addProperty("valuePattern",
		// "en\\/home\\/news\\/artikeldetail\\/[a-zA-Z0-9.?=&/-]+.html");
		// patternInfo.addProperty("valuePattern",
		// "(news_)[0-9a-z-_]+.html$");
		// patternInfo.addProperty("valuePattern",
		// "http:\\/\\/www.supplychaindigital.com\\/(logistics|warehousing|procurement|supplychainmanagement)\\/\\d{4}\\/.*");
		// patternInfo.addProperty("valuePattern", "(news_)[0-9a-z-_]+.html$");
		// patternInfo.addProperty("valuePattern",
		// "\\/news\\/gennews\\/[a-zA-Z0-9\\/-]+uk.asp");
		// obj.add("patternInfo", patternInfo);
		obj.addProperty(JsonFields.checkExists, false);

		JsonArray arrExtractor = new JsonArray();
		JsonPrimitive element1 = new JsonPrimitive(
				"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36");
		arrExtractor.add(element1);
		trackerInfoObj.add(JsonFields.headers, arrExtractor);

		obj.add(JsonFields.trackerInfo, trackerInfoObj);
		if (trackerInfo.getBasedOnUri() != null) {
			obj.addProperty(JsonFields.basedOnUri, trackerInfo.getBasedOnUri());
		}
		String json = new Gson().toJson(obj);
		Message message = new Message("http://localhost:8080/counter", roleName, Performative.REQUEST,
				wgb.io.actions.tracker.Actions.START, Protocol.RI, json);
		client.POST(localeUri).content(new StringContentProvider(message.serialize()))
				.send(new BufferingResponseListener(8 * 1024 * 1024) {
					@Override
					public void onComplete(Result result) {
						if (!result.isFailed()) {
							byte[] responseContent = getContent();
							System.out.println("RESPONSE: " + responseContent.toString());
						}
					}
				});
		Thread.sleep(1000);
	}

	public void startPipelineEndAgent() throws Exception {
		new Agent(new RoleKnowledge() {
			@Override
			protected Role create(Agent agent, String name) {
				if (role == null) {
					role = new PipelineEndRole(wgb.io.roles.tagger.Roles.SEMANTIC_TAGGER_URI, agent);
				}
				return role;
			}
		}, 8084);
	}

	private class PipelineEndRole extends Role {

		public PipelineEndRole(String name, Agent agent) {
			super(name, agent);
			defineCollectAction();
		}

		private void defineCollectAction() {
			defineAction(wgb.io.actions.tagger.Actions.TAG, new Action() {
				int i = 0;
				// File file = new
				// File("/home/galaksiya/Desktop/twitterTestContent.txt");

				@Override
				public String act(String content) throws Exception {

					System.out.println(i++ + " " + new JsonParser().parse(content));

					// FileWriter fw = new FileWriter(file.getAbsoluteFile(),
					// true);
					// BufferedWriter bw = new BufferedWriter(fw);
					// bw.write("\n\n\n" + i + "COLLECTED MESSAGE: " + new
					// JsonParser().parse(content));
					// bw.close();
					return null;
				}
			});
		}

	}
}