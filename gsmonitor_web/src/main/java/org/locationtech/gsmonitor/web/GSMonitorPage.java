package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.web.GeoServerSecuredPage;
import org.locationtech.gsmonitor.*;

public class GSMonitorPage extends GeoServerSecuredPage {

	private String selectedDataStoreName = "selected";
	private String dataStoreInUse = "";
	private String message = "";

	public GSMonitorPage() {

		try {
			final MonitorConfig config = new MonitorConfig(getCatalog()
					.getResourceLoader());

			final FeatureMonitorDAO dao = FeatureMonitorDAO
					.lookupMonitor(config);
			dao.init(config);
			final String configuredStoreID = dao.getStoreID();

			final List<DataStoreInfo> dataStores = getCatalog().getStores(

			DataStoreInfo.class);

			List store = new ArrayList<String>();

			if (dataStores != null) {

				for (DataStoreInfo data : dataStores) {

					if (data != null) {

						store.add(data.getName());
						if (data.getName().equals(configuredStoreID))
							selectedDataStoreName = data.getName();

					}

				}

			}
			final Model<String> adminMessageModel =Model.of("Mensages: "+message);
			final Label adminMessage = new Label("adminMessage", adminMessageModel);
			adminMessage.setOutputMarkupId(true);
			add(adminMessage);
			
			
			
			Form form = new Form("form", new Model(this));

			form.setOutputMarkupId(true);

			add(form);
			
			
			final Model<String> dsNameModel = Model.of("DataStore in use: "
					+ dataStoreInUse);
			final Label dsName = new Label("dsName",dsNameModel );
			dsName.setOutputMarkupId(true);
			form.add(dsName);

			final DropDownChoice<String> option = new DropDownChoice<String>(
					"options", new Model(selectedDataStoreName), store);
			form.add(option);

			
			final Model<String> featureTypeModel = Model.of("FeatureType: "+dao
					.getDataStoreTypeName());
			final Label featureType = new Label("featureType", featureTypeModel); 
			featureType.setOutputMarkupId(true);
			form.add(featureType);

			final TextField<String> newFeatureType = new TextField<String>(
					"newFeatureType", new Model());
			form.add(newFeatureType);

			form.add(new AjaxButton("save", form) {

				protected void onSubmit(AjaxRequestTarget target, Form f) {
					
					
					
					
					String selected = option.getModelObject();

					if (selected != null) {

						DataStoreInfo data = null;

						for (DataStoreInfo dsi : dataStores) {

							if (dsi.getName().equals(selected)) {
								//set the featureType
								dao.setDataStoreTypeName(newFeatureType.getInputName());
								//set the dataStore
								dao.updateDataStoreProperties(dsi.getName(),
										dsi.getConnectionParameters());
								dao.init(config);
								dataStoreInUse = dsi.getName();
								
								
								
								message = "Monitor Configured";
								adminMessageModel.setObject("Mensages: "+message);
								target.addComponent(adminMessage);
								
								featureTypeModel.setObject("FeatureType: "+dao.getDataStoreTypeName());
								target.addComponent(featureType);
								
								dsNameModel.setObject("DataStore in use: "+ dataStoreInUse);
								target.addComponent(dsName);
								
								return;
							}

						}
						
						message = "dataStore not finded";
						adminMessageModel.setObject("Mensages: "+message);
						target.addComponent(adminMessage);

					}

				}

			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

}
