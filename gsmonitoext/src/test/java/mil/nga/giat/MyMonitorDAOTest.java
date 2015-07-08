package mil.nga.giat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

public class MyMonitorDAOTest
{

    MyMonitorDAO dao = new MyMonitorDAO();

    @Before
    public void setup()
            throws IOException {

        File dataDir = new File(
                "testdata");
        
        
        
        if (dataDir.exists()) FileUtils.deleteDirectory(dataDir);
        if (!dataDir.exists()) dataDir.mkdirs();
        
        
        
        HashMap params=new HashMap();
        params.put(JDBCDataStoreFactory.NAMESPACE.key,"DefaulURI");
        params.put(JDBCDataStoreFactory.DATABASE.key,"some database path");
       // params.put(JDBCDataStoreFactory.DBTYPE.key,"h2");
        
        
        params.put(
                PropertyDataStoreFactory.DIRECTORY.key,dataDir.getAbsolutePath().toString());
        params.put(
                PropertyDataStoreFactory.NAMESPACE.key,
                "http://dao.test.org");

        MonitorConfig config = new MonitorConfig();
        dao.setDataStoreTypeName("MonitorRequestData");
        dao.setDataStoreParams(params);
        dao.init(config);
    }

    @Test
    public void test()
            throws IOException {

        dao.add(new RequestData());
    }
    /*
    @Test
    void testMappings() {
           // need to add a protected feature type access method to the DAO
              //final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(this.dao.getFeatureType());
             //final SimpleFeature feature = builder.buildFeature("123");
             //final RequestData request = new RequestData();
           
           // put some test data in each attribute..ignore Error and body[] for now
             //request.setBbox(factory.createPolygon...etc...));
             //request.setHost("localhost");
             //...etc....
           
           //dao.toSimpleFeature(feature, request);
             //final RequestData copyOfRequest = new RequestData();
             //dao.toRequestData(feature, copyOfRequest);
           
           // did the bi-directional mapping work...check each attribute
             //assertEquals(request.getHost(), copyOfRequest.getHost());
             //...etc....
       }
       */
}