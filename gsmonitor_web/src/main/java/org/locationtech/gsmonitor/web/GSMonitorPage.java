package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.html.Option;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;
import org.locationtech.gsmonitor.*;

public class GSMonitorPage extends GeoServerSecuredPage {

	private String selectedDataStoreName = "selected";
	private String dataStoreInUse = "";

	public GSMonitorPage() {

		try {
			final MonitorConfig config = new MonitorConfig(getCatalog()
					.getResourceLoader());

			final FeatureMonitorDAO dao = FeatureMonitorDAO.lookupMonitor(config);
			dao.init(config);
			final String configuredStoreID =  dao.getStoreID();

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

			

			Form form = new Form("form", new Model(this));

			form.setOutputMarkupId(true);

			add(form);
			
			form.add(new Label("dsName", Model.of("DataStore in use: "+dataStoreInUse)));

			final DropDownChoice<String> option = new DropDownChoice<String>(
					"options", new Model(selectedDataStoreName),
					store);
			form.add(option);

			form.add(new AjaxButton("save", form) {

				protected void onSubmit(AjaxRequestTarget target, Form f) {

					String selected = option.getModelObject();

					if (selected != null) {

						DataStoreInfo data = null;

						for (DataStoreInfo dsi : dataStores) {

							if (dsi.getName().equals(selected)) {
								dao.updateDataStoreProperties(dsi.getName(),
										dsi.getConnectionParameters());
								dao.init(config);
								dataStoreInUse = dsi.getName();
								return;

							}

						}

					}

				}

			});
			

			form.add(new Label("featureType", Model.of(dao.getDataStoreTypeName())));
			
			
			
			

			final TextField<String> newFeatureType = new TextField<String>("newFeatureType",new Model());
			form.add(newFeatureType);
			
			
			form.add(new AjaxButton("saveFeatureType", form) {

				protected void onSubmit(AjaxRequestTarget target, Form f) {

					LOGGER.log(Level.SEVERE, "deteted"+newFeatureType.getInputName());
					dao.setDataStoreTypeName(newFeatureType.getInputName());

				}

			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}




}
