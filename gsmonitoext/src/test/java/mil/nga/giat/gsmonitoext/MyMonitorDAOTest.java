package mil.nga.giat.gsmonitoext;



import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;





import org.apache.commons.io.FileUtils;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

public class MyMonitorDAOTest
{

	MyMonitorDAO dao = new MyMonitorDAO();

	GeometryFactory factory = new GeometryFactory(
			new PrecisionModel(
					PrecisionModel.FIXED));
	@Before
	public void setup()
			throws IOException {
		
		File dataDir = new File(
				"testdata");
		if (dataDir.exists()) FileUtils.deleteDirectory(dataDir);
	//	if (!dataDir.exists()) dataDir.mkdirs();
		Map params = new HashMap();
		params.put("dbtype", "h2");
	    params.put("database", "testdata/db");
		MonitorConfig config = new MonitorConfig();
		dao.setDataStoreTypeName("MonitorRequestData");
		dao.setDataStoreParams(params);
		dao.init(config);
	}

	/*
	@Test void testMappings() {
		// need to add a protected feature type access method to the DAO
		final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(dao.getFeatureType());
		final SimpleFeature feature = builder.buildFeature("123");
		final RequestData request = new RequestData();
		
		// put some test data in each attribute..ignore Error and body[] for now
		request.setBbox(factory.createPolygon...etc...));
		request.setHost("localhost");
		...etc....
		
		dao.toSimpleFeature(feature, request);
		final RequestData copyOfRequest = new RequestData();
		dao.toRequestData(feature, copyOfRequest);
		
		// did the bi-directional mapping work...check each attribute
		assertEquals(request.getHost(), copyOfRequest.getHost());
		...etc....
	}*/
	
	@Test
	public void test()
			throws IOException {

		dao.add(new RequestData());
		dao.getRequests(new Query());
	}
}
