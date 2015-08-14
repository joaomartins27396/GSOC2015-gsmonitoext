package org.locationtech.gsmonitor.web;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerBasePage;
import org.locationtech.gsmonitor.*;

public class GSMonitorPage extends GeoServerBasePage {

	private String selected = "jQuery";

	public GSMonitorPage() {
		FeatureMonitorDAO dao = new FeatureMonitorDAO();

		List<DataStoreInfo> dataStores = getCatalog().getStores(
				DataStoreInfo.class);
		List store = new ArrayList<String>();

		if (dataStores != null) {
			for (DataStoreInfo data : dataStores) {
				if (data != null) {
					store.add(data.getName());
				}
			}
		}

		DropDownChoice<String> options = new DropDownChoice<String>("options",
				store);
		add(options);

		add(new Label("hellolabel", "Hello World!"));

	}

}
