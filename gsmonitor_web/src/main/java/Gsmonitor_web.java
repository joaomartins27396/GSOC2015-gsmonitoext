package org.geoserver.gsmonitor_web;

import org.geoserver.web.GeoServerBasePage;
import org.locationtech.gsmonitor.FeatureMonitorDAO;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOption;

public class Gsmonitor_web extends GeoServerBasePage {

	

    private String selected = "jQuery";
    
	public Gsmonitor_web() {

		//FeatureMonitorDAO dao = new FeatureMonitorDAO();
    Select options = new Select("options", new PropertyModel<String>(this, "selected"));
	add(options);
	options.add(new SelectOption<String>("framework1", new Model<String>("Wicket")));
	options.add(new SelectOption<String>("framework2", new Model<String>("Spring MVC")));
	options.add(new SelectOption<String>("framework3", new Model<String>("JSF 2.0")));
	options.add(new SelectOption<String>("Script1", new Model<String>("jQuery")));
	options.add(new SelectOption<String>("Script2", new Model<String>("prototype")));


		add(new Label("hellolabel", "Hello World!"));

	}

}
