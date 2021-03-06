package org.locationtech.gsmonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.monitor.And;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.monitor.Or;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

public class FeatureMonitorDAO implements MonitorDAO {

	private final static Logger LOGGER = Logging
			.getLogger(FeatureMonitorDAO.class);

	public static final String TYPENAME = "requestDataFeature";
	public static final String TYPENAME_PROP_NAME = "mds.type";
	public static final String DSID_PROP_NAME = "mds.id";
	public static final String FEATURE_PROP_FILENAME = "featureMonitor.properties";
	public static final int MAX_SIZE = 255;
	private DataStore dataStore = null;
	private String dataStoreName = "";
	private MonitorConfig config;
	private boolean configured = false;

	private GeoServerDataDirectory dataDirectory;

	public static CoordinateReferenceSystem CRSI;

	static {
		try {
			CRSI = CRS.decode("EPSG:4326");
		} catch (FactoryException e) {
			LOGGER.log(Level.SEVERE, "Failed to CRS decode", e);
		}
	}

	private SimpleFeatureType featureType = null;

	String dataStoreTypeName = TYPENAME;

	private Map<String, Serializable> dataStoreParams = new HashMap<String, Serializable>();

	public FeatureMonitorDAO() {
		LOGGER.info("Created");
	}

	@Override
	public String getName() {
		return "vector";
	}

	public GeoServerDataDirectory getDataDirectory() {
		return dataDirectory;
	}

	public void setDataDirectory(GeoServerDataDirectory dataDir) {
		this.dataDirectory = dataDir;
	}

	public SimpleFeatureType getFeatureType() {
		return featureType;
	}

	public String getDataStoreTypeName() {
		return dataStoreTypeName;
	}

	public void setDataStoreTypeName(String dataStoreTypeName) {
		if (dataStoreTypeName == null || dataStoreTypeName.length() == 0) {
			LOGGER.log(Level.WARNING,
					"Tried to set the Feature Type name to an null or empty value");
			return;
		}
		this.dataStoreTypeName = dataStoreTypeName;
	}

	public Map<String, Serializable> getDataStoreParams() {
		return dataStoreParams;
	}

	private void setDataStoreParams(Map<String, Serializable> dataStoreParams) {
		this.dataStoreParams = dataStoreParams;
		if (dataStoreParams.containsKey(TYPENAME_PROP_NAME)) {
			String typeName = dataStoreParams.get(TYPENAME_PROP_NAME)
					.toString();
			this.dataStoreTypeName = typeName.length() > 0 ? typeName
					: dataStoreTypeName;
			this.featureType = null;
			this.initType();
		}
	}

	private synchronized void initType() {
		if (featureType == null
				|| !featureType.getName().getLocalPart()
						.equals(dataStoreTypeName)) {
			try {
				featureType = DataUtilities
						.createType(
								dataStoreTypeName,
								"envelope:com.vividsolutions.jts.geom.Polygon,id:java.lang.Long,queryString:String,path:String,startTime:Date,"
										+ "endTime:Date,totalTime:java.lang.Long,BodyAsString:String,BodyContentLength:java.lang.Long,"
										+ "Host:String,ErrorMessage:String,HttpMethod:String,HttpReferer:String,InternalHost:String,"
										+ "Operation:String,OwsVersion:String,QueryString:String,RemoteAddr:String,RemoteCity:String,"
										+ "RemoteCountry:String,RemoteHost:String,RemoteLat:double,"
										+ "RemoteLon:double,RemoteUser:String,RemoteUserAgent:String,Resources:String,"
										+ "ResponseContentType:String,ResponseLength:java.lang.Long,ResponseStatus:int,"
										+ "Service:String,SubOperation:String,Status:String,Category:String");
			} catch (SchemaException e) {
				LOGGER.log(Level.SEVERE, "Failed to initialized feature type",
						e);
			}
		}
	}

	private synchronized void initStore() {
		final String requestDataStoreName = (String) dataStoreParams
				.get(DSID_PROP_NAME);
		if (requestDataStoreName != null
				&& !requestDataStoreName.equals(dataStoreName)
				&& dataStore != null) {
			dataStore.dispose();
			dataStore = null;
		}
		if (dataStore == null) {
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
						dataStoreName = (String) params.get(DSID_PROP_NAME);
						return;
					}					
				} catch (Throwable warning) {
					LOGGER.warning(factory.getDisplayName() + " failed:"
							+ warning);
					warning.printStackTrace();
				}
			}
		}
	}

	/**
	 * @throws IOException
	 *             IllegalStateException if the invalid type of Monitor
	 *             installed
	 */
	public static final FeatureMonitorDAO lookupMonitor(
			final MonitorConfig config) throws IOException {
		final MonitorDAO dao = config.createDAO();
		if (dao == null || !(dao instanceof FeatureMonitorDAO)) {
			throw new java.lang.IllegalStateException(
					"Expected a Feature Monitor DAO, found "
							+ dao.getClass().toString());
		}
		return (FeatureMonitorDAO) dao;
	}

	public void updateDataStoreProperties(final String dataStoreID,
			final Map<String, Serializable> properties) {
		File monitoringDir = null;
		if (dataDirectory == null) {
			LOGGER.warning("Data Directory not provided. Check configuration.");
		}
		try {
			monitoringDir = dataDirectory == null ? new File(".")
					: dataDirectory.findOrCreateDir("monitoring");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to find or create the monitoring directory", e);
		}
		File dbprops = new File(monitoringDir, FEATURE_PROP_FILENAME);
		if (dbprops.exists())
			dbprops.delete();
		try (FileWriter writer = new FileWriter(dbprops)) {
			for (Map.Entry<String, Serializable> property : properties
					.entrySet()) {
				writer.write(property.getKey());
				writer.write("=");
				writer.write(property.getValue().toString());
				writer.write("\n");
			}
			writer.write(DSID_PROP_NAME);
			writer.write("=");
			writer.write(dataStoreID);
			writer.write("\n");
			writer.write(TYPENAME_PROP_NAME);
			writer.write("=");
			writer.write(dataStoreTypeName);
			writer.write("\n");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to read the file in monitoring directory", e);
		}
	}

	/**
	 * If configured through the UI, returns a id.
	 */
	public String getStoreID() {
		if (dataStoreParams.containsKey(DSID_PROP_NAME))
			return dataStoreParams.get(DSID_PROP_NAME).toString();
		else
			return null;
	}

	@Override
	public void init(MonitorConfig config) {
		this.config = config;
		configureFromFile();
		Enumeration<Object> key = config.getProperties().keys();
		while (key.hasMoreElements()) {
			Object keyValue = key.nextElement();
			this.dataStoreParams.put(keyValue.toString(), config
					.getProperties().getProperty(keyValue.toString()));
		}
		this.initType();
		this.initStore();

		configured = true;
	}

	@Override
	public RequestData init(RequestData data) {
		if (!configured) {
			configureFromFile();
			this.initType();
			this.initStore();
		}
		save(data);
		return data;
	}

	private void configureFromFile() {
		File monitoringDir = null;
		if (dataDirectory == null) {
			LOGGER.warning("Data Directory not provided. Check configuration.");
		}
		try {
			monitoringDir = dataDirectory == null ? new File(".")
					: dataDirectory.findOrCreateDir("monitoring");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to find or create the monitoring directory", e);
		}
		File dbprops = new File(monitoringDir, FEATURE_PROP_FILENAME);
		if (dbprops.exists()) {

			try (BufferedReader bufferReader = new BufferedReader(
					new FileReader(dbprops))) {
				String out = "";
				Map<String, Serializable> params = new HashMap<String, Serializable>();
				while ((out = bufferReader.readLine()) != null) {
					if (out.length() > 0 && out.toCharArray()[0] != '#') {
						String[] newParams = out.split("=");
						if (newParams.length > 1)
							params.put(newParams[0].trim(), newParams[1].trim());
					}
				}
				setDataStoreParams(params);

			} catch (IOException e) {
				LOGGER.log(Level.SEVERE,
						"Failed to read the file in monitoring directory", e);

			}

		}
	}

	@Override
	public void add(RequestData data) {
		if (data.getBbox() == null)
			return;
		this.save(data);
	}

	@Override
	public void update(RequestData data) {
		Transaction t = new DefaultTransaction("handle");

		if (data.getId() <= 0) {
			data.setId(System.currentTimeMillis());
			this.add(data);
			return;
		}

		FilterFactoryImpl factory = new FilterFactoryImpl();
		Expression exp1 = factory.property("id");
		Expression exp2 = factory.literal(data.getId());
		Filter equals = factory.equals(exp1, exp2);

		try (FeatureWriter<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureWriter(dataStoreTypeName, equals, t)) {
			while (fw.hasNext()) {
				SimpleFeature newFeature = fw.next();
				toSimpleFeature(newFeature, data);
				fw.write();
			}

			t.commit();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to update feature with id " + data.getId(), e);
		} finally {
			try {
				t.close();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Failed to rollback", e);
			}
		}
	}

	protected void toSimpleFeature(SimpleFeature featureToUpdate,
			RequestData data) {

		if (data.getId() <= 0) {
			data.setId(System.currentTimeMillis());
		}
		if (data.getStartTime() == null) {
			data.setStartTime(Calendar.getInstance().getTime());
		}
		// need to fix this...null is not allowed
		com.vividsolutions.jts.geom.Polygon pol = (data.getBbox() != null) ? JTS
				.toGeometry(data.getBbox())
				: JTS.toGeometry(new ReferencedEnvelope(
						CRSI.getCoordinateSystem().getAxis(0).getMinimumValue(),
						CRSI.getCoordinateSystem().getAxis(0).getMaximumValue(),
						CRSI.getCoordinateSystem().getAxis(1).getMinimumValue(),
						CRSI.getCoordinateSystem().getAxis(1).getMaximumValue(),
						CRSI));

		featureToUpdate.setAttribute("envelope", pol);
		featureToUpdate.setAttribute("id", data.getId());
		featureToUpdate.setAttribute("path", data.getPath());

		if (data.getStartTime() != null)
			featureToUpdate.setAttribute("startTime", data.getStartTime());
		if (data.getEndTime() != null)
			featureToUpdate.setAttribute("endTime", data.getEndTime());

		if (data.getTotalTime() > 0)
			featureToUpdate.setAttribute("totalTime", data.getTotalTime());

		if (data.getBodyAsString() != null) {
			featureToUpdate.setAttribute("BodyAsString",
					clip(data.getBodyAsString()));
		}

		if (data.getBodyContentLength() > 0) {
			featureToUpdate.setAttribute("BodyContentLength",
					data.getBodyContentLength());
		}

		if (data.getHost() != null) {
			featureToUpdate.setAttribute("Host", data.getHost());
		}

		if (data.getErrorMessage() != null) {
			featureToUpdate
					.setAttribute("ErrorMessage", data.getErrorMessage());
		}

		if (data.getHttpMethod() != null) {
			featureToUpdate.setAttribute("HttpMethod", data.getHttpMethod());
		}

		if (data.getHttpReferer() != null) {
			featureToUpdate.setAttribute("HttpReferer",
					clip(data.getHttpReferer()));
		}

		if (data.getInternalHost() != null) {
			featureToUpdate
					.setAttribute("InternalHost", data.getInternalHost());
		}

		if (data.getOperation() != null) {
			featureToUpdate
					.setAttribute("Operation", clip(data.getOperation()));
		}

		if (data.getOwsVersion() != null) {
			featureToUpdate.setAttribute("OwsVersion", data.getOwsVersion());
		}

		if (data.getQueryString() != null) {
			featureToUpdate.setAttribute("QueryString",
					clip(data.getQueryString()));
		}

		if (data.getRemoteAddr() != null) {
			featureToUpdate.setAttribute("RemoteAddr", data.getRemoteAddr());
		}

		if (data.getRemoteCity() != null) {
			featureToUpdate.setAttribute("RemoteCity", data.getRemoteCity());
		}

		if (data.getRemoteCountry() != null) {
			featureToUpdate.setAttribute("RemoteCountry",
					data.getRemoteCountry());
		}

		if (data.getRemoteHost() != null) {
			featureToUpdate.setAttribute("RemoteHost", data.getRemoteHost());
		}

		if (data.getRemoteLat() > 0) {
			featureToUpdate.setAttribute("RemoteLat", data.getRemoteLat());
		}

		if (data.getRemoteLon() > 0) {
			featureToUpdate.setAttribute("RemoteLon", data.getRemoteLon());
		}

		if (data.getRemoteUser() != null) {
			featureToUpdate.setAttribute("RemoteUser", data.getRemoteUser());
		}

		if (data.getRemoteUserAgent() != null) {
			featureToUpdate.setAttribute("RemoteUserAgent",
					data.getRemoteUserAgent());
		}

		if (data.getResources() != null && !data.getResources().isEmpty()) {
			StringBuffer buffer = new StringBuffer();
			for (String s : data.getResources()) {
				buffer.append(s).append(',');
			}
			buffer.delete(buffer.length() - 1, buffer.length());

			featureToUpdate.setAttribute("Resources", clip(buffer.toString()));
		}

		if (data.getResponseContentType() != null) {
			featureToUpdate.setAttribute("ResponseContentType",
					data.getResponseContentType());
		}

		if (data.getResponseLength() > 0) {
			featureToUpdate.setAttribute("ResponseLength",
					data.getResponseLength());
		}

		if (data.getResponseStatus() != null) {
			featureToUpdate.setAttribute("ResponseStatus",
					data.getResponseStatus());
		}

		if (data.getService() != null) {
			featureToUpdate.setAttribute("Service", data.getService());
		}

		if (data.getSubOperation() != null) {
			featureToUpdate
					.setAttribute("SubOperation", data.getSubOperation());
		}

		if (data.getStatus() != null) {
			featureToUpdate.setAttribute("Status", data.getStatus().name());
		}

		if (data.getCategory() != null) {
			featureToUpdate.setAttribute("Category", data.getCategory().name());
		}

	}

	private CoordinateReferenceSystem resolve(
			final CoordinateReferenceSystem crs) {
		return crs != null ? crs : (config != null
				&& config.getBboxCrs() != null ? config.getBboxCrs() : CRSI);
	}

	private String clip(String value) {
		return (value.length() > MAX_SIZE) ? value.substring(0, MAX_SIZE)
				: value;
	}

	protected void toRequestData(SimpleFeature feature, RequestData dataToUpdate) {

		if (feature.getAttribute("envelope") != null) {

			com.vividsolutions.jts.geom.Polygon pol = (com.vividsolutions.jts.geom.Polygon) feature
					.getAttribute("envelope");

			Envelope envelope = pol.getEnvelopeInternal();
			dataToUpdate.setBbox(new ReferencedEnvelope(envelope.getMinX(),
					envelope.getMaxX(), envelope.getMinY(), envelope.getMaxY(),
					resolve(feature.getDefaultGeometryProperty().getBounds()
							.getCoordinateReferenceSystem())));
		}
		dataToUpdate.setId(((Number) feature.getAttribute("id")).longValue());
		dataToUpdate.setPath((String) feature.getAttribute("path"));
		dataToUpdate.setStartTime((Date) feature.getAttribute("startTime"));
		dataToUpdate.setEndTime((Date) feature.getAttribute("endTime"));

		if (feature.getAttribute("totalTime") != null)
			dataToUpdate.setTotalTime(((Number) feature
					.getAttribute("totalTime")).longValue());

		if (feature.getAttribute("BodyAsString") != null)
			dataToUpdate
					.setBody(((String) feature.getAttribute("BodyAsString"))
							.getBytes());

		if (feature.getAttribute("BodyContentLength") != null)
			dataToUpdate.setBodyContentLength(((Number) feature
					.getAttribute("BodyContentLength")).longValue());

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
			dataToUpdate.setRemoteLat(((Number) feature
					.getAttribute("RemoteLat")).doubleValue());

		if (feature.getAttribute("RemoteLon") != null)
			dataToUpdate.setRemoteLon(((Number) feature
					.getAttribute("RemoteLon")).doubleValue());

		dataToUpdate.setRemoteUser((String) feature.getAttribute("RemoteUser"));

		dataToUpdate.setRemoteUserAgent((String) feature
				.getAttribute("RemoteUserAgent"));

		String str = (String) feature.getAttribute("Resources");

		if (str != null) {
			dataToUpdate.setResources(Arrays.asList(str.split(",")));
		}

		dataToUpdate.setResponseContentType((String) feature
				.getAttribute("ResponseContentType"));

		if (feature.getAttribute("ResponseLength") != null)
			dataToUpdate.setResponseLength(((Number) feature
					.getAttribute("ResponseLength")).longValue());

		dataToUpdate.setResponseStatus((Integer) feature
				.getAttribute("ResponseStatus"));

		dataToUpdate.setService((String) feature.getAttribute("Service"));

		dataToUpdate.setSubOperation((String) feature
				.getAttribute("SubOperation"));

		org.geoserver.monitor.RequestData.Status status = org.geoserver.monitor.RequestData.Status
				.valueOf((String) feature.getAttribute("Status"));
		dataToUpdate.setStatus(status);

		org.geoserver.monitor.RequestData.Category category = org.geoserver.monitor.RequestData.Category
				.valueOf((String) feature.getAttribute("Category"));
		dataToUpdate.setCategory(category);

	}

	@Override
	public void save(RequestData data) {

		if (dataStore == null)
			return;
		Transaction t = new DefaultTransaction("handle");
		try (FeatureWriter<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureWriterAppend(dataStoreTypeName, t)) {
			SimpleFeature newFeature = fw.next();
			toSimpleFeature(newFeature, data);
			fw.write();
			fw.close();
			t.commit();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE,
					"Failed to write feature with id " + data.getId(), e);
			try {
				t.rollback();
			} catch (IOException e1) {
				LOGGER.log(Level.SEVERE, "Failed to rollback", e1);
			}
		} finally {
			try {
				t.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Transanction close failure", e);
			}

		}
	}

	@Override
	public RequestData getRequest(long id) {
		if (dataStore == null)
			return null;
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

			LOGGER.log(Level.SEVERE, "Failed to request feature with id " + id,
					e);
		} finally {
			try {
				t.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Transanction close failure", e);
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
		return new ArrayList<RequestData>();
	}

	@Override
	public void clear() {
	}

	@Override
	public void dispose() {
	}

	protected org.opengis.filter.Filter convertFilter(
			FilterFactoryImpl factory, org.geoserver.monitor.Filter filter) {

		if (filter == null) {
			return Filter.INCLUDE;
		}
		if (filter instanceof And) {

			And andFilter = (And) filter;
			List<org.opengis.filter.Filter> newChildren = new ArrayList<org.opengis.filter.Filter>();
			for (org.geoserver.monitor.Filter child : andFilter.getFilters()) {
				newChildren.add(convertFilter(factory, child));
			}

			return factory.and(newChildren);
		} else if (filter instanceof Or) {

			Or orFilter = (Or) filter;
			List<org.opengis.filter.Filter> newChildren = new ArrayList<org.opengis.filter.Filter>();
			for (org.geoserver.monitor.Filter child : orFilter.getFilters()) {
				newChildren.add(convertFilter(factory, child));
			}

			return factory.or(newChildren);
		} else {
			switch (filter.getType()) {
			case EQ:
				return factory.equals(factory.property(getName(filter)),
						factory.literal(getValue(filter)));
			case GTE:
				return factory.greaterOrEqual(
						factory.property(getName(filter)),
						factory.literal(getValue(filter)), false);
			case GT:
				return factory.greater(factory.property(getName(filter)),
						factory.literal(getValue(filter)));
			case LTE:
				return factory.lessOrEqual(factory.property(getName(filter)),
						factory.literal(getValue(filter)));
			case LT:
				return factory.less(factory.property(getName(filter)),
						factory.literal(getValue(filter)));
			case NEQ:
				return factory.notEqual(factory.property(getName(filter)),
						factory.literal(getValue(filter)));
			case IN:
				List<org.opengis.filter.Filter> newChildren = new ArrayList<org.opengis.filter.Filter>();
				for (Object obj : (List) getValue(filter)) {
					newChildren.add(factory.equals(
							factory.property(getName(filter)),
							factory.literal(obj)));
				}
				return factory.or(newChildren);
			}
			return Filter.INCLUDE;
		}
	}

	private Object getValue(org.geoserver.monitor.Filter filter) {
		return isProperty(filter.getLeft()) ? filter.getRight() : filter
				.getLeft();
	}

	private String getPropertyName(org.geoserver.monitor.Filter filter) {
		return isProperty(filter.getLeft()) ? filter.getLeft().toString()
				: filter.getRight().toString();
	}

	boolean isProperty(Object obj) {
		if (obj instanceof String) {
			String s = (String) obj;
			return "resource".equals(s) || OwsUtils.has(new RequestData(), s);
		}
		return false;
	}

	private String resolveName(String name) {
		for (AttributeDescriptor descriptor : this.featureType
				.getAttributeDescriptors()) {
			if (descriptor.getLocalName().equalsIgnoreCase(name))
				return descriptor.getLocalName();
		}
		return name;
	}

	private Name getName(org.geoserver.monitor.Filter filter) {
		return new NameImpl(resolveName(getPropertyName(filter)));
	}

	private Filter getQueryFilter(org.geoserver.monitor.Query query) {
		final FilterFactoryImpl factory = new FilterFactoryImpl();
		Filter queryFilter = query == null ? Filter.INCLUDE : convertFilter(
				factory, query.getFilter());

		if (query != null && query.getToDate() != null
				&& query.getFromDate() != null) {
			Filter betweenFilter = factory.between(
					factory.property(new NameImpl("startTime")),
					factory.literal(query.getFromDate()),
					factory.literal(query.getToDate()));
			if (queryFilter == Filter.INCLUDE)
				queryFilter = betweenFilter;
			else {
				betweenFilter = factory.and(Arrays.asList(queryFilter,
						betweenFilter));
			}
		}
		return queryFilter;
	}

	@Override
	public List<RequestData> getRequests(org.geoserver.monitor.Query query) {

		List<RequestData> list = new LinkedList<RequestData>();

		if (dataStore == null)
			return list;

		Transaction t = new DefaultTransaction("handle");

		try (FeatureReader<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureReader(new Query(dataStoreTypeName,
						getQueryFilter(query)), t)) {

			while (fw.hasNext()) {
				RequestData data = new RequestData();
				toRequestData(fw.next(), data);
				list.add(data);

			}
			t.commit();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Cannot execute query "
					+ getQueryFilter(query), e);
		} finally {
			try {
				t.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Transanction close failure", e);
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

		int count = 0;
		if (dataStore == null)
			return count;
		Transaction t = new DefaultTransaction("handle");

		try (FeatureReader<SimpleFeatureType, SimpleFeature> fw = dataStore
				.getFeatureReader(new Query(dataStoreTypeName,
						getQueryFilter(query)), t)) {

			while (fw.hasNext()) {
				RequestData data = new RequestData();
				toRequestData(fw.next(), data);
				count++;

			}
			t.commit();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Cannot execute query "
					+ getQueryFilter(query), e);
		} finally {
			try {
				t.close();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, "Transanction close failure", e);
			}
		}
		return count;
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
