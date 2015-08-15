package org.locationtech.gsmonitor.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.web.GeoServerBasePage;
import org.locationtech.gsmonitor.FeatureMonitorDAO;

public class GSMonitorPage extends GeoServerBasePage {

	public GSMonitorPage() {
		try {
			final MonitorConfig config = new MonitorConfig(getCatalog()
					.getResourceLoader());
			final FeatureMonitorDAO dao = FeatureMonitorDAO
					.lookupMonitor(config);
			// may be null
			final String configuredStoreID = dao.getStoreID();
			final List<DataStoreInfo> dataStores = getCatalog().getStores(
					DataStoreInfo.class);
			final List<String> store = new ArrayList<String>();

			String selectedDataStoreName = null;
			if (dataStores != null) {
				for (DataStoreInfo data : dataStores) {
					if (data != null && data.isEnabled()) {
						store.add(data.getName());
						if (data.getName().equals(configuredStoreID))
							selectedDataStoreName = data.getName();
						
					}					
				}
			}

			DropDownChoice<String> options = new DropDownChoice<String>(
					"options", store);

			// has a data store been configured already?
			if (selectedDataStoreName != null) {
				// TODO, set the options to display the currently configured
				// store
				// should display with 'check' box next to it.
			}

			add(options);

			
			// TODO: Add an action to 'save' button
			// do the following:
			//   dao.updateDataStoreProperties(data.getName(),data.getMetadata().getMap());
			//   dao.init(config)
			// notice that 'data' in this case is the DataStoreInfo from the list
			// dataStores...matched by name
			
			add(new Label("hellolabel", "Hello World!"));
		} catch (Exception e) {
			// TODO: Show 'nice' error on the page.
			e.printStackTrace();
		}
	}

}
