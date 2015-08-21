package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.web.GeoServerSecuredPage;
import org.locationtech.gsmonitor.*;

public class GSMonitorPage extends GeoServerSecuredPage {

	public static final long serialVersionUID = 438L;

	private String selectedDataStoreName = "NA";
	private String message = "";
	transient Holder holder = null;
	private Model<String> adminMessageModel;
	private Label adminMessage;
	private Model<String> dsNameModel;
	private Label dsName;
	private DropDownChoice<String> option;
	private Model<String> featureTypeModel;
	private Label featureType;
	private TextField<String> newFeatureType;

	public GSMonitorPage() {

		List<String> store = new ArrayList<String>();

		String featureTypeName = "";
		if (!getHolder(getApplication()).isOK()) {
			message = "Monitor cannot be configured due to error. Please consult log files for details.";
		} else {
			FeatureMonitorDAO dao = getHolder(getApplication()).getDAO();
			dao.init(getHolder(getApplication()).getConfig());
			String configuredStoreID = dao.getStoreID();

			List<DataStoreInfo> dataStores = getHolder(getApplication())
					.getDataStores();

			if (dataStores != null) {

				for (DataStoreInfo dataStore : dataStores) {

					if (dataStore != null && dataStore.isEnabled()) {

						store.add(dataStore.getName());
						if (dataStore.getName().equals(configuredStoreID))
							selectedDataStoreName = dataStore.getName();
					}

				}
			}
			featureTypeName = dao.getDataStoreTypeName();
		}

		adminMessageModel = Model.of("Messages: " + message);
		adminMessage = new Label("adminMessage", adminMessageModel);
		adminMessage.setOutputMarkupId(true);
		add(adminMessage);
		
		Form form = new Form("form", new Model(this));

		form.setOutputMarkupId(true);

		add(form);

		dsNameModel = Model.of("DataStore in use: " + selectedDataStoreName);
		dsName = new Label("dsName", dsNameModel) {
			private static final long serialVersionUID = 5L;
		};
		dsName.setOutputMarkupId(true);
		form.add(dsName);

		option = new DropDownChoice<String>("options",
				Model.of(selectedDataStoreName), store);
		form.add(option);

		featureTypeModel = Model.of("FeatureType: " + featureTypeName);
		featureType = new Label("featureType", featureTypeModel);
		featureType.setOutputMarkupId(true);
		form.add(featureType);

		newFeatureType = new TextField<String>("newFeatureType", Model.of(""));

		form.add(newFeatureType);

		form.add(new AjaxButton("save", form) {
			private static final long serialVersionUID = 1L;

			protected void onSubmit(AjaxRequestTarget target, Form f) {

				if (!getHolder(getApplication()).isOK()) {
					message = "Monitor cannot be configured due to error. Please consult log files for details.";
					adminMessageModel.setObject("Messages: " + message);
					target.addComponent(adminMessage);
				} else {
					final FeatureMonitorDAO dao = getHolder(getApplication())
							.getDAO();
					final String selected = option.getModelObject();
					if (selected != null && dao != null) {
						for (DataStoreInfo dsi : getHolder(getApplication())
								.getDataStores()) {

							if (dsi.getName().equals(selected)) {
								// set the featureType
								String ftype = newFeatureType
										.getDefaultModelObjectAsString();
								if (ftype != null && ftype.length() > 0) {
									dao.setDataStoreTypeName(ftype.replaceAll(
											"\\W", ""));
								}

								// set the dataStore
								dao.updateDataStoreProperties(dsi.getName(),
										dsi.getConnectionParameters());
								dao.init(getHolder(getApplication())
										.getConfig());

								adminMessageModel
										.setObject("Messages: Monitor Configured");
								target.addComponent(adminMessage);

								featureTypeModel.setObject("FeatureType: "
										+ dao.getDataStoreTypeName());
								target.addComponent(featureType);

								dsNameModel.setObject("DataStore in use: "
										+ dao.getStoreID());
								target.addComponent(dsName);

								return;
							}

						}

						message = "Data Store not valid";
						adminMessageModel.setObject("Messages: " + message);
						target.addComponent(adminMessage);
					}

				}

			}

		});

	}

	public Holder getHolder(org.apache.wicket.Application app) {
		if (holder == null) {
			holder = new Holder(
					((org.geoserver.web.GeoServerApplication) app).getCatalog());
		}
		return holder;
	}

	public static class Holder {

		Catalog catalog;
		MonitorConfig config;

		public Holder(Catalog cat) {
			this.catalog = cat;
			try {
				this.config = new MonitorConfig(catalog.getResourceLoader());
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Impossible to Initialize the Holder",
						e);
			}
		}

		public FeatureMonitorDAO getDAO() {
			try {
				return FeatureMonitorDAO.lookupMonitor(config);
			} catch (IOException e) {
				return null;
			}
		}

		public List<DataStoreInfo> getDataStores() {
			return catalog.getStores(DataStoreInfo.class);
		}

		public MonitorConfig getConfig() {
			return config;
		}

		public boolean isOK() {
			return config != null;
		}

	}

}
