package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.PageParameters;
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

	private String selectedDataStoreName = "selected";
	private String message = "";
	Holder holder = null;

	public GSMonitorPage() {

		if (!getHolder().isOK()) {
			message = "Monitor cannot be configured due to error. Please consult log files for details.";
		}

		final FeatureMonitorDAO dao = getHolder().getDAO();
		dao.init(getHolder().getConfig());
		final String configuredStoreID = dao.getStoreID();

		final List<DataStoreInfo> dataStores = getHolder().getDataStores();

		List store = new ArrayList<String>();

		if (dataStores != null) {

			for (DataStoreInfo dataStore : dataStores) {

				if (dataStore != null && dataStore.isEnabled()) {

					store.add(dataStore.getName());
					if (dataStore.getName().equals(configuredStoreID))
						selectedDataStoreName = dataStore.getName();

				}

			}

		}
		final Model<String> adminMessageModel = Model
				.of("Messages: " + message);
		final Label adminMessage = new Label("adminMessage", adminMessageModel);
		adminMessage.setOutputMarkupId(true);
		add(adminMessage);

		Form form = new Form("form", new Model(this));

		form.setOutputMarkupId(true);

		add(form);

		final Model<String> dsNameModel = Model.of("DataStore in use: "
				+ dao.getStoreID());
		final Label dsName = new Label("dsName", dsNameModel);
		dsName.setOutputMarkupId(true);
		form.add(dsName);

		final DropDownChoice<String> option = new DropDownChoice<String>(
				"options", new Model(selectedDataStoreName), store);
		form.add(option);

		final Model<String> featureTypeModel = Model.of("FeatureType: "
				+ dao.getDataStoreTypeName());
		final Label featureType = new Label("featureType", featureTypeModel);
		featureType.setOutputMarkupId(true);
		form.add(featureType);

		final TextField<String> newFeatureType = new TextField<String>(
				"newFeatureType", new Model());
		form.add(newFeatureType);

		form.add(new AjaxButton("save", form) {

			protected void onSubmit(AjaxRequestTarget target, Form f) {

				if (!getHolder().isOK()) {
					message = "Monitor cannot be configured due to error. Please consult log files for details.";
					adminMessageModel.setObject("Messages: " + message);
					target.addComponent(adminMessage);
				} else {

					String selected = option.getModelObject();

					if (selected != null && dao != null) {

						DataStoreInfo data = null;

						for (DataStoreInfo dsi : dataStores) {

							if (dsi.getName().equals(selected)) {
								// set the featureType
								dao.setDataStoreTypeName(newFeatureType
										.getInputName());
								// set the dataStore
								dao.updateDataStoreProperties(dsi.getName(),
										dsi.getConnectionParameters());
								dao.init(holder.getConfig());

								message = "Monitor Configured";
								adminMessageModel.setObject("Messages: "
										+ message);
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

	public Holder getHolder() {
		if (holder == null) {
			holder = new Holder(getCatalog());
		}
		return holder;
	}

	public class Holder {

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
