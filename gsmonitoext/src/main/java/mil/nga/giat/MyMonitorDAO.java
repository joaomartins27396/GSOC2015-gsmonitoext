package mil.nga.giat;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geotools.data.AbstractDataStoreFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.feature.SchemaException;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.gce.imagemosaic.catalog.index.Indexer.Datastore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.geoserver.monitor.MonitorConfig.Mode;

public class MyMonitorDAO implements MonitorDAO {
	
	
	public static enum Sync {
		SYNC, ASYNC, ASYNC_UPDATE;
	}

	Sync sync = Sync.ASYNC;

	Mode mode = Mode.HISTORY;

	public static final String TYPENAME = "requestDataFeature";
	private DataStore dataStore = null;

	private SimpleFeatureType featureType = null;

	String dataStoreTypeName = TYPENAME;

	// need a way to set/get this via Spring setter/getter
	private Map<String, Serializable> dataStoreParams;

	@Override
	public String getName() {
		return "SimpleFeature Monitor";
	}

	public String getDataStoreTypeName() {
		return dataStoreTypeName;
	}

	public void setDataStoreTypeName(String dataStoreTypeName) {
		this.dataStoreTypeName = dataStoreTypeName;
	}

	public Map<String, Serializable> getDataStoreParams() {
		return dataStoreParams;
	}

	public void setDataStoreParams(Map<String, Serializable> dataStoreParams) {

		this.dataStoreParams = dataStoreParams;
	}

	@Override
	public void init(MonitorConfig config) {

		// example...needs more
		try {
			featureType = DataUtilities
					.createType(
							dataStoreTypeName,
							"envelope:Polygon,id:java.lang.Long,queryString:String,path:String,startTime:Date, endTime:Date, totalTime:java.lang.Long");
		} catch (SchemaException e) {
			e.printStackTrace();
		}

		// HOW TO FIND OR CREATE A DATA STORE

		// Experiment here to see what works best using the page I gave you.
		// For testing, somehow parameterize this to use a shape file store.
		// The latest version of documentation suggests using a Catalog.
		final Map<String, Serializable> params = dataStoreParams;
		
		

		for (Iterator<?> i = DataStoreFinder.getAvailableDataStores(); i
				.hasNext();) {
			DataStoreFactorySpi factory = (DataStoreFactorySpi) i.next();

			try {
				if (factory.canProcess(params)) {
					// hack for geotools bug
					if (factory instanceof PropertyDataStoreFactory)
						new File(params.get(
								PropertyDataStoreFactory.DIRECTORY.key)
								.toString()).delete();

					this.dataStore = factory.createNewDataStore(params);

					if (!dataStore.getNames().contains(
							this.featureType.getName())) {
						dataStore.createSchema(this.featureType);
					}
				}

			} catch (Throwable warning) {
				System.out.println(factory.getDisplayName() + " failed:"
						+ warning);
				warning.printStackTrace();
			}
		}

	}

	@Override
	public RequestData init(RequestData data) {
		// TODO Auto-generated method stub

		if (mode != Mode.HISTORY) {
			if (sync == Sync.ASYNC_UPDATE) {
				save(data);
			}
		} else {
			// don't persist yet, we persist at the very end of request
		}
		return data;
	}

	@Override
	public void add(RequestData data) {
		this.save(data);
	}

	@Override
	public void update(RequestData data) {
		Transaction t = new DefaultTransaction("handle");

		FilterFactoryImpl factory = new FilterFactoryImpl();
		Expression exp1 = factory.property("id");
		Expression exp2 = factory.literal(data.getId());
		Filter equals = factory.equal(exp1, exp2, false);

		try (FeatureWriter<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureWriter(TYPENAME, equals, t)) {
			while (fw.hasNext()) {
				SimpleFeature newFeature = fw.next();
				toSimpleFeature(newFeature, data);
				fw.write();
			}

			t.commit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				t.rollback();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void toSimpleFeature(SimpleFeature featureToUpdate, RequestData data) {
		//
	}

	private void toRequestData(SimpleFeature feature, RequestData dataToUpdate) {
		//
	}

	@Override
	public void save(RequestData data) {
		Transaction t = new DefaultTransaction("handle");
		try (FeatureWriter<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureWriterAppend(TYPENAME, t)) {
			SimpleFeature newFeature = fw.next();
			toSimpleFeature(newFeature, data);
			fw.write();
			t.commit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				t.rollback();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Override
	public RequestData getRequest(long id) {
		Transaction t = new DefaultTransaction("handle");

		FilterFactoryImpl factory = new FilterFactoryImpl();
		Expression exp1 = factory.property("id");
		Expression exp2 = factory.literal(id);
		Filter equals = factory.equal(exp1, exp2, false);
		RequestData data = new RequestData();
		try (FeatureReader<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureReader(new Query(TYPENAME, equals), t)) {

			if (fw.hasNext()) {
				toRequestData(fw.next(), data);

			}
			t.commit();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				t.rollback();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return data;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public long getCount(org.geoserver.monitor.Query arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterator<RequestData> getIterator(org.geoserver.monitor.Query arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequestData> getOwsRequests() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequestData> getOwsRequests(String arg0, String arg1,
			String arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequestData> getRequests() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<RequestData> getRequests(org.geoserver.monitor.Query arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void getRequests(org.geoserver.monitor.Query arg0,
			RequestDataVisitor arg1) {
		// TODO Auto-generated method stub

	}
}