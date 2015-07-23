package mil.nga.giat.gsmonitoext;


import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;





import org.geoserver.monitor.And;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Or;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.SchemaException;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Geometry;

import com.vividsolutions.jts.geom.GeometryFactory;

public class MyMonitorDAO implements MonitorDAO {

	public static final String TYPENAME = "requestDataFeature";
	private DataStore dataStore = null;

	private SimpleFeatureType featureType = null;

	String dataStoreTypeName = TYPENAME;

	// need a way to set/get this via Spring setter/getter
	private Map<String, Serializable> dataStoreParams;

	// myData.createSchema( featureType );

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
							"envelope:com.vividsolutions.jts.geom.Polygon,id:java.lang.Long,queryString:String,path:String,startTime:Date,"
									+ " endTime:Date, totalTime:java.lang.Long, BodyAsString:String, BodyContentLength:java.lang.Long,"
									+ "Host:String, ErrorMessage:String, HttpMethod:String, HttpReferer:String, InternalHost:String,"
									+ "Operation:String,OwsVersion:String,QueryString:String, RemoteAddr:String, RemoteCity:String,"
									+ "RemoteCountry:String, RemoteHost:String,RemoteLat:double,"
									+ " RemoteLon:double,RemoteUser:String, RemoteUserAgent:String, Resources:String,"
									+ "ResponseContentType:String, ResponseLength:java.lang.Long, ResponseStatus:int,"
									+ "Service:String, SubOperation:String,Status:String");
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
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

					this.dataStore = factory.createDataStore(params);

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

	// When is this used...do we need to implement something here...like setting
	// certain attributes
	@Override
	public RequestData init(RequestData data) {

		try {
			featureType = DataUtilities
					.createType(
							dataStoreTypeName,
							"envelope:com.vividsolutions.jts.geom.Polygon,id:java.lang.Long,queryString:String,path:String,startTime:Date,"
									+ " endTime:Date, totalTime:java.lang.Long, BodyAsString:String, BodyContentLength:java.lang.Long,"
									+ "Host:String, ErrorMessage:String, HttpMethod:String, HttpReferer:String, InternalHost:String,"
									+ "Operation:String,OwsVersion:String,QueryString:String, RemoteAddr:String, RemoteCity:String,"
									+ "RemoteCountry:String, RemoteHost:String,RemoteLat:double,"
									+ " RemoteLon:double,RemoteUser:String, RemoteUserAgent:String, Resources:String,"
									+ "ResponseContentType:String, ResponseLength:java.lang.Long, ResponseStatus:int,"
									+ "Service:String, SubOperation:String,Status:String");
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final Map<String, Serializable> params = dataStoreParams;

		for (Iterator<?> i = DataStoreFinder.getAvailableDataStores(); i
				.hasNext();) {
			DataStoreFactorySpi factory = (DataStoreFactorySpi) i.next();

			try {
				if (factory.canProcess(params)) {

					this.dataStore = factory.createDataStore(params);

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

		save(data);

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
				.getFeatureWriter(dataStoreTypeName, equals, t)) {
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

	protected void toSimpleFeature(SimpleFeature featureToUpdate,
			RequestData data) {

		// "envelope:Polygon,id:java.lang.Long,queryString:String,path:String,startTime:Date, endTime:Date, totalTime:java.lang.Long");

		if (data.getBbox() != null) {

			com.vividsolutions.jts.geom.Polygon pol = JTS.toGeometry(data
					.getBbox());
			featureToUpdate.setAttribute("envelope", pol);
		}
		featureToUpdate.setAttribute("id", data.getId());
		featureToUpdate.setAttribute("path", data.getPath());

		if (data.getStartTime() != null)
			featureToUpdate.setAttribute("startTime", data.getStartTime());
		if (data.getEndTime() != null)
			featureToUpdate.setAttribute("endTime", data.getEndTime());

		if (data.getTotalTime() > 0)
			featureToUpdate.setAttribute("totalTime", data.getTotalTime());

		// data.getBodyAsString(); //data.setBody(body); ??
		if (data.getBodyAsString() != null) {
			featureToUpdate
					.setAttribute("BodyAsString", data.getBodyAsString());
		}

		// data.getBodyContentLength();
		// //data.setBodyContentLength(bodyContentLength);
		if (data.getBodyContentLength() > 0) {
			featureToUpdate.setAttribute("BodyContentLength",
					data.getBodyContentLength());
		}

		// data.getHost(); //data.setHost(host);
		if (data.getHost() != null) {
			featureToUpdate.setAttribute("Host", data.getHost());
		}

		// data.getErrorMessage(); //data.setErrorMessage(errorMessage);
		if (data.getErrorMessage() != null) {
			featureToUpdate
					.setAttribute("ErrorMessage", data.getErrorMessage());
		}

		// data.getHttpMethod(); //data.setHttpMethod(httpMethod);
		if (data.getHttpMethod() != null) {
			featureToUpdate.setAttribute("HttpMethod", data.getHttpMethod());
		}

		// data.getHttpReferer(); //data.setHttpReferer(httpReferer);
		if (data.getHttpReferer() != null) {
			featureToUpdate.setAttribute("HttpReferer", data.getHttpReferer());
		}

		data.getInternalHost(); // data.setInternalHost(internalHost);
		if (data.getInternalHost() != null) {
			featureToUpdate
					.setAttribute("InternalHost", data.getInternalHost());
		}

		// data.getOperation(); //data.setOperation(operation);
		if (data.getOperation() != null) {
			featureToUpdate.setAttribute("Operation", data.getOperation());
		}

		// data.getOwsVersion(); //data.setOwsVersion(owsVersion);
		if (data.getOwsVersion() != null) {
			featureToUpdate.setAttribute("OwsVersion", data.getOwsVersion());
		}

		// data.getQueryString(); //data.setQueryString(queryString);
		if (data.getQueryString() != null) {
			featureToUpdate.setAttribute("QueryString", data.getQueryString());
		}

		// data.getRemoteAddr(); //data.setRemoteAddr(remoteAddr);
		if (data.getRemoteAddr() != null) {
			featureToUpdate.setAttribute("RemoteAddr", data.getRemoteAddr());
		}

		// data.getRemoteCity(); //data.setRemoteCity(remoteCity);
		if (data.getRemoteCity() != null) {
			featureToUpdate.setAttribute("RemoteCity", data.getRemoteCity());
		}

		// data.getRemoteCountry(); //data.setRemoteCountry(remoteCountry);
		if (data.getRemoteCountry() != null) {
			featureToUpdate.setAttribute("RemoteCountry",
					data.getRemoteCountry());
		}

		// data.getRemoteHost(); //data.setRemoteHost(remoteHost);
		if (data.getRemoteHost() != null) {
			featureToUpdate.setAttribute("RemoteHost", data.getRemoteHost());
		}

		// data.getRemoteLat(); //data.setRemoteLat(remoteLat);
		if (data.getRemoteLat() > 0) {
			featureToUpdate.setAttribute("RemoteLat", data.getRemoteLat());
		}

		// data.getRemoteLon(); //data.setRemoteLon(remoteLon);
		if (data.getRemoteLon() > 0) {
			featureToUpdate.setAttribute("RemoteLon", data.getRemoteLon());
		}

		// data.getRemoteUser(); //data.setRemoteUser(remoteUser);
		if (data.getRemoteUser() != null) {
			featureToUpdate.setAttribute("RemoteUser", data.getRemoteUser());
		}

		// data.getRemoteUserAgent();
		// //data.setRemoteUserAgent(remoteUserAgent);
		if (data.getRemoteUserAgent() != null) {
			featureToUpdate.setAttribute("RemoteUserAgent",
					data.getRemoteUserAgent());
		}

		// data.getResources(); //data.setResources(resources); tratar lista
		if (data.getResources() != null && !data.getResources().isEmpty()) {
			String resources = "";
			for (String s : data.getResources()) {
				resources += s + ",";
			}
			resources.substring(0, resources.length() - 2);

			featureToUpdate.setAttribute("Resources", resources);
		}

		// data.getResponseContentType();
		// //data.setResponseContentType(responseContentType);
		if (data.getResponseContentType() != null) {
			featureToUpdate.setAttribute("ResponseContentType",
					data.getResponseContentType());
		}

		// data.getResponseLength(); //data.setResponseLength(responseLength);
		if (data.getResponseLength() > 0) {
			featureToUpdate.setAttribute("ResponseLength",
					data.getResponseLength());
		}

		// data.getResponseStatus(); //data.setResponseStatus(httpStatus);
		if (data.getResponseStatus() != null) {
			featureToUpdate.setAttribute("ResponseStatus",
					data.getResponseStatus());
		}

		// data.getService(); //data.setService(service);
		if (data.getService() != null) {
			featureToUpdate.setAttribute("Service", data.getService());
		}

		// data.getSubOperation(); //data.setSubOperation(subOperation);
		if (data.getSubOperation() != null) {
			featureToUpdate
					.setAttribute("SubOperation", data.getSubOperation());
		}

		// data.getStatus();
		if (data.getStatus() != null) {
			featureToUpdate.setAttribute("Status", data.getStatus().name());

		}

	}

	protected void toRequestData(SimpleFeature feature, RequestData dataToUpdate) {

		if (feature.getAttribute("envelope") != null) {

			com.vividsolutions.jts.geom.Polygon pol = (com.vividsolutions.jts.geom.Polygon) feature
					.getAttribute("Polygon");

			GeometryFactory geometryFactory = JTSFactoryFinder
					.getGeometryFactory();

			Geometry geo = (Geometry) geometryFactory.toGeometry(pol
					.getEnvelopeInternal());

			dataToUpdate.setBbox((BoundingBox) geo.getEnvelope());
		}
		dataToUpdate.setId((long) feature.getAttribute("id"));
		dataToUpdate.setPath((String) feature.getAttribute("path"));
		dataToUpdate.setStartTime((Date) feature.getAttribute("startTime"));
		dataToUpdate.setEndTime((Date) feature.getAttribute("endTime"));

		if (feature.getAttribute("totalTime") != null)
			dataToUpdate.setTotalTime((long) feature.getAttribute("totalTime"));

		if (feature.getAttribute("BodyAsString") != null)
			dataToUpdate
					.setBody(((String) feature.getAttribute("BodyAsString"))
							.getBytes());

		if (feature.getAttribute("BodyContentLength") != null)
			dataToUpdate.setBodyContentLength((long) feature
					.getAttribute("BodyContentLength"));

		if (feature.getAttribute("Host") != null)
			dataToUpdate.setHost((String) feature.getAttribute("Host"));

		dataToUpdate.setErrorMessage((String) feature
				.getAttribute("ErrorMessage"));

		dataToUpdate.setErrorMessage((String) feature
				.getAttribute("ErrorMessage"));

		dataToUpdate.setHttpMethod((String) feature.getAttribute("HttpMethod"));

		dataToUpdate.setHttpReferer((String) feature
				.getAttribute("HttpReferer"));

		dataToUpdate.setInternalHost((String) feature
				.getAttribute("InternalHost"));

		dataToUpdate.setOperation((String) feature.getAttribute("Operation"));

		dataToUpdate.setOwsVersion((String) feature.getAttribute("OwsVersion"));

		dataToUpdate.setQueryString((String) feature
				.getAttribute("QueryString"));

		dataToUpdate.setRemoteAddr((String) feature.getAttribute("RemoteAddr"));

		dataToUpdate.setRemoteCity((String) feature.getAttribute("RemoteCity"));

		dataToUpdate.setRemoteCountry((String) feature
				.getAttribute("RemoteCountry"));

		dataToUpdate.setRemoteHost((String) feature.getAttribute("RemoteHost"));

		if (feature.getAttribute("RemoteLat") != null)
			dataToUpdate.setRemoteLat((double) feature
					.getAttribute("RemoteLat"));

		if (feature.getAttribute("RemoteLon") != null)
			dataToUpdate.setRemoteLon((double) feature
					.getAttribute("RemoteLon"));

		dataToUpdate.setRemoteUser((String) feature.getAttribute("RemoteUser"));

		dataToUpdate.setRemoteUserAgent((String) feature
				.getAttribute("RemoteUserAgent"));

		String str = (String) feature.getAttribute("Resources");

		if (str != null) {
			List<String> list = new LinkedList<String>();
			for (String s : str.split(",")) {
				list.add(s);
			}
			dataToUpdate.setResources(list);

		}

		dataToUpdate.setResponseContentType((String) feature
				.getAttribute("ResponseContentType"));

		if (feature.getAttribute("ResponseLength") != null)
			dataToUpdate.setResponseLength((long) feature
					.getAttribute("ResponseLength"));

		dataToUpdate.setResponseStatus((Integer) feature
				.getAttribute("ResponseStatus"));

		dataToUpdate.setService((String) feature.getAttribute("Service"));

		dataToUpdate.setSubOperation((String) feature
				.getAttribute("SubOperation"));

		org.geoserver.monitor.RequestData.Status status = org.geoserver.monitor.RequestData.Status
				.valueOf((String) feature.getAttribute("Status"));
		dataToUpdate.setStatus(status);

		//
	}

	@Override
	public void save(RequestData data) {
		Transaction t = new DefaultTransaction("handle");
		try (FeatureWriter<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureWriterAppend(dataStoreTypeName, t)) {
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
				.getFeatureReader(new Query(dataStoreTypeName, equals), t)) {

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
	public List<RequestData> getOwsRequests() {
		// TODO Auto-generated method stub...ignore for now!!!
		return new ArrayList<RequestData>();
	}

	@Override
	public List<RequestData> getOwsRequests(String service, String operation,
			String version) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void clear() {
	}

	@Override
	public void dispose() {
	}
	
	
	public org.opengis.filter.Filter convertFilter(FilterFactoryImpl factory, org.geoserver.monitor.Filter filter) {

		
		if (filter instanceof And) {
			
			And andFilter = (And)filter;
			List<org.opengis.filter.Filter> newChildren = new ArrayList<org.opengis.filter.Filter>();
			for (org.geoserver.monitor.Filter child : andFilter.getFilters()) {
				newChildren.add(convertFilter(factory,  child));
			}
			
			return factory.and(newChildren);
		}
		else{ 
			
			List filters = new LinkedList<Filter>();
			Filter fil = factory.or(filters);
			Or orFilter = (Or) filter;
			
			for (org.geoserver.monitor.Filter child : orFilter.getFilters()) {
				filters.add(convertFilter(factory,  child));
			}
			
			
			return factory.or(filters);
		}
		
		
		
	}


	@Override
	public List<RequestData> getRequests(org.geoserver.monitor.Query query) {

		List<RequestData> list = new LinkedList<RequestData>();

		Transaction t = new DefaultTransaction("handle");

		FilterFactoryImpl factory = new FilterFactoryImpl();
		Expression exp1 = factory.property("Query");
		Expression exp2 = factory.literal(query);
		Filter equals = factory.equal(exp1, exp2, false);
		
		Filter filt = convertFilter(factory, query.getFilter());
		
	
		try (FeatureReader<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureReader(new Query(dataStoreTypeName, filt), t)) {

			if (fw.hasNext()) {
				RequestData data = new RequestData();
				toRequestData(fw.next(), data);
				list.add(data);
				t.commit();
			}

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
		return list;
	}

	@Override
	public void getRequests(org.geoserver.monitor.Query query,
			RequestDataVisitor visitor) {

		final Iterator<RequestData> it = getIterator(query);
		while (it.hasNext()) {
			visitor.visit(it.next());
		}
	}

	@Override
	public long getCount(org.geoserver.monitor.Query query) {
		// TODO
		return 0;
	}

	@Override
	public Iterator<RequestData> getIterator(org.geoserver.monitor.Query query) {
		return getRequests(query).iterator();
	}

	@Override
	public List<RequestData> getRequests() {
		return getRequests(null);
	}

}
