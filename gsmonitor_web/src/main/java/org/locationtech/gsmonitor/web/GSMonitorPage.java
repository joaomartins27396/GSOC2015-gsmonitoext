package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.text.html.Option;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
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

			add(new Label("hellolabel", "DataStores"));

			Form form = new Form("form", new Model(this));

			form.setOutputMarkupId(true);

			add(form);

			final DropDownChoice<String> option = new DropDownChoice<String>(
					"options", new Model(selectedDataStoreName),

					store);

			// has a data store been configured already?
			if (selectedDataStoreName != null) {

				// //////////////////////////////////////////////////////////////////////
				// I am thinking in change the text of the label if the
				// DataStore already exits and if the user press the button it
				// don't do nothing

				// ///////////////////////////////////////////////////////////////
				// TODO, set the options to display the currently configured
				// store
				// should display with 'check' box next to it.
			}

			form.add(option);

			form.add(new AjaxButton("save", form) {

				protected void onSubmit(AjaxRequestTarget target, Form f) {

					String selected = option.getModelObject();

					if (selected != null) {

						DataStoreInfo data = null;

						for (DataStoreInfo dsi : dataStores) {

							if (dsi.getName().equals(selected)) {

								dao.updateDataStoreProperties(dsi.getName(),
										dsi.getMetadata().getMap());
								dao.init(config);

								return;

							}

						}

					}

				}

			});
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public String getSelectedDataStoreName() {
		return selectedDataStoreName;
	}

	public void setSelectedDataStoreName(String selectedDataStoreName) {
		this.selectedDataStoreName = selectedDataStoreName;
	}

}
