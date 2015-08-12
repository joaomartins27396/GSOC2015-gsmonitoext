package org.locationtech.gsmonitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.Query.Comparison;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestData.Category;
import org.geoserver.monitor.RequestData.Status;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class FeatureMonitorDAOTest {

	static FeatureMonitorDAO dao = new FeatureMonitorDAO();

	GeometryFactory factory = new GeometryFactory(new PrecisionModel(
			PrecisionModel.FIXED));

	@BeforeClass
	public static void setup() throws IOException {

		File dataDir = new File("testdata");
		if (dataDir.exists())
			FileUtils.deleteDirectory(dataDir);

		Map<String, Serializable> params = new HashMap<String, Serializable>();
		params.put("dbtype", "h2");
		params.put("database", "testdata/db");
		
		
		
		
		dao.setDataDirectory(new GeoServerDataDirectory(new File("test/resources/monitoring")));
		
		
		MonitorConfig config = new MonitorConfig();
		dao.setDataStoreTypeName("MonitorRequestData");
		dao.setDataStoreParams(params);
		dao.init(config);

		// create and add sample data for ALL tests
		dao.add(getSample(33));
		RequestData data2 = getSample(34);
		data2.setHost("remote");
		data2.setTotalTime(100);
		data2.setBody("bigger body".getBytes("UTF-8"));
		data2.setBodyContentLength(data2.getBody().length);
		dao.add(data2);
		RequestData data3 = getSample(35);
		data3.setTotalTime(101);
		dao.add(data3);
	}

	private static RequestData getSample(int id)
			throws UnsupportedEncodingException {
		RequestData data = new RequestData();
		data.setBbox(new ReferencedEnvelope(-5, 5, -10, 10, FeatureMonitorDAO.CRSI));
		data.setBody("body".getBytes("UTF-8"));
		data.setBodyContentLength(data.getBody().length);
		data.setCategory(Category.REST);
		data.setErrorMessage("Error");
		data.setHost("local");
		data.setPath("path");
		data.setQueryString("?x=y&z=w");
		data.setId(id);
		data.setRemoteLat(0.1);
		data.setRemoteLon(0.2);
		data.setService("test");
		data.setRemoteUser("user");
		data.setRemoteCountry("country");
		data.setRemoteCity("city");
		data.setOperation("operation");
		data.setSubOperation("sub");
		data.setHttpMethod("POST");
		data.setHttpReferer("localhost");
		data.setStatus(Status.RUNNING);
		data.setResponseStatus(44);
		data.setTotalTime(99);
		Calendar c = Calendar.getInstance();
		data.setEndTime(c.getTime());
		c.set(Calendar.MINUTE, c.get(Calendar.MINUTE) - 1);
		data.setStartTime(c.getTime());
		return data;
	}

	@Test
	public void testMappings() throws UnsupportedEncodingException { // need to
																		// add a
																		// protected
																		// feature
																		// type

		RequestData data = getSample(33);

		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(
				dao.getFeatureType());

		Object attributes[] = new Object[dao.getFeatureType()
				.getAttributeCount()];
		for (int i = 0; i < attributes.length; i++) {
			attributes[i] = dao.getFeatureType().getDescriptor(i)
					.getDefaultValue();
		}
		SimpleFeature sample = builder.buildFeature("test123", attributes);

		RequestData result = new RequestData();
		dao.toSimpleFeature(sample, data);
		dao.toRequestData(sample, result);

		assertEquals(data.getBbox(), result.getBbox());
		assertEquals(data.getBodyAsString(), result.getBodyAsString());
		assertEquals(data.getBodyContentLength(), result.getBodyContentLength());
		assertEquals(data.getCategory(), result.getCategory());
		assertEquals(data.getEndTime(), result.getEndTime());
		assertEquals(data.getStartTime(), result.getStartTime());
		assertEquals(data.getOperation(), result.getOperation());
		assertEquals(data.getStatus(), result.getStatus());
		assertEquals(data.getErrorMessage(), result.getErrorMessage());
		assertEquals(data.getHost(), result.getHost());
		assertEquals(data.getRemoteUser(), result.getRemoteUser());
		assertEquals(data.getRemoteHost(), result.getRemoteHost());
		assertEquals(data.getBodyContentType(), result.getBodyContentType());
		assertEquals(data.getId(), result.getId());
		assertEquals(data.getResources(), result.getResources());
		assertEquals(data.getResponseStatus(), result.getResponseStatus());
		assertEquals(data.getRemoteLat(), result.getRemoteLat(), 0.001);
		assertEquals(data.getRemoteLon(), result.getRemoteLon(), 0.001);
		assertEquals(data.getTotalTime(), result.getTotalTime(), 0.001);

	}

	@Test
	public void test() throws IOException {

		List<RequestData> dataSet = dao.getRequests();
		assertEquals(3, dataSet.size());
		assertEquals(33, dataSet.get(0).getId());
		assertEquals(34, dataSet.get(1).getId());
		assertEquals(35, dataSet.get(2).getId());
	}

	@Test
	public void testQueryById() throws IOException {
		RequestData data = dao.getRequest(33);
		assertTrue(data != null);
	}

	@Test
	public void testCount() throws IOException {
		assertEquals(3, dao.getCount(null));
	}

	@Test
	public void testGTE() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.GTE);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(2, results.size());
	}

	public void testLTE() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.LTE);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(2, results.size());
	}

	@Test
	public void testOr() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.LT);
		query.or("totalTime", 100, Comparison.GT);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(2, results.size());
	}

	@Test
	public void testAnd() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.LTE);
		query.and("BodyContentLength", 5, Comparison.LT);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(1, results.size());
	}

	@Test
	public void testLT() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.LT);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(1, results.size());
	}

	@Test
	public void testEquals() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.EQ);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(1, results.size());
	}

	@Test
	public void testGT() throws Exception {

		Query query = new Query();
		query.filter("totalTime", 100, Comparison.GT);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(1, results.size());
	}
	
	
	
	
	
	@Test
	public void testIN() throws Exception {
		Query query = new Query();
		query.filter("totalTime", Arrays.asList(new Long(100), new Long(101)), Comparison.IN);
		List<RequestData> results = dao.getRequests(query);
		assertEquals(2, results.size());
	}

	

	
}
