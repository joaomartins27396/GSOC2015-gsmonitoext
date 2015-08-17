package org.locationtech.gsmonitor.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

<<<<<<< HEAD
import javax.swing.text.html.Option;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
=======
>>>>>>> master
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.DropDownChoice;
<<<<<<< HEAD
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.MonitorDAO;
import org.geoserver.web.GeoServerBasePage;
import org.geotools.util.logging.Logging;
import org.locationtech.gsmonitor.*;

public class GSMonitorPage extends GeoServerBasePage {

	
	private String selected = "selected";
	
	
	public GSMonitorPage() {
		FeatureMonitorDAO dao = new FeatureMonitorDAO();

		final List<DataStoreInfo> dataStores = getCatalog().getStores(
				DataStoreInfo.class);
		List store = new ArrayList<String>();
=======
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
>>>>>>> master

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

<<<<<<< HEAD
		

		add(new Label("hellolabel", "Hello World!"));
		
		
		

		Form f = new Form("form", new Model(this));
        f.setOutputMarkupId(true);
        add(f);
        
        
        
        
        final DropDownChoice<String> option = new DropDownChoice<String>("options", new Model(selected),
				store){
			@Override
			protected void onSelectionChanged(String newSelection) {

            	LOGGER.info("Ok button pressed dropdownChoice "+newSelection);
				setSelected(newSelection);
				super.onSelectionChanged(newSelection);
			}
		};

		f.add(option);
		f.add(new AjaxButton("run", f) {
            protected void onSubmit(AjaxRequestTarget target, Form f) {
            	String selected = option.getModelObject();
            	if(selected!=null){
            		DataStoreInfo data = null;
            		for(DataStoreInfo dsi: dataStores){
            			if(dsi.getName().equals(selected)){
            				DataStoreInfo dataStoreSelected = dsi;
            				
            				MonitorConfig config;
							try {
								config = new MonitorConfig(getCatalog().getResourceLoader());
								FeatureMonitorDAO dao = new FeatureMonitorDAO();
	            				dao.init(config);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
            				
            				
            				
            				return;
            			}
            		}
            	}
            	
            	LOGGER.info("Ok button pressed form "+option.getModelObject());
            }
        });
		
		
		
		

		
		
		

	}
	
	
	private void setSelected(String string){
		this.selected = string;
	}
	private String getSelected(){
		return selected;
=======
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
>>>>>>> master
	}

}
